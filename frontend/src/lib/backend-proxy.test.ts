import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const serverMocks = vi.hoisted(() => ({
  getRequest: vi.fn(),
  appendResponseHeader: vi.fn(),
  setResponseHeader: vi.fn(),
}))

const fetchMock = vi.hoisted(() => ({ fetch: vi.fn() }))

vi.mock('@tanstack/react-start/server', () => serverMocks)

import { callBackend, refreshAccessToken } from './backend-proxy'

const ACCESS_TOKEN_HEADER = 'x-chafgram-access-token'
const RELOGIN_HEADER = 'x-chafgram-relogin'
const REFRESH_COOKIE = 'chafgram_refresh'
const API = 'http://localhost:8080'

/** Minimal Response shape for the BFF's transport-level assertions. */
function fakeResponse(
  status: number,
  options: { body?: unknown; cookies?: string[] } = {},
): Response {
  return {
    status,
    statusText: status === 401 ? 'Unauthorized' : 'OK',
    ok: status >= 200 && status < 300,
    headers: {
      get: () => null,
      getSetCookie: () => options.cookies ?? [],
    },
    json: async () => options.body ?? {},
  } as unknown as Response
}

function incomingRequest(init: { authorization?: string; cookie?: string } = {}) {
  const headers = new Headers()
  if (init.authorization) headers.set('authorization', init.authorization)
  if (init.cookie) headers.set('cookie', init.cookie)
  return { headers } as Request
}

function authHeaders(callIndex: number): Headers {
  const [, init] = fetchMock.fetch.mock.calls[callIndex]
  return new Headers(init.headers)
}

beforeEach(() => {
  vi.clearAllMocks()
  fetchMock.fetch.mockReset()
  process.env.CHAFGRAM_API_URL = API
  vi.stubGlobal('fetch', fetchMock.fetch)
  serverMocks.getRequest.mockReturnValue(
    incomingRequest({
      authorization: 'Bearer old-access',
      cookie: `${REFRESH_COOKIE}=old-refresh; XSRF-TOKEN=must-not-forward; JSESSIONID=must-not-forward`,
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('callBackend bearer + refresh retry', () => {
  it('forwards only the bearer header and never session or CSRF cookies', async () => {
    fetchMock.fetch.mockResolvedValueOnce(fakeResponse(200))

    await callBackend({
      path: '/graphql',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"query":"{ me { id } }"}',
    })

    expect(fetchMock.fetch).toHaveBeenCalledTimes(1)
    const headers = authHeaders(0)
    expect(headers.get('authorization')).toBe('Bearer old-access')
    expect(headers.get('cookie')).toBeNull()
    expect(headers.get('x-xsrf-token')).toBeNull()
    expect(serverMocks.setResponseHeader).not.toHaveBeenCalled()
  })

  it('refreshes once on 401, relays the rotated cookie, retries with the new bearer and tells the browser', async () => {
    fetchMock.fetch
      .mockResolvedValueOnce(fakeResponse(401))
      .mockResolvedValueOnce(
        fakeResponse(200, {
          body: { accessToken: 'new-access', tokenType: 'Bearer', expiresIn: 900 },
          cookies: [`${REFRESH_COOKIE}=new-refresh; Path=/; HttpOnly; SameSite=Strict`],
        }),
      )
      .mockResolvedValueOnce(fakeResponse(200))

    const result = await callBackend({ path: '/graphql', method: 'POST', body: '{}' })

    expect(result.status).toBe(200)
    // 1 original + 1 refresh + 1 retry.
    expect(fetchMock.fetch).toHaveBeenCalledTimes(3)
    expect(String(fetchMock.fetch.mock.calls[1][0])).toBe(`${API}/api/v1/auth/refresh`)

    const refreshHeaders = authHeaders(1)
    expect(refreshHeaders.get('cookie')).toBe(`${REFRESH_COOKIE}=old-refresh`)
    expect(refreshHeaders.get('authorization')).toBeNull()

    expect(authHeaders(2).get('authorization')).toBe('Bearer new-access')
    expect(serverMocks.setResponseHeader).toHaveBeenCalledWith(ACCESS_TOKEN_HEADER, 'new-access')
    expect(serverMocks.appendResponseHeader).toHaveBeenCalledWith(
      'set-cookie',
      `${REFRESH_COOKIE}=new-refresh; Path=/; HttpOnly; SameSite=Strict`,
    )
    expect(serverMocks.setResponseHeader).not.toHaveBeenCalledWith(RELOGIN_HEADER, '1')
  })

  it('does not retry and signals re-login when the refresh itself fails', async () => {
    fetchMock.fetch
      .mockResolvedValueOnce(fakeResponse(401))
      .mockResolvedValueOnce(fakeResponse(401))

    const result = await callBackend({ path: '/graphql', method: 'POST', body: '{}' })

    expect(result.status).toBe(401)
    expect(fetchMock.fetch).toHaveBeenCalledTimes(2)
    expect(serverMocks.setResponseHeader).toHaveBeenCalledWith(RELOGIN_HEADER, '1')
    expect(serverMocks.setResponseHeader).not.toHaveBeenCalledWith(ACCESS_TOKEN_HEADER, expect.any(String))
  })

  it('refreshes at most once and signals re-login when the retry is also a 401', async () => {
    fetchMock.fetch
      .mockResolvedValueOnce(fakeResponse(401))
      .mockResolvedValueOnce(fakeResponse(200, { body: { accessToken: 'new-access' } }))
      .mockResolvedValueOnce(fakeResponse(401))

    const result = await callBackend({ path: '/graphql', method: 'POST', body: '{}' })

    expect(result.status).toBe(401)
    expect(fetchMock.fetch).toHaveBeenCalledTimes(3)
    expect(serverMocks.setResponseHeader).toHaveBeenCalledWith(ACCESS_TOKEN_HEADER, 'new-access')
    expect(serverMocks.setResponseHeader).toHaveBeenCalledWith(RELOGIN_HEADER, '1')
  })

  it('relays the refresh cookie from a successful login without attempting a refresh', async () => {
    serverMocks.getRequest.mockReturnValue(incomingRequest())
    fetchMock.fetch.mockResolvedValueOnce(
      fakeResponse(200, {
        body: { accessToken: 'fresh-access', tokenType: 'Bearer', expiresIn: 900 },
        cookies: [`${REFRESH_COOKIE}=fresh-refresh; Path=/; HttpOnly; SameSite=Strict`],
      }),
    )

    await callBackend({ path: '/api/v1/auth/login', method: 'POST', body: '{}' })

    expect(fetchMock.fetch).toHaveBeenCalledTimes(1)
    expect(serverMocks.appendResponseHeader).toHaveBeenCalledWith(
      'set-cookie',
      `${REFRESH_COOKIE}=fresh-refresh; Path=/; HttpOnly; SameSite=Strict`,
    )
    expect(fetchMock.fetch.mock.calls.some(([url]) => String(url).includes('/refresh'))).toBe(false)
  })

  it('signals re-login when no refresh cookie is available on a 401', async () => {
    serverMocks.getRequest.mockReturnValue(incomingRequest({ authorization: 'Bearer expired' }))
    fetchMock.fetch.mockResolvedValueOnce(fakeResponse(401))

    const result = await callBackend({ path: '/graphql', method: 'POST', body: '{}' })

    expect(result.status).toBe(401)
    expect(fetchMock.fetch).toHaveBeenCalledTimes(1)
    expect(serverMocks.setResponseHeader).toHaveBeenCalledWith(RELOGIN_HEADER, '1')
  })

  it('forwards only the refresh cookie to backend logout so the token can be revoked', async () => {
    const clearingCookie = `${REFRESH_COOKIE}=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0`
    fetchMock.fetch.mockResolvedValueOnce(fakeResponse(204, { cookies: [clearingCookie] }))

    const result = await callBackend({ path: '/api/v1/auth/logout', method: 'POST' })

    expect(result.status).toBe(204)
    expect(fetchMock.fetch).toHaveBeenCalledTimes(1)
    const headers = authHeaders(0)
    expect(headers.get('cookie')).toBe(`${REFRESH_COOKIE}=old-refresh`)
    expect(headers.get('authorization')).toBe('Bearer old-access')
    expect(fetchMock.fetch.mock.calls.some(([url]) => String(url).includes('/refresh'))).toBe(false)
    expect(serverMocks.appendResponseHeader).toHaveBeenCalledWith('set-cookie', clearingCookie)
  })

  it('cannot mint a new access token after logout revoked the forwarded refresh cookie', async () => {
    let revoked = false
    fetchMock.fetch.mockImplementation(async (url: string, init?: RequestInit) => {
      const path = String(url).replace(API, '')
      if (path === '/api/v1/auth/logout') {
        revoked = new Headers(init?.headers).get('cookie') === `${REFRESH_COOKIE}=old-refresh`
        return fakeResponse(204)
      }
      if (path === '/api/v1/auth/refresh') {
        return fakeResponse(revoked ? 401 : 200, { body: { accessToken: 'unexpected-access' } })
      }
      throw new Error(`Unexpected backend call: ${path}`)
    })

    await callBackend({ path: '/api/v1/auth/logout', method: 'POST' })
    expect(revoked).toBe(true)

    const refresh = await refreshAccessToken()
    expect(refresh).toEqual({ ok: false, accessToken: null })
    expect(serverMocks.setResponseHeader).not.toHaveBeenCalledWith(ACCESS_TOKEN_HEADER, expect.any(String))
  })

  it.each([
    '/api/v1/media/7/bytes?exp=1&uid=2&sig=abc',
    '/api/v1/media/7/signed-url',
  ])('does not consume or rotate the refresh cookie on a media 401 for %s', async (path) => {
    // Queue a refresh response too: pre-fix code consumes it, then the
    // assertions below fail on the extra backend call rather than a TypeError.
    fetchMock.fetch
      .mockResolvedValueOnce(fakeResponse(401))
      .mockResolvedValueOnce(fakeResponse(401))

    const result = await callBackend({ path, method: 'GET' })

    expect(result.status).toBe(401)
    expect(fetchMock.fetch).toHaveBeenCalledTimes(1)
    expect(fetchMock.fetch.mock.calls.some(([url]) => String(url).includes('/api/v1/auth/refresh'))).toBe(false)
    expect(serverMocks.setResponseHeader).not.toHaveBeenCalledWith(ACCESS_TOKEN_HEADER, expect.any(String))
    expect(serverMocks.setResponseHeader).not.toHaveBeenCalledWith(RELOGIN_HEADER, '1')
  })
})

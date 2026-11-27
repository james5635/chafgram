import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  callBackend: vi.fn(),
  refreshAccessToken: vi.fn(),
  setResponseHeader: vi.fn(),
}))

/** Stub the server-function builder so exported functions are raw handlers. */
vi.mock('@tanstack/react-start', () => ({
  createServerFn: () => {
    const builder = {
      validator: () => builder,
      handler: (fn: (ctx?: unknown) => Promise<unknown>) => fn,
    }
    return builder
  },
}))

vi.mock('@tanstack/react-start/server', () => ({ setResponseHeader: mocks.setResponseHeader }))
vi.mock('./backend-proxy', () => ({
  callBackend: mocks.callBackend,
  refreshAccessToken: mocks.refreshAccessToken,
}))

import { RELOGIN_HEADER } from './auth-client'
import { loginRequest, logoutRequest, refreshRequest } from './session-server'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('session server functions', () => {
  it('returns only the access token from a successful login', async () => {
    mocks.callBackend.mockResolvedValue({
      ok: true,
      json: async () => ({ accessToken: 'jwt-access', tokenType: 'Bearer', expiresIn: 900 }),
    })

    const result = await loginRequest({ data: { username: 'rojame', password: 'hunter2' } })

    expect(mocks.callBackend).toHaveBeenCalledWith({
      path: '/api/v1/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ username: 'rojame', password: 'hunter2' }),
    })
    expect(result).toEqual({ ok: true, accessToken: 'jwt-access', tokenType: 'Bearer', expiresIn: 900 })
  })

  it('exposes the silent refresh result to the bootstrap', async () => {
    mocks.refreshAccessToken.mockResolvedValue({ ok: true, accessToken: 'silent-access' })

    const result = await refreshRequest({})

    expect(mocks.refreshAccessToken).toHaveBeenCalledTimes(1)
    expect(result).toEqual({ ok: true, accessToken: 'silent-access' })
  })

  it('clears the in-memory token after a successful logout', async () => {
    mocks.callBackend.mockResolvedValue({ ok: true })

    const result = await logoutRequest({})

    expect(mocks.callBackend).toHaveBeenCalledWith({ path: '/api/v1/auth/logout', method: 'POST' })
    expect(mocks.setResponseHeader).toHaveBeenCalledWith(RELOGIN_HEADER, '1')
    expect(result).toEqual({ ok: true })
  })

  it('also clears the in-memory token when logout fails with a non-2xx response', async () => {
    mocks.callBackend.mockResolvedValue({ ok: false, status: 500 })

    const result = await logoutRequest({})

    expect(mocks.setResponseHeader).toHaveBeenCalledWith(RELOGIN_HEADER, '1')
    expect(result).toEqual({ ok: false })
  })

  it('also clears the in-memory token when logout is rejected with a 401', async () => {
    mocks.callBackend.mockResolvedValue({ ok: false, status: 401 })

    const result = await logoutRequest({})

    expect(mocks.setResponseHeader).toHaveBeenCalledWith(RELOGIN_HEADER, '1')
    expect(result).toEqual({ ok: false })
  })
})

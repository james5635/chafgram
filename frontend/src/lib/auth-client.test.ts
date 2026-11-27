import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  ACCESS_TOKEN_HEADER as HEADER_NAME,
  RELOGIN_HEADER as RELOGIN_NAME,
  REFRESH_COOKIE_NAME,
  bootstrapAuth,
  clearAccessToken,
  consumeReLogin,
  createAuthFetch,
  getAccessToken,
  setAccessToken,
  signalReLogin,
  subscribeAuth,
} from './auth-client'

beforeEach(() => {
  clearAccessToken()
  consumeReLogin()
})

describe('in-memory access token store', () => {
  it('holds the token in memory and notifies subscribers on change and sign-out', () => {
    const listener = vi.fn()
    const unsubscribe = subscribeAuth(listener)

    setAccessToken('jwt-1')
    expect(getAccessToken()).toBe('jwt-1')
    expect(listener).toHaveBeenCalledWith('token-changed')

    clearAccessToken()
    expect(getAccessToken()).toBeNull()
    expect(listener).toHaveBeenCalledWith('signed-out')

    unsubscribe()
    listener.mockClear()
    setAccessToken('jwt-2')
    expect(listener).not.toHaveBeenCalled()
  })

  it('records a re-login signal exactly once until consumed', () => {
    expect(consumeReLogin()).toBe(false)
    signalReLogin()
    expect(getAccessToken()).toBeNull()
    expect(consumeReLogin()).toBe(true)
    expect(consumeReLogin()).toBe(false)
  })

  it('names the HttpOnly refresh cookie the BFF forwards', () => {
    expect(REFRESH_COOKIE_NAME).toBe('chafgram_refresh')
  })
})

describe('BFF server-function fetch', () => {
  it('attaches the bearer to every server-function call, including media re-sign', async () => {
    setAccessToken('jwt-browser')
    const baseFetch = vi.fn().mockResolvedValue(new Response('ok'))
    const authFetch = createAuthFetch(baseFetch)

    await authFetch('http://localhost:3000/_server/src/lib/media-server.refreshMediaUrl', {
      method: 'GET',
    })

    const [, init] = baseFetch.mock.calls[0]
    expect(new Headers(init?.headers).get('authorization')).toBe('Bearer jwt-browser')
  })

  it('omits the bearer when the user is signed out', async () => {
    const baseFetch = vi.fn().mockResolvedValue(new Response('ok'))
    const authFetch = createAuthFetch(baseFetch)

    await authFetch('/_server/graphqlRequest', { method: 'POST', body: '{}' })

    const [, init] = baseFetch.mock.calls[0]
    expect(new Headers(init?.headers).has('authorization')).toBe(false)
  })

  it('adopts a rotated access token returned by the BFF', async () => {
    setAccessToken('stale')
    const baseFetch = vi.fn().mockResolvedValue(
      new Response('ok', { headers: { [HEADER_NAME]: 'rotated' } }),
    )
    const authFetch = createAuthFetch(baseFetch)

    await authFetch('/_server/graphqlRequest', { method: 'POST', body: '{}' })

    expect(getAccessToken()).toBe('rotated')
  })

  it('clears the token and raises a re-login signal when the BFF reports one', async () => {
    setAccessToken('stale')
    const baseFetch = vi.fn().mockResolvedValue(
      new Response('ok', { headers: { [RELOGIN_NAME]: '1' } }),
    )
    const authFetch = createAuthFetch(baseFetch)

    await authFetch('/_server/graphqlRequest', { method: 'POST', body: '{}' })

    expect(getAccessToken()).toBeNull()
    expect(consumeReLogin()).toBe(true)
  })

  it('never reads a refresh cookie from JavaScript', async () => {
    vi.stubGlobal('document', { cookie: `${REFRESH_COOKIE_NAME}=secret` })
    const baseFetch = vi.fn().mockResolvedValue(new Response('ok'))
    const authFetch = createAuthFetch(baseFetch)

    await authFetch('/_server/graphqlRequest', { method: 'POST', body: '{}' })

    const [, init] = baseFetch.mock.calls[0]
    expect(new Headers(init?.headers).has('cookie')).toBe(false)
    vi.unstubAllGlobals()
  })
})

describe('silent bootstrap refresh', () => {
  it('skips refreshing on a public page', async () => {
    const refresh = vi.fn()

    await bootstrapAuth({ isPublic: true, refresh })

    expect(refresh).not.toHaveBeenCalled()
  })

  it('skips refreshing when an access token is already in memory', async () => {
    setAccessToken('jwt-present')
    const refresh = vi.fn()

    await bootstrapAuth({ isPublic: false, refresh })

    expect(refresh).not.toHaveBeenCalled()
    expect(getAccessToken()).toBe('jwt-present')
  })

  it('exchanges the refresh cookie for an access token before data loads', async () => {
    const refresh = vi.fn().mockResolvedValue({ ok: true, accessToken: 'silent-access' })

    await bootstrapAuth({ isPublic: false, refresh })

    expect(refresh).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBe('silent-access')
    expect(consumeReLogin()).toBe(false)
  })

  it('signals re-login when the silent refresh fails with no cookie', async () => {
    const refresh = vi.fn().mockResolvedValue({ ok: false })

    await bootstrapAuth({ isPublic: false, refresh })

    expect(getAccessToken()).toBeNull()
    expect(consumeReLogin()).toBe(true)
  })

  it('shares one refresh across concurrent callers (StrictMode double effect)', async () => {
    let resolveRefresh!: (value: { ok: boolean; accessToken: string }) => void
    const refresh = vi.fn(() => new Promise<{ ok: boolean; accessToken: string }>((resolve) => {
      resolveRefresh = resolve
    }))

    const first = bootstrapAuth({ isPublic: false, refresh })
    const second = bootstrapAuth({ isPublic: false, refresh })

    expect(refresh).toHaveBeenCalledTimes(1)
    resolveRefresh({ ok: true, accessToken: 'silent-access' })
    await Promise.all([first, second])

    expect(refresh).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBe('silent-access')
    expect(consumeReLogin()).toBe(false)
  })

  it('signals re-login when the refresh call itself throws', async () => {
    const refresh = vi.fn().mockRejectedValue(new Error('offline'))

    await bootstrapAuth({ isPublic: false, refresh })

    expect(consumeReLogin()).toBe(true)
  })
})

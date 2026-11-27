import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  getRequestHeader: vi.fn(),
  callBackend: vi.fn(),
}))

/**
 * TanStack Start needs a request-scope AsyncLocalStorage to run a server
 * function through its RPC wrapper. In this unit test we stub the builder so
 * the exported function *is* its handler, which lets us invoke it directly and
 * still exercise the real getRequestHeader → callBackend chain.
 */
vi.mock('@tanstack/react-start', () => ({
  createServerFn: () => {
    const builder = {
      validator: () => builder,
      handler: (fn: (ctx: unknown) => Promise<unknown>) => fn,
    }
    return builder
  },
}))

vi.mock('@tanstack/react-start/server', () => ({ getRequestHeader: mocks.getRequestHeader }))
vi.mock('./backend-proxy', () => ({ callBackend: mocks.callBackend }))

import { decodeBase64, mediaAuthHeaders, refreshMediaUrl } from './media-server'

beforeEach(() => {
  vi.clearAllMocks()
  mocks.callBackend.mockResolvedValue({
    ok: true,
    json: async () => ({ url: '/api/media/9?exp=1&sig=x', expiresAt: '2026-11-27T00:00:00Z' }),
  })
})

describe('decodeBase64', () => {
  it('decodes plain bytes', () => {
    const bytes = decodeBase64(btoa('hello'))

    expect(Array.from(bytes)).toEqual([104, 101, 108, 108, 111])
  })

  it('decodes an empty payload', () => {
    expect(decodeBase64('')).toHaveLength(0)
  })

  it('decodes binary data', () => {
    const bytes = decodeBase64(btoa(String.fromCharCode(0, 127, 255)))

    expect(Array.from(bytes)).toEqual([0, 127, 255])
  })
})

describe('mediaAuthHeaders', () => {
  it('forwards the browser-injected bearer so an expired media URL can be re-signed', () => {
    expect(mediaAuthHeaders('Bearer jwt-from-browser')).toEqual({
      Authorization: 'Bearer jwt-from-browser',
    })
  })

  it('sends no auth header when the browser has no access token', () => {
    expect(mediaAuthHeaders(null)).toEqual({})
  })
})

describe('refreshMediaUrl handler', () => {
  it('forwards the header injected by createAuthFetch to the backend signed-url path', async () => {
    mocks.getRequestHeader.mockImplementation((name: string) =>
      name === 'Authorization' ? 'Bearer jwt-from-createAuthFetch' : undefined,
    )

    const result = await refreshMediaUrl({ data: { mediaId: '9' } })

    expect(mocks.getRequestHeader).toHaveBeenCalledWith('Authorization')
    expect(mocks.callBackend).toHaveBeenCalledTimes(1)
    expect(mocks.callBackend).toHaveBeenCalledWith({
      path: '/api/v1/media/9/signed-url',
      method: 'GET',
      headers: { Authorization: 'Bearer jwt-from-createAuthFetch' },
    })
    expect(result.url).toBe('/api/media/9?exp=1&sig=x')
  })

  it('sends no Authorization header when the browser has no access token', async () => {
    mocks.getRequestHeader.mockReturnValue(undefined)

    await refreshMediaUrl({ data: { mediaId: '10' } })

    expect(mocks.callBackend).toHaveBeenCalledWith({
      path: '/api/v1/media/10/signed-url',
      method: 'GET',
      headers: {},
    })
  })

  it('rejects a non-numeric media id before calling the backend', async () => {
    await expect(refreshMediaUrl({ data: { mediaId: 'nope' } })).rejects.toThrow('Invalid media id')
    expect(mocks.callBackend).not.toHaveBeenCalled()
  })
})

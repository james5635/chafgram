import { describe, expect, it, vi } from 'vitest'
const { callBackend: mockCallBackend } = vi.hoisted(() => ({ callBackend: vi.fn() }))
vi.mock('@/lib/backend-proxy', () => ({ callBackend: mockCallBackend }))

import { Route } from '../routes/api/media/$id'

function get(params: { id: string }, range?: string, search?: string) {
  const handlers = (Route as any).options.server.handlers
  const request = new Request('http://localhost/api/media/' + params.id + (search ?? ''))
  if (range) request.headers.set('range', range)
  return handlers.GET({ params, request })
}

describe('media BFF route', () => {
  it('streams backend bytes with the original content type', async () => {
    mockCallBackend.mockResolvedValue(
      new Response(new Uint8Array([137, 80, 78, 71]), {
        headers: { 'content-type': 'image/png' },
      }),
    )

    const response = await get({ id: '41' })

    expect(mockCallBackend).toHaveBeenCalledWith({
      path: '/api/v1/media/41/bytes',
      method: 'GET',
    })
    expect(response.status).toBe(200)
    expect(response.headers.get('content-type')).toBe('image/png')
    expect((await response.arrayBuffer()).byteLength).toBe(4)
  })

  it('forwards signed URL parameters and keeps the private short cache', async () => {
    mockCallBackend.mockResolvedValue(
      new Response(new Uint8Array([1, 2, 3]), {
        headers: { 'content-type': 'image/png' },
      }),
    )

    const response = await get(
      { id: '41' },
      undefined,
      '?exp=1791000000&uid=7&sig=abc123',
    )

    expect(mockCallBackend).toHaveBeenCalledWith({
      path: '/api/v1/media/41/bytes?exp=1791000000&uid=7&sig=abc123',
      method: 'GET',
    })
    expect(response.headers.get('cache-control')).toBe('private, max-age=300')
  })

  it('rejects a non-numeric media id before calling the backend', async () => {
    const response = await get({ id: 'oops' })

    expect(response.status).toBe(400)
    expect(mockCallBackend).not.toHaveBeenCalled()
  })

  it('serves byte ranges so videos can seek', async () => {
    mockCallBackend.mockResolvedValue(
      new Response(new Uint8Array([1, 2, 3, 4, 5]), {
        headers: { 'content-type': 'video/mp4' },
      }),
    )

    const response = await get({ id: '42' }, 'bytes=1-2')

    expect(response.status).toBe(206)
    expect(response.headers.get('content-range')).toBe('bytes 1-2/5')
    expect(Array.from(new Uint8Array(await response.arrayBuffer()))).toEqual([2, 3])
  })
})

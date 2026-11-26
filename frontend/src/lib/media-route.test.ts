import { describe, expect, it, vi } from 'vitest'
import { callBackend } from '@/lib/backend-proxy'

const { callBackend: mockCallBackend } = vi.hoisted(() => ({ callBackend: vi.fn() }))
vi.mock('@/lib/backend-proxy', () => ({ callBackend: mockCallBackend }))

import { Route } from '../routes/api/media/$id'

describe('media BFF route', () => {
  it('streams backend bytes with the original content type', async () => {
    mockCallBackend.mockResolvedValue(
      new Response(new Uint8Array([137, 80, 78, 71]), {
        headers: { 'content-type': 'image/png' },
      }),
    )

    const handlers = (Route as any).options.server.handlers
    const response = await handlers.GET({ params: { id: '41' } })

    expect(mockCallBackend).toHaveBeenCalledWith({
      path: '/api/v1/media/41/bytes',
      method: 'GET',
    })
    expect(response.status).toBe(200)
    expect(response.headers.get('content-type')).toBe('image/png')
    expect((await response.arrayBuffer()).byteLength).toBe(4)
  })

  it('rejects a non-numeric media id before calling the backend', async () => {
    const handlers = (Route as any).options.server.handlers
    const response = await handlers.GET({ params: { id: 'oops' } })

    expect(response.status).toBe(400)
    expect(mockCallBackend).not.toHaveBeenCalled()
  })
})

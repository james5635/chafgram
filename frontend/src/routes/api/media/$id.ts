import { createFileRoute } from '@tanstack/react-router'
import { callBackend } from '@/lib/backend-proxy'

/**
 * BFF proxy for authorized media bytes. The browser loads images from
 * `/api/media/:id`, so the backend URL and session cookie stay server-side.
 */
export const Route = createFileRoute('/api/media/$id')({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const id = params.id
        if (!/^\d+$/.test(id)) {
          return new Response('Invalid media id', { status: 400 })
        }

        const backend = await callBackend({
          path: `/api/v1/media/${id}/bytes`,
          method: 'GET',
        })
        if (!backend.ok) {
          return new Response('Media not found', { status: backend.status })
        }

        return new Response(await backend.arrayBuffer(), {
          headers: {
            'content-type': backend.headers.get('content-type') ?? 'application/octet-stream',
            'cache-control': 'private, max-age=3600',
          },
        })
      },
    },
  },
})

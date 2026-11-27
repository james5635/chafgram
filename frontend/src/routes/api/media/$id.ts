import { createFileRoute } from '@tanstack/react-router'
import { callBackend } from '@/lib/backend-proxy'

/**
 * BFF proxy for authorized media bytes. The browser loads images from
 * `/api/media/:id?exp=...&uid=...&sig=...` (a short-lived backend-signed URL),
 * so the backend URL and credentials stay server-side and <img>/<video> need
 * no bearer header. Unsigned requests keep working for header-authenticated
 * API clients.
 */
export const Route = createFileRoute('/api/media/$id')({
  server: {
    handlers: {
      GET: async ({ params, request }) => {
        const id = params.id
        if (!/^\d+$/.test(id)) {
          return new Response('Invalid media id', { status: 400 })
        }

        // Only signature parameters are relayed; never forward arbitrary
        // query strings to the backend bytes route.
        const signedQuery = new URL(request.url).searchParams
        const query = ['exp', 'uid', 'sig']
          .filter((key) => signedQuery.has(key))
          .map((key) => `${key}=${encodeURIComponent(signedQuery.get(key) ?? '')}`)
          .join('&')
        const backend = await callBackend({
          path: `/api/v1/media/${id}/bytes${query ? `?${query}` : ''}`,
          method: 'GET',
        })
        if (!backend.ok) {
          return new Response('Media not found', { status: backend.status })
        }

        const bytes = await backend.arrayBuffer()
        const contentType = backend.headers.get('content-type') ?? 'application/octet-stream'
        const headers = {
          'content-type': contentType,
          'cache-control': 'private, max-age=300',
          'accept-ranges': 'bytes',
        }

        // The backend streams the whole asset; slice here so <video> can seek.
        const range = request.headers.get('range')
        const match = range?.match(/^bytes=(\d*)-(\d*)$/)
        if (range && !match) {
          return new Response(null, { status: 416, headers: { 'content-range': `bytes */${bytes.byteLength}` } })
        }
        if (match) {
          const total = bytes.byteLength
          const start = match[1] ? Number(match[1]) : Math.max(0, total - Number(match[2] || 0))
          const end = match[1] && match[2] ? Math.min(Number(match[2]), total - 1) : total - 1
          if (start > end || start >= total) {
            return new Response(null, { status: 416, headers: { 'content-range': `bytes */${total}` } })
          }
          return new Response(bytes.slice(start, end + 1), {
            status: 206,
            headers: {
              ...headers,
              'content-range': `bytes ${start}-${end}/${total}`,
              'content-length': String(end - start + 1),
            },
          })
        }

        return new Response(bytes, { headers })
      },
    },
  },
})

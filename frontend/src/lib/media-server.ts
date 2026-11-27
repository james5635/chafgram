import { createServerFn } from '@tanstack/react-start'
import { getRequestHeader } from '@tanstack/react-start/server'
import { callBackend } from './backend-proxy'

/** Uploads above this size are rejected before they reach the backend. */
export const MAX_UPLOAD_BYTES = 25 * 1024 * 1024

export interface MediaUploadInput {
  filename: string
  contentType: string
  /** base64-encoded file bytes. */
  data: string
}

export interface MediaUploadResult {
  id: string
  kind: string
  status: string
}

/** Decode base64 into bytes without depending on Node-only globals. */
export function decodeBase64(data: string): Uint8Array<ArrayBuffer> {
  const binary = atob(data)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}

export interface SignedMediaUrl {
  url: string
  expiresAt: string
}

/**
 * The browser→BFF fetch attaches the in-memory bearer to every server-function
 * call (see `auth-client.ts`). Lifting this into a helper keeps the media
 * re-sign path's auth transport explicit and independently testable.
 */
export function mediaAuthHeaders(authorization: string | null | undefined): Record<string, string> {
  return authorization ? { Authorization: authorization } : {}
}

/**
 * Re-sign one media asset for the current viewer after a signed URL expires.
 * The backend re-checks media access before issuing the URL. Signed-URL and
 * bytes paths are excluded from the BFF's refresh-and-retry: a 401 here fails
 * the re-sign instead of consuming or rotating the refresh cookie, and the
 * browser only learns the short-lived URL.
 */
export const refreshMediaUrl = createServerFn({ method: 'GET' })
  .validator((input: { mediaId: string }) => input)
  .handler(async ({ data }): Promise<SignedMediaUrl> => {
    if (!/^\d+$/.test(data.mediaId)) {
      throw new Error('Invalid media id')
    }

    // The browser's access token is in memory; the global BFF fetch attaches
    // it to this server-function call, so /signed-url can authenticate the
    // viewer even after the signed media URL has expired.
    const authorization = getRequestHeader('Authorization')
    const response = await callBackend({
      path: `/api/v1/media/${data.mediaId}/signed-url`,
      method: 'GET',
      headers: mediaAuthHeaders(authorization),
    })
    if (!response.ok) {
      throw new Error(`Media URL refresh failed: ${response.status}`)
    }
    return (await response.json()) as SignedMediaUrl
  })

/**
 * Media upload (REST keep-list): multipart upload of the bytes, GraphQL for
 * the metadata. The browser sends base64 so the payload stays JSON-safe on
 * the way through the server function.
 */
export const uploadMedia = createServerFn({ method: 'POST' })
  .validator((input: MediaUploadInput) => input)
  .handler(async ({ data }): Promise<MediaUploadResult> => {
    const bytes = decodeBase64(data.data)

    if (bytes.byteLength > MAX_UPLOAD_BYTES) {
      throw new Error('That file is too large (25 MB maximum).')
    }

    const response = await callBackend({
      path: '/api/v1/media',
      method: 'POST',
      multipart: {
        filename: data.filename,
        contentType: data.contentType || 'application/octet-stream',
        bytes,
      },
    })

    if (!response.ok) {
      throw new Error(`Media upload failed: ${response.status} ${response.statusText}`)
    }

    const asset = (await response.json()) as {
      id: string | number
      kind?: string
      status?: string
    }

    return {
      id: String(asset.id),
      kind: asset.kind ?? 'image',
      status: asset.status ?? 'ready',
    }
  })

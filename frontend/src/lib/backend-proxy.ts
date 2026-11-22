import { appendResponseHeader, getRequest } from '@tanstack/react-start/server'
import {
  XSRF_HEADER_NAME,
  applySetCookies,
  parseCookieHeader,
  serializeCookieHeader,
  xsrfToken,
  xsrfTokenFromSetCookies,
} from './http'

/**
 * Server-only proxy to the Spring Boot backend.
 *
 * This is the single egress point of the BFF: the browser never talks to the
 * backend directly, so backend URLs and the session cookie stay server-side.
 *
 * It forwards the browser's cookies (session cookie + double-submit CSRF
 * token) to the backend, echoes the CSRF token in the `X-XSRF-TOKEN` header,
 * and copies every `Set-Cookie` the backend issues back to the browser.
 */

export interface BackendCall {
  /** Path on the backend, e.g. `/graphql`. */
  path: string
  method: string
  headers?: Record<string, string>
  /** Replayable body — a retry must be able to send it again. */
  body?: string
  /** Multipart upload — rebuilt for every attempt so a retry can resend it. */
  multipart?: { filename: string; contentType: string; bytes: Uint8Array<ArrayBuffer> }
}

function apiUrl(): string {
  return process.env.CHAFGRAM_API_URL ?? 'http://localhost:8080'
}

function buildBody(call: BackendCall): BodyInit | undefined {
  if (call.multipart) {
    const form = new FormData()
    form.append(
      'file',
      new Blob([call.multipart.bytes], { type: call.multipart.contentType }),
      call.multipart.filename,
    )
    return form
  }
  return call.body
}

async function send(
  call: BackendCall,
  cookieHeader: string,
  csrfToken: string | null,
): Promise<Response> {
  const headers = new Headers(call.headers)
  if (cookieHeader) headers.set('cookie', cookieHeader)
  if (csrfToken) headers.set(XSRF_HEADER_NAME, csrfToken)

  return fetch(`${apiUrl()}${call.path}`, {
    method: call.method,
    headers,
    body: buildBody(call),
    redirect: 'manual',
  })
}

/**
 * Call the backend on behalf of the browser.
 *
 * The backend issues its `XSRF-TOKEN` cookie only when it rejects a request
 * that arrived without one (Spring defers the token until it is needed). Such
 * a rejection is a 403 that carries the fresh token — so the call is retried
 * once with that token, which keeps the browser's very first mutation working
 * without a dedicated token-priming round trip.
 */
export async function callBackend(call: BackendCall): Promise<Response> {
  const incoming = getRequest()
  const jar = parseCookieHeader(incoming.headers.get('cookie'))
  const token = xsrfToken(jar)

  let response = await send(call, serializeCookieHeader(jar), token)
  let setCookies = response.headers.getSetCookie()

  if (response.status === 403) {
    const issued = xsrfTokenFromSetCookies(setCookies)
    if (issued && issued !== token) {
      const retryJar = applySetCookies(jar, setCookies)
      response = await send(call, serializeCookieHeader(retryJar), issued)
      setCookies = [...setCookies, ...response.headers.getSetCookie()]
    }
  }

  for (const raw of setCookies) {
    appendResponseHeader('set-cookie', raw)
  }

  return response
}

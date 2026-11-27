import {
  appendResponseHeader,
  getRequest,
  setResponseHeader,
} from '@tanstack/react-start/server'
import { ACCESS_TOKEN_HEADER, RELOGIN_HEADER, REFRESH_COOKIE_NAME } from './auth-client'
import { parseCookieHeader, serializeCookieHeader } from './http'

/**
 * Server-only proxy to the Spring Boot backend — the single BFF egress point.
 *
 * It forwards the browser's in-memory bearer as `Authorization`. On an
 * expired token (401) it exchanges the browser's HttpOnly refresh cookie
 * once, relays the rotated cookie, and resends the original request with the
 * new bearer. A second 401 is terminal.
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

const AUTH_PATH_PREFIX = '/api/v1/auth/'
const REFRESH_PATH = '/api/v1/auth/refresh'

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

/** Only the access token crosses the browser→BFF boundary; cookies never do. */
function requestAuthorization(): string | null {
  return getRequest().headers.get('authorization')
}

function refreshCookieHeader(): string | null {
  const jar = parseCookieHeader(getRequest().headers.get('cookie'))
  const refresh = jar[REFRESH_COOKIE_NAME]
  if (!refresh) return null
  // Forward only the refresh cookie, never other browser cookies.
  return serializeCookieHeader({ [REFRESH_COOKIE_NAME]: refresh })
}

async function send(call: BackendCall, authorization: string | null): Promise<Response> {
  const headers = new Headers(call.headers)
  if (authorization) headers.set('Authorization', authorization)

  return fetch(`${apiUrl()}${call.path}`, {
    method: call.method,
    headers,
    body: buildBody(call),
    redirect: 'manual',
  })
}

interface RefreshOutcome {
  accessToken: string | null
  setCookies: string[]
}

async function performRefresh(): Promise<RefreshOutcome> {
  const cookie = refreshCookieHeader()
  if (!cookie) return { accessToken: null, setCookies: [] }

  const response = await fetch(`${apiUrl()}${REFRESH_PATH}`, {
    method: 'POST',
    headers: { cookie },
    redirect: 'manual',
  })
  const setCookies = response.headers.getSetCookie()
  if (!response.ok) return { accessToken: null, setCookies }

  const payload = (await response.json()) as { accessToken?: unknown }
  return {
    accessToken: typeof payload.accessToken === 'string' ? payload.accessToken : null,
    setCookies,
  }
}

function relaySetCookies(setCookies: string[]): void {
  for (const raw of setCookies) appendResponseHeader('set-cookie', raw)
}

/**
 * Silent refresh used by the bootstrap server function. The browser only
 * receives the rotated HttpOnly cookie and a new access token in memory; the
 * raw refresh token is never part of a server-function result.
 */
export async function refreshAccessToken(): Promise<{
  ok: boolean
  accessToken: string | null
}> {
  const outcome = await performRefresh()
  relaySetCookies(outcome.setCookies)
  if (outcome.accessToken) setResponseHeader(ACCESS_TOKEN_HEADER, outcome.accessToken)

  return { ok: Boolean(outcome.accessToken), accessToken: outcome.accessToken }
}

export async function callBackend(call: BackendCall): Promise<Response> {
  const authorization = requestAuthorization()
  let response = await send(call, authorization)
  let setCookies = response.headers.getSetCookie()

  // Auth endpoints must not recurse into /refresh (e.g. a bad login is a 401).
  if (response.status === 401 && !call.path.startsWith(AUTH_PATH_PREFIX)) {
    const refresh = await performRefresh()
    setCookies = [...setCookies, ...refresh.setCookies]

    if (refresh.accessToken) {
      // Tell the browser to swap its in-memory token, then send the bearer on
      // the single allowed retry.
      setResponseHeader(ACCESS_TOKEN_HEADER, refresh.accessToken)
      response = await send(call, `Bearer ${refresh.accessToken}`)
      setCookies = [...setCookies, ...response.headers.getSetCookie()]
      if (response.status === 401) setResponseHeader(RELOGIN_HEADER, '1')
    } else {
      // Failed refresh (or no refresh cookie): fail closed, no retry.
      setResponseHeader(RELOGIN_HEADER, '1')
    }
  }

  relaySetCookies(setCookies)
  return response
}

/**
 * Browser-side half of the token BFF channel.
 *
 * The access JWT lives only in this module's memory — never in localStorage,
 * sessionStorage or a JavaScript-readable cookie. The refresh token lives in
 * the backend's HttpOnly `chafgram_refresh` cookie and is never read here.
 *
 * TanStack Start server functions are ordinary same-origin fetches. The BFF
 * fetch below attaches the in-memory bearer to every one of them (GraphQL,
 * media re-sign, upload, logout), which is what lets server functions such as
 * `refreshMediaUrl` authenticate the viewer with `getRequestHeader`. It also
 * applies the small response-header channel the BFF uses to report a rotated
 * access token or a failed refresh back to this store.
 */

export const ACCESS_TOKEN_HEADER = 'x-chafgram-access-token'
export const RELOGIN_HEADER = 'x-chafgram-relogin'
export const REFRESH_COOKIE_NAME = 'chafgram_refresh'

export type AuthEvent = 'token-changed' | 'signed-out'

let accessToken: string | null = null
let reloginRequired = false
const listeners = new Set<(event: AuthEvent) => void>()

function emit(event: AuthEvent): void {
  for (const listener of listeners) listener(event)
}

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
  emit('token-changed')
}

export function clearAccessToken(): void {
  accessToken = null
  emit('signed-out')
}

/** Clear the token and remember that the UI needs to send the visitor to /login. */
export function signalReLogin(): void {
  clearAccessToken()
  reloginRequired = true
}

/** Read-and-clear flag so a guard mounting after a failed refresh still reacts. */
export function consumeReLogin(): boolean {
  const pending = reloginRequired
  reloginRequired = false
  return pending
}

export function subscribeAuth(listener: (event: AuthEvent) => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/**
 * Wrap the fetch TanStack Start uses for all BFF server-function calls.
 *
 * This is *not* a backend client: it only decorates the browser→BFF hop. The
 * only module that talks to Spring Boot is `backend-proxy.ts`.
 */
export function createAuthFetch(baseFetch: typeof fetch = fetch): typeof fetch {
  return (input, init) => {
    const headers = new Headers(init?.headers)
    const token = getAccessToken()
    if (token) headers.set('Authorization', `Bearer ${token}`)

    return baseFetch(input, { ...init, headers }).then((response) => {
      const rotated = response.headers.get(ACCESS_TOKEN_HEADER)
      if (rotated) setAccessToken(rotated)

      if (response.headers.get(RELOGIN_HEADER)) signalReLogin()

      return response
    })
  }
}

export interface RefreshResult {
  ok: boolean
  accessToken?: string | null
}

export interface BootstrapOptions {
  /** Public pages render without a session and never touch the refresh cookie. */
  isPublic: boolean
  refresh: () => Promise<RefreshResult>
}

/**
 * Silent refresh before the first authenticated GraphQL operation.
 *
 * A reload wipes in-memory access tokens, but the HttpOnly refresh cookie is
 * still sent by the browser. JavaScript cannot inspect that cookie, so the
 * decision whether a refresh token exists is made by the backend: on a
 * protected page with no in-memory token we ask it once, and a failed exchange
 * is treated as signed out.
 */
let bootstrapInFlight: Promise<void> | null = null

export function bootstrapAuth({ isPublic, refresh }: BootstrapOptions): Promise<void> {
  if (isPublic || getAccessToken()) return Promise.resolve()

  // React StrictMode runs effects twice in development. A refresh rotates the
  // opaque token, so a second concurrent exchange would replay the revoked
  // token and falsely log the visitor out. Share one in-flight attempt.
  if (bootstrapInFlight) return bootstrapInFlight

  bootstrapInFlight = (async () => {
    try {
      const result = await refresh()
      if (result.ok && result.accessToken) {
        setAccessToken(result.accessToken)
      } else {
        signalReLogin()
      }
    } catch {
      signalReLogin()
    }
  })().finally(() => {
    bootstrapInFlight = null
  })

  return bootstrapInFlight
}

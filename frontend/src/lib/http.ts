/**
 * Cookie and CSRF helpers shared by the BFF server functions.
 *
 * The backend authenticates with a session cookie (JSESSIONID) and protects
 * every mutating request with the double-submit pattern: it issues a
 * JavaScript-readable `XSRF-TOKEN` cookie and expects the same value in the
 * `X-XSRF-TOKEN` header.
 *
 * These helpers are pure on purpose — the tricky string handling is unit
 * tested instead of being buried in a request handler.
 */

export const XSRF_COOKIE_NAME = 'XSRF-TOKEN'
export const XSRF_HEADER_NAME = 'X-XSRF-TOKEN'

/** Cookie name → value. */
export type CookieJar = Record<string, string>

function decodeCookieValue(value: string): string {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

function encodeCookieValue(value: string): string {
  return encodeURIComponent(value)
}

/** Parse a `Cookie:` request header into a jar. */
export function parseCookieHeader(header: string | null | undefined): CookieJar {
  const jar: CookieJar = {}
  if (!header) return jar

  for (const part of header.split(';')) {
    const separator = part.indexOf('=')
    if (separator < 0) continue
    const name = part.slice(0, separator).trim()
    if (!name) continue
    jar[name] = decodeCookieValue(part.slice(separator + 1).trim())
  }
  return jar
}

/** Serialize a jar back into a `Cookie:` request header. */
export function serializeCookieHeader(jar: CookieJar): string {
  return Object.entries(jar)
    .map(([name, value]) => `${name}=${encodeCookieValue(value)}`)
    .join('; ')
}

/** The double-submit CSRF token held in the jar, if any. */
export function xsrfToken(jar: CookieJar): string | null {
  return jar[XSRF_COOKIE_NAME] ?? null
}

/**
 * Apply backend `Set-Cookie` headers to a jar.
 *
 * An expired cookie (empty value, `Expires` in the past, or `Max-Age=0`)
 * removes the entry — that is how the backend clears the CSRF cookie after a
 * login token rotation and how it invalidates the session on logout.
 */
export function applySetCookies(jar: CookieJar, setCookies: string[]): CookieJar {
  const next: CookieJar = { ...jar }

  for (const raw of setCookies) {
    const pair = raw.split(';')[0]
    const separator = pair.indexOf('=')
    if (separator < 0) continue
    const name = pair.slice(0, separator).trim()
    if (!name) continue
    const value = pair.slice(separator + 1).trim()

    const expired = value === '' || /max-age=0/i.test(raw) || /expires=thu, 01 jan 1970/i.test(raw)
    if (expired) {
      delete next[name]
    } else {
      next[name] = decodeCookieValue(value)
    }
  }

  return next
}

/** The CSRF token a backend response issued, if it issued one. */
export function xsrfTokenFromSetCookies(setCookies: string[]): string | null {
  return xsrfToken(applySetCookies({}, setCookies))
}

/**
 * Cookie-header helpers shared by the BFF server functions.
 *
 * The only cookie the BFF handles is the backend's HttpOnly refresh cookie;
 * browser JavaScript never reads it. These helpers are pure on purpose — the
 * tricky string handling is unit tested instead of being buried in a request
 * handler.
 */

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

import { describe, expect, it } from 'vitest'
import {
  applySetCookies,
  parseCookieHeader,
  serializeCookieHeader,
  xsrfToken,
  xsrfTokenFromSetCookies,
} from './http'

describe('parseCookieHeader', () => {
  it('parses names and values', () => {
    expect(parseCookieHeader('JSESSIONID=abc123; XSRF-TOKEN=token-1')).toEqual({
      JSESSIONID: 'abc123',
      'XSRF-TOKEN': 'token-1',
    })
  })

  it('returns an empty jar for a missing header', () => {
    expect(parseCookieHeader(null)).toEqual({})
    expect(parseCookieHeader(undefined)).toEqual({})
    expect(parseCookieHeader('')).toEqual({})
  })

  it('decodes percent-encoded values', () => {
    expect(parseCookieHeader('XSRF-TOKEN=a%20b')).toEqual({ 'XSRF-TOKEN': 'a b' })
  })

  it('keeps values that contain an equals sign', () => {
    expect(parseCookieHeader('XSRF-TOKEN=a=b')).toEqual({ 'XSRF-TOKEN': 'a=b' })
  })

  it('ignores malformed segments', () => {
    expect(parseCookieHeader('novalue; =orphan; XSRF-TOKEN=t')).toEqual({ 'XSRF-TOKEN': 't' })
  })
})

describe('serializeCookieHeader', () => {
  it('round-trips a parsed header', () => {
    const jar = parseCookieHeader('JSESSIONID=abc123; XSRF-TOKEN=token-1')
    expect(serializeCookieHeader(jar)).toBe('JSESSIONID=abc123; XSRF-TOKEN=token-1')
  })

  it('encodes values that need it', () => {
    expect(serializeCookieHeader({ 'XSRF-TOKEN': 'a b' })).toBe('XSRF-TOKEN=a%20b')
  })
})

describe('xsrfToken', () => {
  it('reads the double-submit token', () => {
    expect(xsrfToken({ 'XSRF-TOKEN': 'token-1' })).toBe('token-1')
  })

  it('is null when the cookie is missing', () => {
    expect(xsrfToken({ JSESSIONID: 'abc' })).toBeNull()
  })
})

describe('applySetCookies', () => {
  it('adds and updates cookies', () => {
    const next = applySetCookies({ JSESSIONID: 'old' }, ['JSESSIONID=new; Path=/; HttpOnly'])
    expect(next).toEqual({ JSESSIONID: 'new' })
  })

  it('removes cookies cleared by the backend', () => {
    const next = applySetCookies(
      { 'XSRF-TOKEN': 'old', JSESSIONID: 'abc' },
      ['XSRF-TOKEN=; Expires=Thu, 01 Jan 1970 00:00:10 GMT; Path=/'],
    )
    expect(next).toEqual({ JSESSIONID: 'abc' })
  })

  it('removes cookies with Max-Age=0', () => {
    const next = applySetCookies({ JSESSIONID: 'abc' }, ['JSESSIONID=; Max-Age=0; Path=/'])
    expect(next).toEqual({})
  })

  it('keeps the jar untouched when nothing is set', () => {
    const jar = { JSESSIONID: 'abc' }
    expect(applySetCookies(jar, [])).toEqual(jar)
  })
})

describe('xsrfTokenFromSetCookies', () => {
  it('finds a freshly issued token', () => {
    expect(xsrfTokenFromSetCookies(['XSRF-TOKEN=fresh; Path=/'])).toBe('fresh')
  })

  it('is null when the response only clears the cookie', () => {
    expect(
      xsrfTokenFromSetCookies(['XSRF-TOKEN=; Expires=Thu, 01 Jan 1970 00:00:10 GMT; Path=/']),
    ).toBeNull()
  })

  it('is null when no CSRF cookie is present', () => {
    expect(xsrfTokenFromSetCookies(['JSESSIONID=abc; Path=/'])).toBeNull()
  })
})

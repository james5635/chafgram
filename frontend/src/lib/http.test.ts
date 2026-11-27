import { describe, expect, it } from 'vitest'
import { parseCookieHeader, serializeCookieHeader } from './http'

describe('parseCookieHeader', () => {
  it('parses names and values', () => {
    expect(parseCookieHeader('chafgram_refresh=abc123; other=keep-me')).toEqual({
      chafgram_refresh: 'abc123',
      other: 'keep-me',
    })
  })

  it('returns an empty jar for a missing header', () => {
    expect(parseCookieHeader(null)).toEqual({})
    expect(parseCookieHeader(undefined)).toEqual({})
    expect(parseCookieHeader('')).toEqual({})
  })

  it('decodes percent-encoded values', () => {
    expect(parseCookieHeader('chafgram_refresh=a%20b')).toEqual({ chafgram_refresh: 'a b' })
  })

  it('keeps values that contain an equals sign', () => {
    expect(parseCookieHeader('chafgram_refresh=a=b')).toEqual({ chafgram_refresh: 'a=b' })
  })

  it('ignores malformed segments', () => {
    expect(parseCookieHeader('novalue; =orphan; chafgram_refresh=t')).toEqual({
      chafgram_refresh: 't',
    })
  })
})

describe('serializeCookieHeader', () => {
  it('round-trips a parsed header', () => {
    const jar = parseCookieHeader('chafgram_refresh=abc123; other=keep-me')
    expect(serializeCookieHeader(jar)).toBe('chafgram_refresh=abc123; other=keep-me')
  })

  it('encodes values that need it', () => {
    expect(serializeCookieHeader({ chafgram_refresh: 'a b' })).toBe('chafgram_refresh=a%20b')
  })
})

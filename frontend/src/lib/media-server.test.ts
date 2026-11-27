import { describe, expect, it } from 'vitest'
import { decodeBase64, mediaAuthHeaders } from './media-server'

describe('decodeBase64', () => {
  it('decodes plain bytes', () => {
    const bytes = decodeBase64(btoa('hello'))

    expect(Array.from(bytes)).toEqual([104, 101, 108, 108, 111])
  })

  it('decodes an empty payload', () => {
    expect(decodeBase64('')).toHaveLength(0)
  })

  it('decodes binary data', () => {
    const bytes = decodeBase64(btoa(String.fromCharCode(0, 127, 255)))

    expect(Array.from(bytes)).toEqual([0, 127, 255])
  })
})

describe('mediaAuthHeaders', () => {
  it('forwards the browser-injected bearer so an expired media URL can be re-signed', () => {
    expect(mediaAuthHeaders('Bearer jwt-from-browser')).toEqual({
      Authorization: 'Bearer jwt-from-browser',
    })
  })

  it('sends no auth header when the browser has no access token', () => {
    expect(mediaAuthHeaders(null)).toEqual({})
  })
})

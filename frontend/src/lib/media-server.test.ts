import { describe, expect, it } from 'vitest'
import { decodeBase64 } from './media-server'

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

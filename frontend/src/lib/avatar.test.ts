import { describe, it, expect } from 'vitest'
import { avatarInitial, avatarTint } from './avatar'

describe('avatarInitial', () => {
  it('uses only the first character, uppercased', () => {
    expect(avatarInitial('dara chan')).toBe('D')
    expect(avatarInitial('  rojame')).toBe('R')
  })

  it('falls back to a question mark for an empty name', () => {
    expect(avatarInitial('')).toBe('?')
    expect(avatarInitial('   ')).toBe('?')
  })
})

describe('avatarTint', () => {
  it('is stable for the same name, ignoring case and surrounding space', () => {
    expect(avatarTint('Dara Chan')).toBe(avatarTint('dara chan'))
    expect(avatarTint('Dara Chan')).toBe(avatarTint('  Dara Chan  '))
  })

  it('maps to one of the declared avatar tint tokens', () => {
    expect(avatarTint('Rojame')).toMatch(/^var\(--avatar-tint-[1-6]\)$/)
  })
})

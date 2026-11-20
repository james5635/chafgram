import { describe, it, expect } from 'vitest'
import { isPublicPath, PUBLIC_PATHS } from './public-paths'

describe('isPublicPath', () => {
  it('treats welcome and login as public (app chrome hidden)', () => {
    for (const path of PUBLIC_PATHS) {
      expect(isPublicPath(path)).toBe(true)
    }
  })

  it('treats authenticated screens as non-public', () => {
    for (const path of ['/', '/explore', '/messages', '/notifications', '/profile/rojame', '/compose', '/settings']) {
      expect(isPublicPath(path)).toBe(false)
    }
  })

  it('does not treat partial or trailing-slash paths as public', () => {
    expect(isPublicPath('/welcome/')).toBe(false)
    expect(isPublicPath('/login?redirect=/')).toBe(false)
    expect(isPublicPath('/welcome-extra')).toBe(false)
  })
})

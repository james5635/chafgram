import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

import '@testing-library/jest-dom/vitest'

// Unmount components and remove them from the DOM after every test so queries
// never match elements left over from a previous test.
afterEach(() => {
  cleanup()
})

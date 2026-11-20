import { defineConfig } from 'vitest/config'

export default defineConfig({
  resolve: {
    tsconfigPaths: true,
  },
  test: {
    /** jsdom gives us a DOM for React component tests without a browser. */
    environment: 'jsdom',
    /** Make jest-dom matchers (toBeInTheDocument, toBeDisabled, …) available. */
    setupFiles: ['./src/test/setupTests.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    /** Keep output deterministic in CI. */
    isolate: true,
  },
})

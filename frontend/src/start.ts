import { createStart } from '@tanstack/react-start'
import { createAuthFetch } from './lib/auth-client'

/**
 * All TanStack Start server-function calls go through the BFF fetch wrapper.
 * It puts the in-memory bearer on the browser→BFF hop (including media
 * re-signing) and applies the rotated-token / re-login response headers.
 * Start only uses this on the client; during SSR server functions are called
 * directly, so no browser token state is involved.
 */
export const startInstance = createStart(() => ({
  serverFns: { fetch: createAuthFetch() },
}))

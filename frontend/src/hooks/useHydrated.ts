import { useSyncExternalStore } from 'react'

const subscribe = () => () => {}
const getSnapshot = () => true
const getServerSnapshot = () => false

/**
 * False during SSR and the first client render, true afterwards. Use it to
 * avoid hydration mismatches for content that depends on browser-only data
 * (cookies, session, localStorage).
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)
}

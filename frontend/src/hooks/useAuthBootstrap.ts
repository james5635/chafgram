import { useEffect, useState } from 'react'
import { bootstrapAuth } from '../lib/auth-client'
import { refreshRequest } from '../lib/session-server'

/**
 * Gate route content on the client-side silent refresh.
 *
 * The initial value is `false` on both server and first client render, so SSR
 * only emits the public shell. After hydration we try one refresh (public
 * pages and already-tokened sessions skip it) and reveal the route.
 */
export function useAuthBootstrap(isPublic: boolean): boolean {
  const [booted, setBooted] = useState(false)

  useEffect(() => {
    let active = true
    void bootstrapAuth({ isPublic, refresh: () => refreshRequest({}) })
      .catch(() => {
        // bootstrapAuth normally converts failures into a re-login signal; a
        // transport throw must never strand the public shell forever.
      })
      .finally(() => {
        if (active) setBooted(true)
      })
    return () => {
      active = false
    }
  }, [isPublic])

  return booted
}

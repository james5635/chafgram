import { useQuery } from '@apollo/client/react'
import { useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'
import { MeDocument } from '../generated/graphql'
import { consumeReLogin, subscribeAuth } from '../lib/auth-client'
import { isUnauthorizedError } from '../lib/errors'

/**
 * Sends the visitor to the sign-in screen when authentication is gone.
 *
 * Two sources funnel through this guard: GraphQL authorization errors from the
 * backend, and the BFF's failed-refresh signal (which may arrive before this
 * hook mounts during silent bootstrap).
 */
export function useSessionGuard() {
  const navigate = useNavigate()
  const { data, error, loading } = useQuery(MeDocument)

  useEffect(() => {
    if (consumeReLogin()) {
      void navigate({ to: '/login' })
      return
    }

    const unsubscribe = subscribeAuth((event) => {
      if (event === 'signed-out') void navigate({ to: '/login' })
    })
    return unsubscribe
  }, [navigate])

  useEffect(() => {
    if (!error || !isUnauthorizedError(error)) return
    void navigate({ to: '/login' })
  }, [error, navigate])

  return { me: data?.me, loading }
}

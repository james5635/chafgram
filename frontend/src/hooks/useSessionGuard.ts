import { useQuery } from '@apollo/client/react'
import { useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'
import { MeDocument } from '../generated/graphql'
import { isUnauthorizedError } from '../lib/errors'

/**
 * Sends the visitor to the sign-in screen when the session is missing.
 *
 * The backend reports authorization failures as GraphQL errors, so every
 * authenticated screen funnels through this guard instead of rendering an
 * empty shell.
 */
export function useSessionGuard() {
  const navigate = useNavigate()
  const { data, error, loading } = useQuery(MeDocument)

  useEffect(() => {
    if (!error || !isUnauthorizedError(error)) return
    void navigate({ to: '/login' })
  }, [error, navigate])

  return { me: data?.me, loading }
}

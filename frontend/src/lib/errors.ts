import { CombinedGraphQLErrors } from '@apollo/client/errors'

/**
 * True when a failed operation means "you are not signed in".
 *
 * The backend reports authorization failures as GraphQL errors with an
 * `extensions.classification` field (`UNAUTHORIZED` / `UNAUTHENTICATED`);
 * network and server failures must not trigger a redirect to the sign-in
 * screen, so only those classifications count.
 */
export function isUnauthorizedError(error: unknown): boolean {
  if (CombinedGraphQLErrors.is(error)) {
    return error.errors.some((graphQLError) => {
      const classification = graphQLError.extensions?.classification
      return classification === 'UNAUTHORIZED' || classification === 'UNAUTHENTICATED'
    })
  }

  return error instanceof Error && /unauthorized|unauthenticated/i.test(error.message)
}

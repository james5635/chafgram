import { ApolloClient, ApolloLink, InMemoryCache, Observable } from '@apollo/client'
import { CombinedGraphQLErrors } from '@apollo/client/errors'
import { print } from 'graphql'
import { signalReLogin } from './auth-client'
import { graphqlRequest } from './graphql-server'

/**
 * Apollo link that sends every operation through the TanStack Start BFF server
 * function instead of talking to the backend directly from the browser.
 *
 * The global BFF fetch attaches the in-memory bearer; the BFF refreshes it
 * once on a backend 401. A remaining 401 means re-authentication is required.
 */
export const bffLink = new ApolloLink((operation) =>
  new Observable((observer) => {
    void graphqlRequest({
      data: {
        query: print(operation.query),
        variables: operation.variables,
        operationName: operation.operationName,
      },
    })
      .then((result) => {
        if (result.errors?.length) {
          observer.error(
            new CombinedGraphQLErrors({
              data: result.data ?? undefined,
              errors: result.errors,
            }),
          )
          return
        }
        observer.next({ data: result.data ?? {} })
        observer.complete()
      })
      .catch((error: unknown) => {
        if (error instanceof Error && /\b401\b/.test(error.message)) signalReLogin()
        observer.error(error)
      })
  }),
)

export function createApolloClient() {
  return new ApolloClient({
    link: bffLink,
    cache: new InMemoryCache(),
    defaultOptions: {
      watchQuery: { fetchPolicy: 'cache-and-network' },
      query: { fetchPolicy: 'network-only' },
    },
  })
}

/** Shared client for the app (one cache, one BFF link). */
export const apolloClient = createApolloClient()

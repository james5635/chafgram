import { ApolloClient, ApolloLink, InMemoryCache, Observable } from '@apollo/client'
import { CombinedGraphQLErrors } from '@apollo/client/errors'
import { print } from 'graphql'
import { graphqlRequest } from './graphql-server'

/**
 * Apollo link that sends every operation through the TanStack Start BFF server
 * function instead of talking to the backend directly from the browser.
 *
 * The BFF owns the session cookie and the CSRF token, so the browser never
 * needs to know the backend URL or how authentication is wired.
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
      .catch((error: unknown) => observer.error(error))
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

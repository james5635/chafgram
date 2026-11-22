import { createServerFn } from '@tanstack/react-start'
import { callBackend } from './backend-proxy'

/** JSON-safe value shape required by TanStack Start's serializable RPC types. */
export type JsonValue = string | number | boolean | null | JsonValue[] | { [key: string]: JsonValue }

export interface GraphQLRequestPayload {
  query: string
  variables?: Record<string, unknown>
  operationName?: string
}

export interface GraphQLError {
  message: string
  extensions?: { [key: string]: JsonValue }
}

export interface GraphQLResponse {
  data?: { [key: string]: JsonValue } | null
  errors?: GraphQLError[]
}

/**
 * BFF: the only egress point to the Spring Boot GraphQL endpoint.
 *
 * Runs on the server, forwards the browser's session cookie and the
 * double-submit CSRF token, and never exposes backend URLs or credentials to
 * the browser. Apollo talks to this function through `bffLink`.
 */
export const graphqlRequest = createServerFn({ method: 'POST' })
  .validator((payload: GraphQLRequestPayload) => payload)
  .handler(async ({ data }): Promise<GraphQLResponse> => {
    const response = await callBackend({
      path: '/graphql',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      throw new Error(`Backend GraphQL request failed: ${response.status} ${response.statusText}`)
    }

    return (await response.json()) as GraphQLResponse
  })

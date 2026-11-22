import { describe, expect, it, vi, beforeEach } from 'vitest'
import { gql } from '@apollo/client'

const { graphqlRequest } = vi.hoisted(() => ({ graphqlRequest: vi.fn() }))

vi.mock('./graphql-server', () => ({ graphqlRequest }))

import { createApolloClient } from './apollo'

const QUERY = gql`
  query Me {
    me {
      id
      username
    }
  }
`

beforeEach(() => {
  graphqlRequest.mockReset()
})

describe('createApolloClient', () => {
  it('sends operations through the BFF server function', async () => {
    graphqlRequest.mockResolvedValue({ data: { me: { id: '1', username: 'rojame' } } })

    const client = createApolloClient()
    const result = await client.query({ query: QUERY, variables: { first: 20 } })

    expect(result.data).toEqual({ me: { id: '1', username: 'rojame' } })
    expect(graphqlRequest).toHaveBeenCalledTimes(1)

    const [call] = graphqlRequest.mock.calls[0]
    expect(call.data.query).toContain('query Me')
    expect(call.data.query).toContain('username')
    expect(call.data.variables).toEqual({ first: 20 })
  })

  it('turns GraphQL errors into a failed query', async () => {
    graphqlRequest.mockResolvedValue({
      data: null,
      errors: [{ message: 'Unauthorized', extensions: { classification: 'UNAUTHORIZED' } }],
    })

    const client = createApolloClient()

    await expect(client.query({ query: QUERY })).rejects.toThrow(/Unauthorized/)
  })

  it('propagates transport failures', async () => {
    graphqlRequest.mockRejectedValue(new Error('Backend GraphQL request failed: 500'))

    const client = createApolloClient()

    await expect(client.query({ query: QUERY })).rejects.toThrow(/Backend GraphQL request failed/)
  })
})

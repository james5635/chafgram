import { describe, expect, it, vi, beforeEach } from 'vitest'
import { gql } from '@apollo/client'

const { graphqlRequest } = vi.hoisted(() => ({ graphqlRequest: vi.fn() }))
const { signalReLogin } = vi.hoisted(() => ({ signalReLogin: vi.fn() }))

vi.mock('./graphql-server', () => ({ graphqlRequest }))
vi.mock('./auth-client', () => ({ signalReLogin }))

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
  signalReLogin.mockReset()
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
    expect(signalReLogin).not.toHaveBeenCalled()
  })

  it('signals re-login when the BFF still returns 401 after the refresh retry', async () => {
    graphqlRequest.mockRejectedValue(new Error('Backend GraphQL request failed: 401 Unauthorized'))

    const client = createApolloClient()

    await expect(client.query({ query: QUERY })).rejects.toThrow(/401/)
    expect(signalReLogin).toHaveBeenCalledTimes(1)
  })
})

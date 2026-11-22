import { CombinedGraphQLErrors } from '@apollo/client/errors'
import { describe, expect, it } from 'vitest'
import { isUnauthorizedError } from './errors'

function graphQLError(classification?: string) {
  return new CombinedGraphQLErrors({
    data: null,
    errors: [
      {
        message: 'Unauthorized',
        extensions: classification ? { classification } : undefined,
      },
    ],
  })
}

describe('isUnauthorizedError', () => {
  it('detects the UNAUTHORIZED classification', () => {
    expect(isUnauthorizedError(graphQLError('UNAUTHORIZED'))).toBe(true)
  })

  it('detects the UNAUTHENTICATED classification', () => {
    expect(isUnauthorizedError(graphQLError('UNAUTHENTICATED'))).toBe(true)
  })

  it('ignores other classifications', () => {
    expect(isUnauthorizedError(graphQLError('FORBIDDEN'))).toBe(false)
  })

  it('ignores errors without a classification', () => {
    expect(isUnauthorizedError(graphQLError())).toBe(false)
  })

  it('falls back to the message for plain errors', () => {
    expect(isUnauthorizedError(new Error('Unauthorized'))).toBe(true)
    expect(isUnauthorizedError(new Error('Network request failed'))).toBe(false)
  })

  it('is false for values that are not errors', () => {
    expect(isUnauthorizedError(undefined)).toBe(false)
    expect(isUnauthorizedError('nope')).toBe(false)
  })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

const mocks = vi.hoisted(() => ({
  navigate: vi.fn(),
  useQuery: vi.fn(),
  consumeReLogin: vi.fn(),
  subscribeAuth: vi.fn(),
  isUnauthorizedError: vi.fn(),
}))

vi.mock('@tanstack/react-router', () => ({ useNavigate: () => mocks.navigate }))
vi.mock('@apollo/client/react', () => ({ useQuery: mocks.useQuery }))
vi.mock('../generated/graphql', () => ({ MeDocument: {} }))
vi.mock('../lib/auth-client', () => ({
  consumeReLogin: mocks.consumeReLogin,
  subscribeAuth: mocks.subscribeAuth,
}))
vi.mock('../lib/errors', () => ({ isUnauthorizedError: mocks.isUnauthorizedError }))

import { useSessionGuard } from './useSessionGuard'

beforeEach(() => {
  vi.clearAllMocks()
  mocks.useQuery.mockReturnValue({ data: undefined, error: undefined, loading: false })
  mocks.consumeReLogin.mockReturnValue(false)
  mocks.isUnauthorizedError.mockReturnValue(false)
  mocks.subscribeAuth.mockReturnValue(vi.fn())
})

describe('useSessionGuard', () => {
  it('redirects when a failed refresh left a re-login signal before mount', async () => {
    mocks.consumeReLogin.mockReturnValue(true)

    renderHook(() => useSessionGuard())

    await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith({ to: '/login' }))
  })

  it('redirects when the BFF signals sign-out while a screen is mounted', async () => {
    let listener: ((event: string) => void) | undefined
    mocks.subscribeAuth.mockImplementation((callback: (event: string) => void) => {
      listener = callback
      return vi.fn()
    })

    renderHook(() => useSessionGuard())
    listener?.('signed-out')

    await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith({ to: '/login' }))
  })

  it('ignores non-sign-out auth events', async () => {
    let listener: ((event: string) => void) | undefined
    mocks.subscribeAuth.mockImplementation((callback: (event: string) => void) => {
      listener = callback
      return vi.fn()
    })

    renderHook(() => useSessionGuard())
    listener?.('token-changed')

    expect(mocks.navigate).not.toHaveBeenCalled()
  })

  it('still redirects on a GraphQL authorization error', async () => {
    const error = new Error('Unauthorized')
    mocks.useQuery.mockReturnValue({ data: undefined, error, loading: false })
    mocks.isUnauthorizedError.mockReturnValue(true)

    renderHook(() => useSessionGuard())

    await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith({ to: '/login' }))
  })
})

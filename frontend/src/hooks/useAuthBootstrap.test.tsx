import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

const mocks = vi.hoisted(() => ({
  bootstrapAuth: vi.fn(),
  refreshRequest: vi.fn(),
}))

vi.mock('../lib/auth-client', () => ({ bootstrapAuth: mocks.bootstrapAuth }))
vi.mock('../lib/session-server', () => ({ refreshRequest: mocks.refreshRequest }))

import { useAuthBootstrap } from './useAuthBootstrap'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('useAuthBootstrap', () => {
  it('renders the public shell first, then reveals content after the silent refresh', async () => {
    mocks.bootstrapAuth.mockResolvedValue(undefined)

    const { result } = renderHook(() => useAuthBootstrap(false))

    expect(result.current).toBe(false)
    expect(mocks.bootstrapAuth).toHaveBeenCalledWith({
      isPublic: false,
      refresh: expect.any(Function),
    })

    const { refresh } = mocks.bootstrapAuth.mock.calls[0][0]
    await refresh()
    expect(mocks.refreshRequest).toHaveBeenCalledWith({})

    await waitFor(() => expect(result.current).toBe(true))
  })

  it('reveals content even when the refresh call fails', async () => {
    mocks.bootstrapAuth.mockRejectedValue(new Error('offline'))

    const { result } = renderHook(() => useAuthBootstrap(true))

    expect(mocks.bootstrapAuth).toHaveBeenCalledWith({
      isPublic: true,
      refresh: expect.any(Function),
    })
    await waitFor(() => expect(result.current).toBe(true))
  })
})

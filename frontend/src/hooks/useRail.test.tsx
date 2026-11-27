import { beforeEach, describe, expect, it } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { RailProvider, useRail } from './useRail'

function wrapper({ children }: { children: ReactNode }) {
  return <RailProvider>{children}</RailProvider>
}

beforeEach(() => {
  localStorage.removeItem('rail-collapsed')
})

describe('useRail', () => {
  it('defaults to expanded', () => {
    const { result } = renderHook(() => useRail(), { wrapper })

    expect(result.current.collapsed).toBe(false)
  })

  it('toggles the rail and persists the collapsed state', () => {
    const { result } = renderHook(() => useRail(), { wrapper })

    act(() => result.current.toggle())

    expect(result.current.collapsed).toBe(true)
    expect(localStorage.getItem('rail-collapsed')).toBe('1')
  })

  it('restores a saved collapsed state and persists expansion', () => {
    localStorage.setItem('rail-collapsed', '1')
    const { result } = renderHook(() => useRail(), { wrapper })

    expect(result.current.collapsed).toBe(true)

    act(() => result.current.toggle())

    expect(result.current.collapsed).toBe(false)
    expect(localStorage.getItem('rail-collapsed')).toBe('0')
  })
})

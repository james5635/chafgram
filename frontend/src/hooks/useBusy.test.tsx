import { describe, it, expect, vi } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { useBusy } from './useBusy'

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

describe('useBusy', () => {
  it('is busy while the task is pending and clears busy with the result', async () => {
    const gate = deferred<string>()
    const task = vi.fn(() => gate.promise)
    const { result } = renderHook(() => useBusy())

    let pending!: Promise<string | undefined>
    act(() => {
      pending = result.current.run(task)
    })

    expect(result.current.busy).toBe(true)
    expect(task).toHaveBeenCalledTimes(1)

    await act(async () => {
      gate.resolve('done')
      await expect(pending).resolves.toBe('done')
    })

    expect(result.current.busy).toBe(false)
  })

  it('skips a second run while busy without invoking its task', async () => {
    const gate = deferred<string>()
    const firstTask = vi.fn(() => gate.promise)
    const secondTask = vi.fn(async () => 'should not run')
    const { result } = renderHook(() => useBusy())

    let pending!: Promise<string | undefined>
    act(() => {
      pending = result.current.run(firstTask)
    })

    let skipped!: Promise<string | undefined>
    act(() => {
      skipped = result.current.run(secondTask)
    })

    await expect(skipped).resolves.toBeUndefined()
    expect(secondTask).not.toHaveBeenCalled()
    expect(firstTask).toHaveBeenCalledTimes(1)

    await act(async () => {
      gate.resolve('done')
      await pending
    })
    await waitFor(() => expect(result.current.busy).toBe(false))
  })

  it('clears busy and rejects to the caller when the task fails', async () => {
    const gate = deferred<string>()
    const task = vi.fn(() => gate.promise)
    const { result } = renderHook(() => useBusy())

    let pending!: Promise<string | undefined>
    act(() => {
      pending = result.current.run(task)
    })

    expect(result.current.busy).toBe(true)

    await act(async () => {
      gate.reject(new Error('boom'))
      await expect(pending).rejects.toThrow('boom')
    })

    expect(result.current.busy).toBe(false)
  })

  it('allows a later run after a skipped one', async () => {
    const gate = deferred<string>()
    const { result } = renderHook(() => useBusy())

    let pending!: Promise<string | undefined>
    act(() => {
      pending = result.current.run(() => gate.promise)
    })
    act(() => {
      void result.current.run(async () => 'skipped')
    })
    await act(async () => {
      gate.resolve('first')
      await pending
    })

    await act(async () => {
      await expect(result.current.run(async () => 'second')).resolves.toBe('second')
    })
    expect(result.current.busy).toBe(false)
  })
})

import { useCallback, useRef, useState } from 'react'

export interface BusyState {
  busy: boolean
  run: <T>(task: () => Promise<T>) => Promise<T | undefined>
}

/**
 * Tracks one in-flight user action. A second `run` while busy is skipped and
 * resolves to `undefined`, so rapid double clicks cannot duplicate a mutation.
 */
export function useBusy(): BusyState {
  const [busy, setBusy] = useState(false)
  const busyRef = useRef(false)

  const run = useCallback(
    async <T,>(task: () => Promise<T>): Promise<T | undefined> => {
      if (busyRef.current) return undefined

      busyRef.current = true
      setBusy(true)
      try {
        return await task()
      } finally {
        busyRef.current = false
        setBusy(false)
      }
    },
    [],
  )

  return { busy, run }
}

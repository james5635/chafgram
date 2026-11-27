import { createContext, useCallback, useContext, useEffect, useState } from 'react'

export interface RailState {
  collapsed: boolean
  toggle: () => void
}

/** Components used outside the provider simply render the expanded rail. */
const RailContext = createContext<RailState>({ collapsed: false, toggle: () => {} })

export function RailProvider({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = useState(false)

  useEffect(() => {
    setCollapsed(localStorage.getItem('rail-collapsed') === '1')
  }, [])

  const toggle = useCallback(() => {
    const next = !collapsed
    setCollapsed(next)
    localStorage.setItem('rail-collapsed', next ? '1' : '0')
  }, [collapsed])

  return <RailContext.Provider value={{ collapsed, toggle }}>{children}</RailContext.Provider>
}

export function useRail(): RailState {
  return useContext(RailContext)
}

import LeftRail from './LeftRail'
import { useSessionGuard } from '../hooks/useSessionGuard'
import { useRail } from '../hooks/useRail'

interface AppShellProps {
  children: React.ReactNode
  rightRail?: React.ReactNode
}

/** Shared shell for authenticated screens: fixed left rail + offset content. */
export default function AppShell({ children, rightRail }: AppShellProps) {
  useSessionGuard()
  const { collapsed } = useRail()

  return (
    <>
      <LeftRail />
      <div className={`flex min-h-[calc(100vh-14rem)] ${collapsed ? 'lg:pl-16' : 'lg:pl-64'}`}>
        <div className="flex-1 min-w-0">{children}</div>
        {rightRail}
      </div>
    </>
  )
}

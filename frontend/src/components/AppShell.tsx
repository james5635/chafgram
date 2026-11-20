import LeftRail from './LeftRail'

interface AppShellProps {
  children: React.ReactNode
  rightRail?: React.ReactNode
}

/** Shared shell for authenticated screens: fixed left rail + offset content. */
export default function AppShell({ children, rightRail }: AppShellProps) {
  return (
    <>
      <LeftRail />
      <div className="flex min-h-[calc(100vh-14rem)] lg:pl-64">
        <div className="flex-1 min-w-0">{children}</div>
        {rightRail}
      </div>
    </>
  )
}

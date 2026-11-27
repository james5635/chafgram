import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from '@tanstack/react-router'
import { ChevronRight, LogOut, Settings } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { useBusy } from '../hooks/useBusy'
import { clearAccessToken } from '../lib/auth-client'
import { logoutRequest } from '../lib/session-server'

interface AccountMenuProps {
  displayName: string
  username: string
  collapsed?: boolean
}

/** The signed-in member's chip and its Settings / Sign out menu. */
export default function AccountMenu({ displayName, username, collapsed = false }: AccountMenuProps) {
  const [open, setOpen] = useState(false)
  const { busy, run } = useBusy()
  const rootRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const location = useLocation()

  useEffect(() => {
    if (!open) return

    function handlePointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setOpen(false)
        triggerRef.current?.focus()
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [open])

  useEffect(() => {
    setOpen(false)
  }, [location.pathname])

  function handleSignOut() {
    void run(async () => {
      try {
        await logoutRequest({})
      } catch {
        // The BFF signals re-login even on a failed logout; either way the
        // local token must go and the session guard navigates to /login.
      } finally {
        clearAccessToken()
      }
    })
  }

  return (
    <div ref={rootRef} className="relative">
      <button
        ref={triggerRef}
        type="button"
        aria-label="Account menu"
        aria-expanded={open}
        aria-haspopup="true"
        onClick={() => setOpen((value) => !value)}
        className={`flex w-full items-center gap-2 rounded-xl px-2 py-2 text-left no-underline transition hover:bg-[var(--brand-light)]/50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand)] ${collapsed ? 'justify-center' : ''}`}
      >
        <Avatar className="h-8 w-8" aria-label={displayName}>
          <AvatarFallback name={displayName} />
        </Avatar>
        {!collapsed && (
          <>
            <span className="min-w-0 flex-1 leading-none">
              <span className="block truncate text-sm font-semibold text-[var(--sea-ink)]">{displayName}</span>
              <span className="block truncate text-[0.7rem] text-[var(--sea-ink-soft)]">
                {username ? `@${username}` : '…'}
              </span>
            </span>
            <ChevronRight
              className={`h-4 w-4 shrink-0 text-[var(--sea-ink-soft)] transition-transform ${open ? 'rotate-90' : ''}`}
              aria-hidden="true"
            />
          </>
        )}
      </button>

      {open && (
        <div
          aria-label="Account menu options"
          className="absolute bottom-full left-0 right-0 z-30 mb-2 rounded-xl border border-[var(--line)] bg-[var(--surface-strong)] p-1 shadow-lg backdrop-blur-md"
        >
          <Link
            to="/settings"
            aria-disabled={busy || undefined}
            tabIndex={busy ? -1 : undefined}
            onClick={(event) => {
              if (busy) {
                event.preventDefault()
                return
              }
              setOpen(false)
            }}
            className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-[var(--sea-ink)] no-underline transition hover:bg-[var(--brand-light)]/60 aria-disabled:cursor-not-allowed aria-disabled:opacity-60"
          >
            <Settings className="h-4 w-4 shrink-0" aria-hidden="true" />
            Settings
          </Link>
          <button
            type="button"
            disabled={busy}
            onClick={handleSignOut}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium text-[var(--sea-ink)] transition hover:bg-[var(--brand-light)]/60 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <LogOut className="h-4 w-4 shrink-0" aria-hidden="true" />
            {busy ? 'Signing out…' : 'Sign out'}
          </button>
        </div>
      )}
    </div>
  )
}

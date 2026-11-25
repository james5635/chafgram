import { Link, useLocation } from '@tanstack/react-router'
import { Home, Compass, MessageSquare, Bell, User, Plus, ChevronRight, MoreHorizontal } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'

const navLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/explore', label: 'Explore', icon: Compass },
  { to: '/messages', label: 'Messages', icon: MessageSquare, badge: 3 },
  { to: '/notifications', label: 'Notifications', icon: Bell, badge: 5 },
  { to: '/profile/$username', label: 'Profile', icon: User },
  { to: '/settings', label: 'More', icon: MoreHorizontal },
]

export default function LeftRail() {
  const location = useLocation()

  return (
    <aside
      className="fixed left-0 top-14 bottom-0 z-20 flex w-64 flex-col border-r border-[var(--line)] bg-[var(--surface)] px-3 py-4 backdrop-blur-md"
      aria-label="Main navigation"
    >
      <nav className="flex-1 space-y-1">
        {navLinks.map((link) => {
          const Icon = link.icon
          const isActive = link.to === '/profile/$username'
            ? location.pathname.startsWith('/profile/')
            : location.pathname === link.to
          return (
            <Link
              key={link.label}
              to={link.to}
              params={link.to === '/profile/$username' ? { username: 'me' } : undefined}
              className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition no-underline ${
                isActive
                  ? '!bg-[var(--brand-light)] !text-[var(--brand-dark)] font-semibold'
                  : '!text-[var(--sea-ink-soft)] hover:!bg-[var(--brand-light)]/50'
              }`}
            >
              <Icon className="w-5 h-5 shrink-0" aria-hidden="true" />
              {link.label}
              {link.badge && (
                <span className="ml-auto inline-flex h-5 w-5 items-center justify-center rounded-full border border-white bg-[var(--brand)] px-1 text-[0.625rem] font-bold text-white shadow-sm">
                  {link.badge}
                </span>
              )}
            </Link>
          )
        })}
      </nav>

      <div className="mt-auto pt-6 space-y-3">
        <Button className="w-full gap-2" asChild aria-label="Create post">
          <Link to="/compose" className="flex items-center justify-center gap-2">
            <Plus className="h-4 w-4" aria-hidden="true" />
            Create Post
          </Link>
        </Button>
        <Link
          to="/profile/$username"
          params={{ username: 'me' }}
          className="flex items-center gap-2 rounded-xl px-2 py-2 no-underline transition hover:bg-[var(--brand-light)]/50"
          aria-label="Your profile"
        >
          <Avatar className="h-8 w-8">
            <AvatarFallback>RJ</AvatarFallback>
          </Avatar>
          <span className="flex-1 min-w-0 leading-none">
            <span className="block text-sm font-semibold text-[var(--sea-ink)]">Rojame</span>
            <span className="block text-[0.7rem] text-[var(--sea-ink-soft)]">@rojame</span>
          </span>
          <ChevronRight className="h-4 w-4 text-[var(--sea-ink-soft)]" aria-hidden="true" />
        </Link>
      </div>
    </aside>
  )
}

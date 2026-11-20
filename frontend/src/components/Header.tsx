import { Link } from '@tanstack/react-router'
import ThemeToggle from './ThemeToggle'
import Avatar from './Avatar'

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/explore', label: 'Explore' },
  { to: '/messages', label: 'Messages' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/profile/me', label: 'Profile' },
]

export default function Header() {
  return (
    <header className="sticky top-0 z-50 border-b border-[var(--line)] bg-[var(--header-bg)] px-4 backdrop-blur-lg">
      <nav className="page-wrap flex flex-wrap items-center gap-x-3 gap-y-2 py-3 sm:py-4">
        <div className="order-1 flex-shrink-0">
          <Link
            to="/"
            className="inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] no-underline shadow-[0_8px_24px_rgba(30,90,72,0.08)] sm:px-4 sm:py-2"
          >
            <span className="h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]" />
            <span className="font-bold tracking-tight">Chafgram</span>
          </Link>
        </div>

        <div className="order-2 flex-1 min-w-0">
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--sea-ink-soft)]">
              <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M11.742 10.344a6.5 6.5 0 1 0 0-9.188 6.5 6.5 0 0 0 0 9.188zm3.156-4.188L9.375 11.25l-.563-.563a4.485 4.485 0 0 1-6.343 0L3.375 6.156l.563-.563a4.484 4.484 0 0 1 6.344 0l.875.876z"/></svg>
            </span>
            <input
              type="search"
              placeholder="Search Chafgram…"
              className="w-full pl-9 pr-3 py-1.5 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[color-mix(in_oklab,var(--lagoon-deep)58%,var(--line))] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--lagoon)24%,transparent)]"
            />
          </div>
        </div>

        <div className="order-3 flex w-full flex-wrap items-center gap-x-4 gap-y-1 pb-1 text-sm font-semibold sm:order-none sm:w-auto sm:flex-nowrap sm:pb-0">
          {navLinks.map((link) => (
            <Link
              key={link.label}
              to={link.to}
              className="nav-link px-2 py-1.5"
              activeProps={{ className: 'nav-link is-active px-2 py-1.5' }}
            >
              {link.label}
              {link.label === 'Messages' && (
                <span className="ml-1 inline-flex h-5 w-5 items-center justify-center rounded-full border border-white bg-[var(--lagoon)] px-1 text-[0.625rem] font-bold text-white shadow-sm">
                  3
                </span>
              )}
              {link.label === 'Notifications' && (
                <span className="ml-1 inline-flex h-5 w-5 items-center justify-center rounded-full border border-white bg-[var(--palm)] px-1 text-[0.625rem] font-bold text-white shadow-sm">
                  5
                </span>
              )}
            </Link>
          ))}
        </div>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          <ThemeToggle />
          <div className="flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow-[0_8px_24px_rgba(30,90,72,0.08)]">
            <Avatar src="/placeholder.svg" initials="YD" label="You" size={32} />
            <span className="text-[var(--sea-ink-soft)]">You</span>
          </div>
        </div>
      </nav>
    </header>
  )
}

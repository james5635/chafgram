import { Link, useLocation } from '@tanstack/react-router'

const navLinks = [
  { to: '/', label: 'Home', icon: 'home' },
  { to: '/explore', label: 'Explore', icon: 'compass' },
  { to: '/messages', label: 'Messages', icon: 'message', badge: 3 },
  { to: '/notifications', label: 'Notifications', icon: 'bell', badge: 5 },
  { to: '/profile/me', label: 'Profile', icon: 'user' },
  { to: '/more', label: 'More', icon: 'more' },
]

const ICONS: Record<string, string> = {
  home: '<path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6V5m0 0h3m-3 0V5" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>',
  compass: '<path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>',
  message: '<path d="M8 12h.01M12 12h.01M12 12a3 3 0 00-3 3m7.532-2.146a3 3 0 11-4.243-4.243M3 16c0-2.76 2.018-5 4.5-5S12 13.24 12 16" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>',
  bell: '<path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13 7l3-3m0 0l-3 3m3 3l-3 3" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>',
  user: '<path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>',
  more: '<circle cx="12" cy="12" r="1" fill="currentColor"/><circle cx="12" cy="5" r="1" fill="currentColor"/><circle cx="12" cy="19" r="1" fill="currentColor"/>',
}

export default function LeftRail() {
  const location = useLocation()

  return (
    <aside
      className="fixed left-0 top-14 bottom-0 w-64 border-r border-[var(--line)] bg-white/60 backdrop-blur-md z-20 flex flex-col px-3 py-4"
      aria-label="Main navigation"
    >
      <div className="flex items-center gap-2 mb-8">
        <span className="h-8 w-8 rounded-full bg-[var(--brand)] flex items-center justify-center text-white font-bold text-lg shadow-lg shadow-[var(--brand)]/30">
          C
        </span>
        <span className="text-xl font-bold tracking-tight text-[var(--sea-ink)]">Chafgram</span>
      </div>

      <nav className="flex-1 space-y-1">
        {navLinks.map((link) => {
          const isActive = location.pathname === link.to
          return (
            <Link
              key={link.label}
              to={link.to}
              className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                isActive
                  ? 'bg-[var(--brand-light)] text-[var(--brand-dark)] font-semibold'
                  : 'text-[var(--sea-ink-soft)] hover:bg-[var(--brand-light)]/50'
              }`}
            >
              <span
                className="w-5 h-5 shrink-0"
                dangerouslySetInnerHTML={{ __html: ICONS[link.icon] }}
                aria-hidden="true"
              />
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

      <div className="mt-auto pt-6">
        <Link
          to="/compose"
          className="w-full flex items-center justify-center gap-2 rounded-full border border-[var(--brand)] bg-[var(--brand)] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[var(--brand-dark)] hover:shadow-lg hover:shadow-[var(--brand)]/40"
          aria-label="Create post"
        >
          <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true">
            <path fill="currentColor" d="M5 8V3H3v2H0v2h2v5h2V8h2V6H5V8zM12 7a1 1 0 11-2 0 1 1 0 012 0zM7.5 1a5.5 5.5 0 110 11 5.5 5.5 0 010-11z" />
          </svg>
          Create Post
        </Link>
        <p className="mt-8 text-[0.6875rem] text-[var(--sea-ink-soft)] text-center leading-relaxed">
          Connect. Share. Belong.
        </p>
      </div>
    </aside>
  )
}

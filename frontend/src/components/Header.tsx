import { Link } from '@tanstack/react-router'
import { PanelLeft, PanelLeftOpen, Search } from 'lucide-react'
import { useRail } from '../hooks/useRail'

export default function Header() {
  const { collapsed, toggle } = useRail()
  const RailIcon = collapsed ? PanelLeftOpen : PanelLeft

  return (
    <header className="sticky top-0 z-40 border-b border-[var(--line)] bg-[var(--header-bg)] px-4 backdrop-blur-md">
      <div className={`relative flex items-center justify-between gap-4 py-2.5 ${collapsed ? 'pl-16' : 'pl-32 sm:pl-40 lg:pl-64'}`}>
        <button
          type="button"
          aria-label={collapsed ? 'Expand navigation' : 'Collapse navigation'}
          onClick={toggle}
          className="absolute left-2 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-[var(--sea-ink-soft)] transition hover:bg-[var(--brand-light)] hover:text-[var(--sea-ink)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand)]"
        >
          <RailIcon className="h-5 w-5" aria-hidden="true" />
        </button>
        <Link
          to="/"
          className={`absolute top-1/2 flex -translate-y-1/2 items-center gap-2 no-underline ${collapsed ? 'left-10' : 'left-12 lg:left-14'}`}
          aria-label="Chafgram home"
        >
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[var(--brand)] text-base font-bold text-white shadow-sm">C</span>
          <span className={`text-sm font-bold tracking-tight text-[var(--sea-ink)] ${collapsed ? 'hidden' : 'hidden sm:inline'}`}>Chafgram</span>
        </Link>
        <div className="ml-auto flex-1 max-w-2xl">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--sea-ink-soft)]" aria-hidden="true" />
            <input
              type="search"
              placeholder="Search Chafgram…"
              className="w-full pl-9 pr-3 py-1.5 rounded-full border border-[var(--line)] bg-white/80 text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[var(--brand)] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--brand)24%,transparent)]"
            />
          </div>
        </div>
      </div>
    </header>
  )
}

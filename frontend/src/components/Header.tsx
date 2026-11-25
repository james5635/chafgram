import ThemeToggle from './ThemeToggle'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Link } from '@tanstack/react-router'
import { useQuery } from '@apollo/client/react'
import { MeDocument } from '@/generated/graphql'
import { Search, ChevronDown } from 'lucide-react'

export default function Header() {
  const { data } = useQuery(MeDocument)
  const displayName = data?.me.displayName ?? 'You'

  return (
    <header className="sticky top-0 z-40 border-b border-[var(--line)] bg-[var(--header-bg)] px-4 backdrop-blur-md">
      <div className="relative flex items-center justify-between gap-4 py-2.5 pl-32 sm:pl-40 lg:pl-64">
        <Link to="/" className="absolute left-0 top-1/2 flex -translate-y-1/2 items-center gap-2 no-underline lg:left-2" aria-label="Chafgram home">
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[var(--brand)] text-base font-bold text-white shadow-sm">C</span>
          <span className="hidden text-sm font-bold tracking-tight text-[var(--sea-ink)] sm:inline">Chafgram</span>
        </Link>
        <div className="flex-1 max-w-2xl">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--sea-ink-soft)]" aria-hidden="true" />
            <input
              type="search"
              placeholder="Search Chafgram…"
              className="w-full pl-9 pr-3 py-1.5 rounded-full border border-[var(--line)] bg-white/80 text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[var(--brand)] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--brand)24%,transparent)]"
            />
          </div>
        </div>

        <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
          <ThemeToggle />
          <div className="flex items-center gap-2 rounded-full border border-[var(--line)] bg-white/80 px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow-sm">
            <Avatar className="h-8 w-8" aria-label={displayName}>
              <AvatarFallback name={displayName} />
            </Avatar>
            <ChevronDown className="h-4 w-4 text-[var(--sea-ink-soft)]" aria-hidden="true" />
          </div>
        </div>
      </div>
    </header>
  )
}

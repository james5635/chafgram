import ThemeToggle from './ThemeToggle'
import Avatar from './Avatar'

export default function Header() {
  return (
    <header
      className="sticky top-0 z-40 border-b border-[var(--line)] bg-white/60 backdrop-blur-md px-4"
    >
      <div className="page-wrap flex items-center justify-between gap-4 py-3 sm:py-4">
        <div className="flex-1 max-w-2xl">
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--sea-ink-soft)]">
              <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M11.742 10.344a6.5 6.5 0 1 0 0-9.188 6.5 6.5 0 0 0 0 9.188zm3.156-4.188L9.375 11.25l-.563-.563a4.485 4.485 0 0 1-6.343 0L3.375 6.156l.563-.563a4.484 4.484 0 0 1 6.344 0l.875.876z"/></svg>
            </span>
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
            <Avatar src="/placeholder.svg" initials="R" label="You" size={32} />
            <svg viewBox="0 0 16 16" className="h-4 w-4 text-[var(--sea-ink-soft)]" aria-hidden="true">
              <path fill="currentColor" d="M11.83 4.93A.75.75 0 0010.5 4.5h-3a.75.75 0 000 1.5h.17l.443.589a1.259 1.259 0 011.106 1.106l.589-.443a.75.75 0 001.106-1.106l-.443-.589A.75.75 0 0011.057 5h1.172a.75.75 0 00.653-1.071zM7.5 1a5.5 5.5 0 110 11 5.5 5.5 0 010-11z" />
            </svg>
          </div>
        </div>
      </div>
    </header>
  )
}

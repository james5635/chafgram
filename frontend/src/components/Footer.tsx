export default function Footer() {
  const year = new Date().getFullYear()

  return (
    <footer className="mt-16 border-t border-[var(--line)] px-4 pb-14 pt-10 text-[var(--sea-ink-soft)]">
      <div className="page-wrap flex flex-col items-center justify-between gap-4 text-center sm:flex-row sm:text-left">
        <div className="flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]" />
          <span className="text-sm font-semibold text-[var(--sea-ink)]">Chafgram</span>
        </div>
        <p className="m-0 text-sm">
          &copy; {year} Your company. All rights reserved.
        </p>
        <p className="island-kicker m-0 hidden sm:block">Private social for your company</p>
      </div>
    </footer>
  )
}

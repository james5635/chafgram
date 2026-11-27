import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft } from 'lucide-react'
import ThemeToggle from '../components/ThemeToggle'

export const Route = createFileRoute('/settings')({
  component: Settings,
})

export function Settings() {
  return (
    <main className="page-wrap px-4 pb-8 pt-14 max-w-2xl">
      <header className="mb-8 flex items-center gap-3">
        <Link to="/" className="text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]">
          <ArrowLeft className="h-5 w-5 mr-1" aria-hidden="true" />
          Back
        </Link>
        <h1 className="m-0 text-xl font-bold text-[var(--sea-ink)] sm:text-2xl">Settings</h1>
      </header>

      <section className="space-y-8">
        <section className="island-shell rise-in rounded-xl p-6 sm:p-8">
          <p className="island-kicker mb-4">Appearance</p>
          <div className="flex items-center gap-3">
            <ThemeToggle />
          </div>
        </section>

        <section className="island-shell rise-in rounded-xl p-6 sm:p-8">
          <p className="island-kicker mb-4">Notifications</p>
          <div className="space-y-4">
            <label className="flex items-center justify-between px-4 py-3 cursor-pointer group">
              <span>
                <p className="m-0 text-sm text-[var(--sea-ink)]">New followers</p>
                <p className="m-0 mt-0.5 text-[0.75rem] text-[var(--sea-ink-soft)]">Get notified when someone follows you</p>
              </span>
              <input type="checkbox" className="rounded border-[var(--line)] text-[var(--lagoon)] focus:ring-[var(--lagoon)]" defaultChecked />
            </label>
            <label className="flex items-center justify-between px-4 py-3 cursor-pointer group">
              <span>
                <p className="m-0 text-sm text-[var(--sea-ink)]">Comments on your posts</p>
                <p className="m-0 mt-0.5 text-[0.75rem] text-[var(--sea-ink-soft)]">Get notified when someone comments</p>
              </span>
              <input type="checkbox" className="rounded border-[var(--line)] text-[var(--lagoon)] focus:ring-[var(--lagoon)]" defaultChecked />
            </label>
            <label className="flex items-center justify-between px-4 py-3 cursor-pointer group">
              <span>
                <p className="m-0 text-sm text-[var(--sea-ink)]">Direct messages</p>
                <p className="m-0 mt-0.5 text-[0.75rem] text-[var(--sea-ink-soft)]">Get notified when you receive a message</p>
              </span>
              <input type="checkbox" className="rounded border-[var(--line)] text-[var(--lagoon)] focus:ring-[var(--lagoon)]" defaultChecked />
            </label>
          </div>
        </section>
      </section>
    </main>
  )
}

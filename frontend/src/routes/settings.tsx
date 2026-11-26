import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import ThemeToggle from '../components/ThemeToggle'

export const Route = createFileRoute('/settings')({
  component: Settings,
})

function Settings() {
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
          <p className="island-kicker mb-4">Passkeys</p>
          <p className="mb-4 max-w-md text-sm text-[var(--sea-ink-soft)]">
            Sign in with passkeys. You can register several and name each one.
          </p>
          <div className="space-y-3">
            <Button variant="outline" className="w-full justify-between rounded-full">
              <span>Add a passkey</span>
              <span className="text-[var(--sea-ink-soft)]">+</span>
            </Button>
            <div className="flex items-center justify-between px-4 py-3">
              <div>
                <p className="m-0 text-sm text-[var(--sea-ink)]">Work laptop</p>
                <p className="m-0 text-[0.75rem] text-[var(--sea-ink-soft)]">Registered 2 days ago</p>
              </div>
              <Button variant="ghost" size="icon" aria-label="Remove passkey">
                <Trash2 className="h-4 w-4 text-[var(--sea-ink-soft)] hover:text-[#9f3030]" aria-hidden="true" />
              </Button>
            </div>
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

import { createFileRoute, Link } from '@tanstack/react-router'
import Avatar from '../components/Avatar'

export const Route = createFileRoute('/settings')({
  component: Settings,
})

function Settings() {
  return (
    <main className="page-wrap px-4 pb-8 pt-14 max-w-2xl">
      <header className="mb-8 flex items-center gap-3">
        <Link to="/" className="text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]">
          <svg viewBox="0 0 16 16" className="h-5 w-5" aria-hidden="true"><path fill="currentColor" d="M11.25 4.75l-4 4 4 4 1.06-1.06L7.81 8.5l4.5-4.5-1.06-1.06z"/></svg>
          Back
        </Link>
        <h1 className="m-0 text-xl font-bold text-[var(--sea-ink)] sm:text-2xl">Settings</h1>
      </header>

      <section className="space-y-8">
        <section className="island-shell rise-in rounded-2xl p-6 sm:p-8">
          <p className="island-kicker mb-4">Appearance</p>
          <div className="flex items-center gap-3">
            <ThemeTogglePreview />
          </div>
        </section>

        <section className="island-shell rise-in rounded-2xl p-6 sm:p-8">
          <p className="island-kicker mb-4">Passkeys</p>
          <p className="mb-4 max-w-md text-sm text-[var(--sea-ink-soft)]">
            Sign in with passkeys. You can register several and name each one.
          </p>
          <div className="space-y-3">
            <button type="button" className="w-full flex items-center justify-between rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-4 py-3 text-sm text-left text-[var(--sea-ink)] transition hover:bg-[var(--link-bg-hover)]">
              <span>Add a passkey</span>
              <span className="text-[var(--sea-ink-soft)]">+</span>
            </button>
            <div className="flex items-center justify-between px-4 py-3">
              <div>
                <p className="m-0 text-sm text-[var(--sea-ink)]">Work laptop</p>
                <p className="m-0 text-[0.75rem] text-[var(--sea-ink-soft)]">Registered 2 days ago</p>
              </div>
              <button type="button" className="text-[var(--sea-ink-soft)] transition hover:text-[#9f3030]" aria-label="Remove passkey">
                <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M5.5 8h5V7H5.5z"/></svg>
              </button>
            </div>
          </div>
        </section>

        <section className="island-shell rise-in rounded-2xl p-6 sm:p-8">
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

function ThemeTogglePreview() {
  return (
    <div className="flex items-center gap-2">
      <span className="text-sm text-[var(--sea-ink-soft)]">Theme mode</span>
      <button type="button" className="rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink)] shadow-[0_8px_22px_rgba(30,90,72,0.08)] transition hover:-translate-y-0.5">
        Auto
      </button>
    </div>
  )
}

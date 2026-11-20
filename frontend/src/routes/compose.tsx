import { createFileRoute, Link } from '@tanstack/react-router'
import { useState } from 'react'
import Avatar from '../components/Avatar'
import AppShell from '../components/AppShell'

export const Route = createFileRoute('/compose')({
  component: Compose,
})

const CHIPS = [
  { label: 'Photo', color: '#2563eb', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9.5" r="1.5"/><path d="M21 16l-5-5-8 8"/></svg>' },
  { label: 'Video', color: '#2563eb', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="14" height="16" rx="2"/><path d="M17 9l4-3v12l-4-3z"/></svg>' },
  { label: 'Feeling', color: '#f59e0b', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><circle cx="9" cy="9" r="0.8" fill="currentColor"/><circle cx="15" cy="9" r="0.8" fill="currentColor"/></svg>' },
  { label: 'Poll', color: '#4f46e5', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M5 8v8M10 4v12M15 10v6M20 6v10"/></svg>' },
] as const

export function Compose() {
  const [body, setBody] = useState('')

  return (
    <AppShell>
      <div className="page-wrap px-4 pb-8 pt-6 max-w-3xl">
        <header className="mb-5 flex items-center gap-3">
          <Link to="/" className="text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]" aria-label="Back to home">
            <svg viewBox="0 0 16 16" className="h-5 w-5" aria-hidden="true"><path fill="currentColor" d="M11.25 4.75l-4 4 4 4 1.06-1.06L7.81 8.5l4.5-4.5-1.06-1.06z"/></svg>
          </Link>
          <h1 className="m-0 text-xl font-bold text-[var(--sea-ink)] sm:text-2xl">Create Post</h1>
        </header>

        <section className="island-shell rise-in overflow-hidden rounded-2xl">
          <div className="grid gap-0 md:grid-cols-2">
            <div className="p-5 sm:p-6">
              <div className="flex gap-3">
                <Avatar initials="RJ" label="Rojame" size={40} />
                <textarea
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                  placeholder="What's on your mind?"
                  rows={4}
                  className="flex-1 min-w-0 bg-transparent pt-1 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline outline-none resize-none focus:ring-0 leading-6"
                />
              </div>

              <div className="mt-6 flex flex-wrap items-center gap-4 border-t border-[var(--line)] pt-4">
                {CHIPS.map((chip) => (
                  <button key={chip.label} type="button" className="flex items-center gap-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]">
                    <span className="h-4 w-4 [&>svg]:h-full [&>svg]:w-full" style={{ color: chip.color }} dangerouslySetInnerHTML={{ __html: chip.icon }} aria-hidden="true" />
                    {chip.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="border-t border-[var(--line)] p-5 sm:p-6 md:border-l md:border-t-0">
              <button
                type="button"
                className="flex h-full min-h-40 w-full flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed border-[var(--line)] bg-[var(--brand-light)]/40 p-6 text-center transition hover:border-[var(--brand)] hover:bg-[var(--brand-light)]/70"
              >
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-[var(--brand-dark)] shadow-sm">
                  <svg viewBox="0 0 24 24" className="h-6 w-6" aria-hidden="true"><path fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M4 16l4.6-4.6a2 2 0 012.8 0L16 16m-2-2l1.6-1.6a2 2 0 012.8 0L20 14M4 20h16a2 2 0 002-2V6a2 2 0 00-2-2H4a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
                </span>
                <span className="text-sm font-semibold text-[var(--sea-ink)]">Upload Image</span>
                <span className="text-xs text-[var(--sea-ink-soft)]">or drag and drop</span>
              </button>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-[var(--line)] px-5 py-4 sm:px-6">
            <Link
              to="/"
              className="rounded-full border border-[var(--line)] bg-white px-5 py-2 text-sm font-semibold text-[var(--sea-ink-soft)] no-underline transition hover:bg-[var(--brand-light)]"
            >
              Cancel
            </Link>
            <button
              type="button"
              className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-6 py-2 text-sm font-bold !text-white shadow-lg shadow-[var(--brand)]/30 transition hover:bg-[var(--brand-dark)]"
            >
              Post
            </button>
          </div>
        </section>
      </div>
    </AppShell>
  )
}

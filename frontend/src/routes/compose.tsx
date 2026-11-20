import { createFileRoute, Link } from '@tanstack/react-router'
import { useState } from 'react'
import Avatar from '../components/Avatar'

export const Route = createFileRoute('/compose')({
  component: Compose,
})

const CHIPS = ['Photo', 'Video', 'Feeling', 'Poll'] as const

function Compose() {
  const [body, setBody] = useState('')
  const [selectedChip, setSelectedChip] = useState<string | null>(null)

  return (
    <main className="page-wrap px-4 pb-8 pt-14 max-w-2xl">
      <header className="mb-6 flex items-center gap-3">
        <Link to="/" className="text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]">
          <svg viewBox="0 0 16 16" className="h-5 w-5" aria-hidden="true"><path fill="currentColor" d="M11.25 4.75l-4 4 4 4 1.06-1.06L7.81 8.5l4.5-4.5-1.06-1.06z"/></svg>
          Back
        </Link>
        <h1 className="m-0 text-xl font-bold text-[var(--sea-ink)] sm:text-2xl">Create post</h1>
      </header>

      <section className="island-shell rise-in rounded-2xl p-6 sm:p-8">
        <div className="flex gap-3 mb-6">
          <div className="flex-shrink-0">
            <Avatar src="/placeholder.svg" initials="YD" label="You" size={40} />
          </div>
          <div className="flex-1 min-w-0">
            <textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="What's on your mind?"
              className="w-full demo-textarea mt-1 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline focus:ring-0"
            />
            <div className="flex flex-wrap gap-2 mt-3">
              {CHIPS.map((chip) => (
                <button
                  key={chip}
                  type="button"
                  onClick={() => setSelectedChip(selectedChip === chip ? null : chip)}
                  className={`rounded-full border px-3 py-1.5 text-xs font-semibold transition ${selectedChip === chip ? 'border-[var(--chip-line)] bg-[var(--chip-bg)] text-[var(--sea-ink)]' : 'border-transparent bg-transparent text-[var(--sea-ink-soft)] hover:bg-[var(--link-bg-hover)]'}`}
                >
                  {chip}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="mb-6 flex items-start gap-3">
          <div className="flex-shrink-0 text-[var(--sea-ink-soft)] text-sm mt-0.5">Media</div>
          <div className="flex-1">
            <button type="button" className="flex items-center gap-2 rounded-full border border-[var(--line)] bg-transparent px-4 py-2 text-sm font-semibold text-[var(--sea-ink)] transition hover:bg-[var(--link-bg-hover)]">
              <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M11.8 12c0-.7-.6-1.3-1.3-1.3h-.8c-.1 0-.3-.1-.4-.2l-2.3-2.3c-.4-.4-1-.4-1.4 0s-.4 1 0 1.4l.6.6c.2.2.4.3.7.3h.1c.3 0 .6-.1.8-.3l2.3-2.3c.4-.4.4-1 0-1.4v-.6c0-.7-.6-1.3-1.3-1.3h-3c-.7 0-1.3.6-1.3 1.3v3c0 .7.6 1.3 1.3 1.3h3c.7 0 1.3-.6 1.3-1.3v-.8zM6.5 6c-.3 0-.5-.2-.5-.5s.2-.5.5-.5.5.2.5.5-.2.5-.5.5zm7.5-4.5H2.5C1.1 1.5 0 2.6 0 4v8c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.4-.9-2.5-2-2.5z"/></svg>
              Attach photo or video
            </button>
          </div>
        </div>

        <p className="mb-6 text-[0.75rem] text-[var(--sea-ink-soft)]">
          Posts can include text, photos, or a single video. Mixed photo + video posts aren't supported.
        </p>

        <div className="flex justify-end">
          <button type="button" className="px-6 py-2.5 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)] rounded-full border border-transparent">
            Cancel
          </button>
          <button
            type="button"
            disabled={body.trim().length === 0}
            className="ml-3 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] px-6 py-2.5 text-sm font-semibold text-[var(--sea-ink)] transition disabled:opacity-55 disabled:transform-none hover:!bg-[color-mix(in_oklab,var(--lagoon)30%,var(--surface-strong))]"
          >
            Post
          </button>
        </div>
      </section>
    </main>
  )
}

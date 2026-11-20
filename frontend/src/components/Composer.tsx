import { useState } from 'react'
import Avatar from './Avatar'

const CHIPS = ['Photo', 'Video', 'Feeling', 'Poll'] as const

export default function Composer({ member }: { member: { name: string; username: string; avatar?: string; initials?: string } }) {
  const [body, setBody] = useState('')
  const [selectedChip, setSelectedChip] = useState<string | null>(null)

  const canPost = body.trim().length > 0

  return (
    <section className="island-shell rise-in relative overflow-hidden rounded-[2rem] p-6 sm:p-8">
      <div className="pointer-events-none absolute -left-20 -top-24 h-56 w-56 rounded-full bg-[radial-gradient(circle,rgba(79,184,178,0.32),transparent_66%)]" />
      <div className="pointer-events-none absolute -bottom-20 -right-20 h-56 w-56 rounded-full bg-[radial-gradient(circle,rgba(47,106,74,0.18),transparent_66%)]" />

      <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <div className="flex-shrink-0">
          <Avatar src={member.avatar} initials={member.initials} label={member.name} size={32} />
        </div>

        <div className="flex-1 min-w-0">
          <p className="island-kicker mb-1">Create post</p>
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="What's on your mind?"
            className="demo-textarea mt-1 mb-3 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline focus:ring-0"
          />
          <div className="flex flex-wrap gap-2 sm:gap-3">
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

        <button
          type="button"
          disabled={!canPost}
          className="flex-shrink-0 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] px-5 py-2.5 text-sm font-semibold text-[var(--sea-ink)] no-underline transition hover:bg-[color-mix(in_oklab,var(--lagoon)30%,var(--surface-strong))] disabled:opacity-55 disabled:transform-none"
        >
          Post
        </button>
      </div>

      {selectedChip && (
        <p className="mt-2 text-[var(--sea-ink-soft)] text-xs">Adding {selectedChip.toLowerCase()}…</p>
      )}
    </section>
  )
}

import { useState } from 'react'
import Avatar from './Avatar'

const CHIPS = [
  { label: 'Photo', color: '#2563eb', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9.5" r="1.5"/><path d="M21 16l-5-5-8 8"/></svg>' },
  { label: 'Video', color: '#2563eb', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="14" height="16" rx="2"/><path d="M17 9l4-3v12l-4-3z"/></svg>' },
  { label: 'Feeling', color: '#f59e0b', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><circle cx="9" cy="9" r="0.8" fill="currentColor"/><circle cx="15" cy="15" r="0.8" fill="currentColor"/></svg>' },
  { label: 'Poll', color: '#4f46e5', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M5 8v8M10 4v12M15 10v6M20 6v10"/></svg>' },
] as const

interface ComposerProps {
  member: { name: string; username: string; avatar?: string; initials?: string }
  /**
   * Publishes the composed text. Returning `true` clears the editor; the
   * composer keeps the text when it returns `false` (e.g. the backend
   * rejected the post), so nothing is lost on a failed publish.
   */
  onSubmit?: (body: string) => Promise<boolean> | boolean
}

export default function Composer({ member, onSubmit }: ComposerProps) {
  const [body, setBody] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit() {
    const text = body.trim()
    if (!text || !onSubmit || submitting) return

    setSubmitting(true)
    try {
      const published = await onSubmit(text)
      if (published) setBody('')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="island-shell rise-in rounded-[1.5rem] overflow-hidden border border-[var(--line)] bg-white shadow-sm">
      <div className="flex items-center gap-3 px-5 py-4 sm:px-6 sm:py-5">
        <Avatar src={member.avatar} initials={member.initials} label={member.name} size={32} />
        <textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="What's on your mind?"
          rows={1}
          aria-label="What's on your mind?"
          className="flex-1 min-w-0 bg-transparent text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline outline-none resize-none focus:ring-0 leading-6"
        />
      </div>

      <div className="border-t border-[var(--line)] px-5 py-3 sm:px-6 flex items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-1 sm:gap-2">
          {CHIPS.map((chip) => (
            <button
              key={chip.label}
              type="button"
              className="flex items-center gap-1.5 rounded-full px-2 py-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[#f3f4f6]"
            >
              <span className="h-4 w-4 shrink-0 [&>svg]:h-full [&>svg]:w-full" style={{ color: chip.color }} dangerouslySetInnerHTML={{ __html: chip.icon }} aria-hidden="true" />
              {chip.label}
            </button>
          ))}
        </div>

        <button
          type="button"
          onClick={() => void handleSubmit()}
          disabled={!body.trim() || submitting}
          className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-5 py-2 text-sm font-bold !text-white no-underline transition hover:bg-[var(--brand-dark)] shadow-lg shadow-[var(--brand)]/30 disabled:opacity-50 disabled:shadow-none"
        >
          {submitting ? 'Posting…' : 'Post'}
        </button>
      </div>
    </section>
  )
}

import { useState } from 'react'
import Avatar from './Avatar'

const CHIPS = [
  { label: 'Photo', icon: '<path d="M4 16l4.586-4.586a2 2 0 112.828 2.828L4 16zm5.586-2.828l2.829-2.829a1 1 0 10-1.414-1.414L6.586 11.586a1 1 0 000 1.414zm8.414-8.414L16.586 16m-4-4l3.414-3.414a1 1 0 011.414 1.414L18 13.414a1 1 0 01-1.414 1.414L16.586 15" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>', color: '#2563eb' },
  { label: 'Video', icon: '<path d="M14.5 2l-11 .75a2 2 0 00-1.956 1.798L2 18a2 2 0 001.956 1.798L14.5 22a2 2 0 001.956-1.798L22 18a2 2 0 00-1.956-1.798zM12 10l2 2 2-2m-7 0l2 2 2-2" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>', color: '#3b82f6' },
  { label: 'Feeling', icon: '<path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A7 7 0 003 11c0 1.61.557 3.084 1.439 4.246A11.977 11.977 0 0012 21.056c3.483 0 6.65-1.523 8.862-4.016A7 7 0 0021 11c0-3.859-2.633-7.143-6.182-8.326z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>', color: '#f59e0b' },
  { label: 'Poll', icon: '<path d="M5 3v4M5 19v4M7 3h4M7 19h4m-2-4L5 3m14 16v-4m0 0h-4m4 4h4M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"/>', color: '#6366f1' },
] as const

export default function Composer({ member }: { member: { name: string; username: string; avatar?: string; initials?: string } }) {
  const [body, setBody] = useState('')

  const canPost = body.trim().length > 0

  return (
    <section className="island-shell rise-in relative overflow-hidden rounded-[2rem] p-6 sm:p-8">
      <div className="pointer-events-none absolute -left-20 -top-24 h-56 w-56 rounded-full bg-[radial-gradient(circle,rgba(79,184,178,0.32),transparent_66%)]" />
      <div className="pointer-events-none absolute -bottom-20 -right-20 h-56 w-56 rounded-full bg-[radial-gradient(circle,rgba(47,106,74,0.18),transparent_66%)]" />

      <div className="flex gap-4 sm:flex-row sm:items-start">
        <div className="flex-shrink-0">
          <Avatar src={member.avatar} initials={member.initials} label={member.name} size={32} />
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-[0.875rem] font-medium text-[var(--sea-ink)] leading-none">What's on your mind?</p>
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="What's on your mind?"
            className="w-full mt-2 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline focus:ring-0 resize-none"
            rows={3}
          />
          <div className="flex flex-wrap gap-2 mt-3">
            {CHIPS.map((chip) => (
              <button
                key={chip.label}
                type="button"
                className="flex items-center gap-1.5 rounded-full border border-[var(--line)] bg-white px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink)] transition hover:bg-[#f3f4f6]"
              >
                <span className="h-3 w-3 shrink-0" dangerouslySetInnerHTML={{ __html: chip.icon }} aria-hidden="true" />
                {chip.label}
              </button>
            ))}
          </div>
        </div>

        <button
          type="button"
          disabled={!canPost}
          className={`flex-shrink-0 rounded-full border px-5 py-2.5 text-sm font-bold text-white no-underline transition shadow-lg shadow-[var(--brand)]/30 disabled:opacity-60 disabled:cursor-not-allowed disabled:shadow-none ${
            canPost
              ? 'bg-[var(--brand)] hover:bg-[var(--brand-dark)]'
              : 'bg-[color-mix(in_oklab,var(--brand)20%,white_80%)] border-transparent text-[var(--brand-dark)]'
          }`}
        >
          Post
        </button>
      </div>
    </section>
  )
}

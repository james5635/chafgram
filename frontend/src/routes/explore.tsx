import { createFileRoute } from '@tanstack/react-router'
import { gradientPlaceholder } from '../lib/placeholders'
import AppShell from '../components/AppShell'

export const Route = createFileRoute('/explore')({
  component: Explore,
})

const TABS = ['For You', 'Following', 'Trending', 'Nearby'] as const

const CARDS = [
  { title: 'Better Together', likes: '12.5K', img: gradientPlaceholder({ colors: ['#4f7fc4', '#9fc6ef'] }) },
  { title: 'Good Morning ☀️', likes: '24.1K', img: gradientPlaceholder({ colors: ['#7ec8e3', '#cdeaf5'] }) },
  { title: 'City Architecture', likes: '11.3K', img: gradientPlaceholder({ colors: ['#4a6f8c', '#9db8c9'] }) },
  { title: 'Coffee Break', likes: '15.6K', img: gradientPlaceholder({ colors: ['#9c7b64', '#d9bda6'] }) },
  { title: 'Ocean View', likes: '8.9K', img: gradientPlaceholder({ colors: ['#3d9bc4', '#9fd8ea'] }) },
  { title: 'Morning Light', likes: '7.2K', img: gradientPlaceholder({ colors: ['#e0a45c', '#f6dcae'] }) },
]

const TOPICS = [
  { name: 'Technology', posts: '234K posts', icon: 'M12 2a2 2 0 012 2v2a2 2 0 01-2 2 2 2 0 00-2 2v8a2 2 0 002 2h8a2 2 0 002-2v-3a2 2 0 00-2-2h-2' },
  { name: 'Travel', posts: '156K posts', icon: 'M2 12l10-7 10 7-10 7-10-7z' },
  { name: 'Lifestyle', posts: '108K posts', icon: 'M12 21s-7-4.6-9-9a5 5 0 019-3 5 5 0 019 3c-2 4.4-9 9-9 9z' },
  { name: 'Education', posts: '142K posts', icon: 'M3 6l9-3 9 3-9 3-9-3z' },
  { name: 'Gaming', posts: '128K posts', icon: 'M6 12h4m-2-2v4m7-6h.01M17 12h.01M8 3h8a5 5 0 015 5v8a5 5 0 01-5 5H8a5 5 0 01-5-5V8a5 5 0 015-5z' },
]

function SuggestedTopics() {
  return (
    <aside className="hidden w-72 flex-shrink-0 py-6 pr-4 xl:block" aria-label="Suggested topics">
      <section className="island-shell rounded-2xl p-5">
        <h3 className="m-0 mb-4 text-base font-bold text-[var(--sea-ink)]">Suggested Topics</h3>
        <div className="space-y-1">
          {TOPICS.map((topic) => (
            <div key={topic.name} className="flex items-center gap-3 rounded-xl px-2 py-2 transition hover:bg-[var(--brand-light)]/60">
              <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-[var(--brand-light)] text-[var(--brand-dark)]">
                <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
                  <path d={topic.icon} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
              <span className="flex-1 min-w-0">
                <span className="block text-sm font-semibold text-[var(--sea-ink)]">{topic.name}</span>
                <span className="block text-[0.7rem] text-[var(--sea-ink-soft)]">{topic.posts}</span>
              </span>
              <button type="button" className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full border border-[var(--line)] text-[var(--brand-dark)] transition hover:bg-[var(--brand)] hover:text-white" aria-label={`Follow ${topic.name}`}>
                +
              </button>
            </div>
          ))}
        </div>
      </section>
    </aside>
  )
}

export function Explore() {
  return (
    <AppShell rightRail={<SuggestedTopics />}>
      <div className="page-wrap px-4 pb-8 pt-6">
        <div className="flex items-center gap-4 mb-5">
          <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Explore</h1>
          <div className="ml-auto">
            <button type="button" className="rounded-full border border-[var(--line)] bg-white/80 px-2 py-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Search">
              <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M11.742 10.344a6.5 6.5 0 1 0 0-9.188 6.5 6.5 0 0 0 0 9.188zm3.156-4.188L9.375 11.25l-.563-.563a4.485 4.485 0 0 1-6.343 0L3.375 6.156l.563-.563a4.484 4.484 0 0 1 6.344 0l.875.876z"/></svg>
            </button>
          </div>
        </div>

        <div className="flex flex-wrap gap-2 mb-6">
          {TABS.map((tab, i) => (
            <button
              key={tab}
              type="button"
              className={`rounded-full border px-4 py-1.5 text-sm font-semibold transition ${
                i === 0
                  ? 'border-[var(--brand)] bg-[var(--brand)] !text-white shadow-lg shadow-[var(--brand)]/30'
                  : 'border-[var(--line)] bg-white/80 text-[var(--sea-ink-soft)] transition hover:bg-[var(--brand-light)]'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {CARDS.map((card, i) => (
            <article key={i} className="island-shell feature-card rise-in overflow-hidden rounded-xl" style={{ animationDelay: `${i * 90}ms` }}>
              <div className="relative h-48 overflow-hidden">
                <img src={card.img} alt={card.title} className="h-full w-full object-cover" loading="lazy" decoding="async" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent" />
                <div className="absolute bottom-0 left-0 right-0 p-4">
                  <h3 className="m-0 text-white font-semibold text-lg shadow-sm">{card.title}</h3>
                </div>
              </div>
              <div className="p-3 flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="text-[var(--lagoon-deep)]">♥</span>
                  <span className="text-sm font-semibold text-[var(--sea-ink)]">{card.likes}</span>
                </div>
              </div>
            </article>
          ))}
        </section>
      </div>
    </AppShell>
  )
}

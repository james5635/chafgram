import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/explore')({
  component: Explore,
})

const TABS = ['For You', 'Following', 'Trending', 'Nearby'] as const

const CARDS = [
  { title: 'Better Together', likes: '12.5K', img: 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=600&q=80' },
  { title: 'Good Morning ☀️', likes: '24.1K', img: 'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=600&q=80' },
  { title: 'City Architecture', likes: '11.3K', img: 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=600&q=80' },
  { title: 'Coffee Break', likes: '15.6K', img: 'https://images.unsplash.com/photo-1497944071364-3ef9c02c4ba8?w=600&q=80' },
  { title: 'Ocean View', likes: '8.9K', img: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80' },
  { title: 'Morning Light', likes: '7.2K', img: 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&q=80' },
]

function Explore() {
  return (
    <main className="page-wrap px-4 pb-8 pt-14">
      <div className="flex items-center gap-4 mb-6">
        <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Explore</h1>
        <div className="ml-auto">
          <button type="button" className="rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-2 py-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Search">
            <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M11.742 10.344a6.5 6.5 0 1 0 0-9.188 6.5 6.5 0 0 0 0 9.188zm3.156-4.188L9.375 11.25l-.563-.563a4.485 4.485 0 0 1-6.343 0L3.375 6.156l.563-.563a4.484 4.484 0 0 1 6.344 0l.875.876z"/></svg>
          </button>
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-2 mb-6 no-scrollbar">
        {TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            className="rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-4 py-1.5 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)] whitespace-nowrap"
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
    </main>
  )
}

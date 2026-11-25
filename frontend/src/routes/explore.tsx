import { useQuery } from '@apollo/client/react'
import { createFileRoute } from '@tanstack/react-router'
import { Cpu, Plane, Heart, BookOpen, Gamepad2, Search, Heart as HeartIcon, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import AppShell from '@/components/AppShell'
import { DiscoverFeedDocument } from '@/generated/graphql'
import { gradientPlaceholder } from '@/lib/placeholders'

export const Route = createFileRoute('/explore')({
  component: Explore,
})

const TABS = ['For You', 'Following', 'Trending', 'Nearby'] as const

const TOPICS = [
  { name: 'Technology', posts: '234K posts', icon: Cpu },
  { name: 'Travel', posts: '156K posts', icon: Plane },
  { name: 'Lifestyle', posts: '108K posts', icon: Heart },
  { name: 'Education', posts: '142K posts', icon: BookOpen },
  { name: 'Gaming', posts: '128K posts', icon: Gamepad2 },
]

const EXPLORE_PLACEHOLDERS = [
  gradientPlaceholder({ colors: ['#93c5fd', '#2563eb'] }),
  gradientPlaceholder({ colors: ['#dbeafe', '#64748b'] }),
  gradientPlaceholder({ colors: ['#bfdbfe', '#1d4ed8'] }),
  gradientPlaceholder({ colors: ['#60a5fa', '#dbeafe'] }),
  gradientPlaceholder({ colors: ['#cbd5e1', '#475569'] }),
  gradientPlaceholder({ colors: ['#bfdbfe', '#3b82f6'] }),
]

function SuggestedTopics() {
  return (
    <aside className="hidden w-72 flex-shrink-0 py-6 pr-4 xl:block" aria-label="Suggested topics">
      <section className="island-shell rounded-xl p-5">
        <h3 className="m-0 mb-4 text-base font-bold text-[var(--sea-ink)]">Suggested Topics</h3>
        <div className="space-y-1">
          {TOPICS.map((topic) => {
            const Icon = topic.icon
            return (
              <div key={topic.name} className="flex items-center gap-3 rounded-xl px-2 py-2 transition hover:bg-[var(--brand-light)]/60">
                <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-[var(--brand-light)] text-[var(--brand-dark)]">
                  <Icon className="h-4 w-4" aria-hidden="true" />
                </span>
                <span className="flex-1 min-w-0">
                  <span className="block text-sm font-semibold text-[var(--sea-ink)]">{topic.name}</span>
                  <span className="block text-[0.7rem] text-[var(--sea-ink-soft)]">{topic.posts}</span>
                </span>
                <Button variant="ghost" size="icon" aria-label={`Follow ${topic.name}`} className="h-6 w-6 rounded-full border border-[var(--line)] text-[var(--brand-dark)] transition hover:bg-[var(--brand)] hover:text-white">
                  <Plus className="h-3.5 w-3.5" aria-hidden="true" />
                </Button>
              </div>
            )
          })}
        </div>
      </section>
    </aside>
  )
}

export function Explore() {
  const { data, loading, error } = useQuery(DiscoverFeedDocument, { variables: { first: 18 } })
  const posts = data?.discoverFeed?.posts ?? []

  return (
    <AppShell rightRail={<SuggestedTopics />}>
      <div className="page-wrap px-4 pb-8 pt-6">
        <div className="flex items-center gap-4 mb-5">
          <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Explore</h1>
          <div className="ml-auto">
            <Button variant="ghost" size="icon" aria-label="Search" className="rounded-full border border-[var(--line)] bg-white/80">
              <Search className="h-4 w-4" aria-hidden="true" />
            </Button>
          </div>
        </div>

        <div className="flex flex-wrap gap-2 mb-6">
          {TABS.map((tab, i) => (
            <Button
              key={tab}
              type="button"
              className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
                i === 0
                  ? 'border-[var(--brand)] bg-[var(--brand)] !text-white shadow-lg shadow-[var(--brand)]/30'
                  : 'border-[var(--line)] bg-white/80 text-[var(--sea-ink-soft)] transition hover:bg-[var(--brand-light)]'
              }`}
            >
              {tab}
            </Button>
          ))}
        </div>

        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-busy={loading}>
          {error && <p className="col-span-full text-sm text-[#9f3030]">Could not load the feed: {error.message}</p>}
          {loading && !data && [0, 1, 2].map((n) => <div key={n} className="h-48 animate-pulse rounded-xl" />)}
          {!loading && !error && posts.length === 0 && (
            <p className="col-span-full text-center text-sm text-[var(--sea-ink-soft)]">Nothing to explore yet.</p>
          )}
          {posts.map((post, i) => (
            <article key={post.id} className="island-shell feature-card rise-in overflow-hidden rounded-xl" style={{ animationDelay: `${i * 90}ms` }}>
              <div className="relative h-48 overflow-hidden">
                <img
                  src={EXPLORE_PLACEHOLDERS[i % EXPLORE_PLACEHOLDERS.length]}
                  alt={post.body ?? post.author.displayName}
                  className="h-full w-full object-cover"
                  loading="lazy"
                  decoding="async"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent" />
                <div className="absolute bottom-0 left-0 right-0 p-3">
                  <h3 className="m-0 line-clamp-2 text-white font-semibold text-base leading-5 shadow-sm">{post.body ?? post.author.displayName}</h3>
                  <div className="mt-1.5 flex items-center gap-1 text-xs font-semibold text-white">
                    <HeartIcon className="h-3.5 w-3.5 fill-white" aria-hidden="true" />
                    <span>{post.reactionCount}</span>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </section>
      </div>
    </AppShell>
  )
}

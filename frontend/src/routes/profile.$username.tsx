import { createFileRoute } from '@tanstack/react-router'
import PostCard from '../components/PostCard'
import Avatar from '../components/Avatar'
import { gradientPlaceholder } from '../lib/placeholders'

export const Route = createFileRoute('/profile/$username')({
  component: Profile,
})

const POSTS = [
  {
    id: 'p1',
    author: { name: 'Rojame', username: 'rojame', avatar: undefined, initials: 'RJ' },
    createdAt: '1d ago',
    body: 'Keep going! 💙',
    media: [{ url: gradientPlaceholder({ colors: ['#5b8dd6', '#9ec8f0'] }), alt: 'Mountain landscape' }],
    reactions: { count: 45 },
    comments: 8,
    shares: 3,
    edited: false,
  },
  {
    id: 'p2',
    author: { name: 'Rojame', username: 'rojame', avatar: undefined, initials: 'RJ' },
    createdAt: '3d ago',
    body: 'Always exploring new things.',
    media: [],
    reactions: { count: 32 },
    comments: 5,
    shares: 2,
    edited: true,
  },
]

function Profile() {
  return (
    <main className="page-wrap px-4 pb-8 pt-14">
      <section className="island-shell rise-in rounded-2xl p-6 sm:p-8 mb-8">
        <div className="flex flex-col sm:flex-row sm:items-start gap-6">
          <div className="flex-shrink-0">
            <Avatar initials="RJ" label="Rojame" size={56} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-baseline gap-3 sm:gap-4">
              <h1 className="m-0 text-3xl font-bold text-[var(--sea-ink)] sm:text-4xl leading-[1.02]">
                Rojame
              </h1>
              <span className="ml-1 text-sm font-medium text-[var(--sea-ink-soft)]">@rojame</span>
            </div>
            <p className="m-0 mt-2 max-w-md text-base text-[var(--sea-ink-soft)] leading-7">
              Software Engineer &amp; Tech Enthusiast 🚀 Always exploring new things.
            </p>
            <div className="mt-5 flex items-center gap-4 sm:gap-6 text-sm">
              <span className="flex items-center gap-1">
                <span className="font-semibold text-[var(--sea-ink)]">128</span>
                <span className="text-[var(--sea-ink-soft)]">Posts</span>
              </span>
              <span className="flex items-center gap-1">
                <span className="font-semibold text-[var(--sea-ink)]">2.4K</span>
                <span className="text-[var(--sea-ink-soft)]">Followers</span>
              </span>
              <span className="flex items-center gap-1">
                <span className="font-semibold text-[var(--sea-ink)]">320</span>
                <span className="text-[var(--sea-ink-soft)]">Following</span>
              </span>
            </div>
            <div className="mt-5 flex gap-2">
              <button type="button" className="flex-1 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] px-4 py-2 text-sm font-semibold text-[var(--sea-ink)] transition hover:bg-[color-mix(in_oklab,var(--lagoon)30%,var(--surface-strong))]">
                Edit Profile
              </button>
              <button type="button" className="rounded-full border border-[var(--line)] bg-transparent px-4 py-2 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]">
                <span className="sr-only">Share profile</span>
                <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M13.5 4.75a.75.75 0 0 0-1.499.107l-.5 13a.75.75 0 0 0 1.498.106l.5-13a.75.75 0 0 0-.106-1.499zm-1.5.25L7.5 15.5l-.5-13L7.5 0zm4 0-.5 13.5-.5-13.5z"/></svg>
              </button>
            </div>
          </div>
        </div>

        <div className="mt-8 flex gap-2 border-b border-[var(--line)] pb-4 overflow-x-auto no-scrollbar sm:overflow-visible sm:flex-nowrap">
          {['Posts', 'Replies', 'Media', 'Likes'].map((tab) => (
            <button
              key={tab}
              type="button"
              className="rounded-full border border-transparent px-4 py-1.5 text-sm font-semibold text-[var(--sea-ink)] border-b-2 border-[var(--lagoon)] transition hover:text-[var(--lagoon-deep)] sm:border-b-0 sm:px-3"
            >
              {tab}
            </button>
          ))}
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {POSTS.map((post) => (
          <PostCard key={post.id} post={post} />
        ))}
      </section>
    </main>
  )
}

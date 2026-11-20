import { Link } from '@tanstack/react-router'
import Avatar from './Avatar'

const FOLLOW_SUGGESTIONS = [
  { name: 'Mey Sokka', handle: 'mey', followers: '12.5K followers', avatar: undefined, initials: 'MS' },
  { name: 'Dara Chan', handle: 'dara', followers: '8.7K followers', avatar: undefined, initials: 'DR' },
  { name: 'Vichea', handle: 'vichea', followers: '6.3K followers', avatar: undefined, initials: 'VC' },
]

const TRENDING = [
  { rank: 1, topic: 'Tech & AI', posts: '24.8k posts' },
  { rank: 2, topic: 'Life in Cambodia', posts: '18.3k posts' },
  { rank: 3, topic: 'Developer Tips', posts: '16.7k posts' },
  { rank: 4, topic: 'Study and Career', posts: '14.1k posts' },
  { rank: 5, topic: 'Random Thoughts', posts: '12.9k posts' },
]

export default function RightRail() {
  return (
    <aside className="hidden w-80 flex-col gap-6 lg:block">
      <section className="island-shell rounded-2xl p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="m-0 text-lg font-bold text-[var(--sea-ink)]">Who to follow</h3>
          <Link
            to="/explore"
            className="text-[0.75rem] font-semibold text-[var(--brand-dark)] no-underline transition hover:underline"
          >
            See all →
          </Link>
        </div>
        <div className="space-y-3">
          {FOLLOW_SUGGESTIONS.map((person, i) => (
            <div key={i} className="flex items-center gap-3">
              <Avatar src={person.avatar} initials={person.initials} label={person.name} size={32} />
              <div className="flex-1 min-w-0">
                <p className="m-0 text-sm font-semibold text-[var(--sea-ink)] leading-none">
                  {person.name}
                </p>
                <p className="m-0 text-[0.75rem] text-[var(--sea-ink-soft)] leading-none">
                  {person.followers}
                </p>
              </div>
              <button
                type="button"
                className="rounded-full border border-[var(--line)] bg-white px-3 py-1 text-xs font-semibold text-[var(--sea-ink)] transition hover:bg-[var(--brand-light)] hover:text-[var(--brand-dark)]"
              >
                Follow
              </button>
            </div>
          ))}
        </div>
      </section>

      <section className="island-shell rounded-2xl p-5">
        <div className="flex items-center gap-2 mb-4">
          <span className="flex items-center gap-1 text-[var(--brand-dark)] font-bold">
            <span className="h-2 w-2 rounded-full bg-[var(--brand)]" />
            Trending
          </span>
        </div>
        <ol className="list-none m-0 p-0 space-y-3">
          {TRENDING.map((item) => (
            <li key={item.topic} className="flex items-center gap-3">
              <span className="w-5 text-center text-xs font-bold text-[var(--sea-ink-soft)]">
                {item.rank}
              </span>
              <div className="flex-1 min-w-0">
                <p className="m-0 text-sm font-semibold text-[var(--sea-ink)] leading-none">
                  {item.topic}
                </p>
                <p className="m-0 text-[0.75rem] text-[var(--sea-ink-soft)] leading-none">
                  {item.posts}
                </p>
              </div>
              <svg viewBox="0 0 16 16" className="h-3.5 w-3.5 shrink-0 text-[var(--sea-ink-soft)]" aria-hidden="true">
                <path fill="currentColor" d="m12 4-3 3m0 0-3-3m3 3-1.172 1.172a1 1 0 00-1.414 1.414l4 4a1 1 0 001.414-1.414L9.828 7H16a1 1 0 001-1V4a1 1 0 00-1-1h-6.172z" />
              </svg>
            </li>
          ))}
        </ol>
      </section>

      <section className="island-shell rounded-2xl p-5 text-center">
        <div className="flex flex-col items-center gap-3">
          <div className="relative h-20 w-20 mx-auto overflow-hidden rounded-full border border-[var(--line)] bg-[var(--brand-light)]">
            <svg viewBox="0 0 64 64" className="h-full w-full text-[var(--brand-dark)]" aria-hidden="true">
              <path d="M32 2c-3.2 0-6.3.4-9.3 1.2-5.6 1.5-10.8 5.6-14.2 11.2-2.1 3.4-3.3 7.3-3.3 11.3s1.2 7.8 3.3 11.3c3.4 5.6 8.6 9.7 14.2 11.2 3 0.8 6.1 1.2 9.3 1.2s6.3-.4 9.3-1.2c5.6-1.5 10.8-5.6 14.2-11.2 2.1-3.4 3.3-7.3 3.3-11.3s-1.2-7.8-3.3-11.3c-3.4-5.6-8.6-9.7-14.2-11.2-3-0.8-6.1-1.2-9.3-1.2zm0 58c-1.7 0-3.3-.2-4.8-.6-2.8-.8-5.4-2.8-7.1-5.6-1-1.7-1.6-3.6-1.6-5.6s.6-3.9 1.6-5.6c1.7-2.8 4.3-4.8 7.1-5.6 1.5-.4 3.1-.6 4.8-.6s3.3.2 4.8.6c2.8.8 5.4 2.8 7.1 5.6 1 1.7 1.6 3.6 1.6 5.6s-.6 3.9-1.6 5.6c-1.7 2.8-4.3 4.8-7.1 5.6-1.5.4-3.1.6-4.8.6z" fill="currentColor"/>
            </svg>
          </div>
          <h3 className="m-0 text-lg font-bold text-[var(--sea-ink)]">Join Chafgram Today</h3>
          <p className="m-0 text-sm text-[var(--sea-ink-soft)] leading-6 max-w-xs mx-auto">
            Be part of our growing company community!
          </p>
          <Link
            to="/welcome"
            className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-6 py-2.5 text-sm font-semibold text-white transition hover:bg-[var(--brand-dark)]"
          >
            Sign Up
          </Link>
        </div>
      </section>
    </aside>
  )
}

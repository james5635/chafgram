import { useQuery } from '@apollo/client/react'
import { createFileRoute, useParams } from '@tanstack/react-router'
import Avatar from '../components/Avatar'
import AppShell from '../components/AppShell'
import { gradientPlaceholder } from '../lib/placeholders'
import { MemberDocument } from '../generated/graphql'

export const Route = createFileRoute('/profile/$username')({
  component: Profile,
})

const COVER = gradientPlaceholder({ colors: ['#6ba8e0', '#bcdcf5'], width: 1200, height: 320 })

function ProfilePanels() {
  return (
    <aside className="hidden w-72 flex-shrink-0 space-y-4 py-6 pr-4 xl:block" aria-label="Profile details">
      <section className="island-shell rounded-2xl p-5">
        <h3 className="m-0 mb-4 text-base font-bold text-[var(--sea-ink)]">About</h3>
        <ul className="m-0 space-y-3 p-0 list-none">
          <li className="flex items-center gap-2.5 text-sm text-[var(--sea-ink-soft)]">
            <span aria-hidden="true">📍</span> Your company workspace
          </li>
          <li className="flex items-center gap-2.5 text-sm text-[var(--sea-ink-soft)]">
            <span aria-hidden="true">📅</span> Member of Chafgram
          </li>
        </ul>
      </section>
    </aside>
  )
}

export function Profile() {
  const params = useParams({ from: '/profile/$username' })
  const username = params.username
  const { data, loading, error } = useQuery(MemberDocument, {
    variables: { username },
    skip: !username || username === 'me',
  })
  const member = data?.member

  return (
    <AppShell rightRail={<ProfilePanels />}>
      <div className="page-wrap px-4 pb-8 pt-6">
        <section className="island-shell rise-in overflow-hidden rounded-2xl">
          <div className="relative h-36 sm:h-44">
            <img src={COVER} alt="Profile cover" className="h-full w-full object-cover" />
          </div>

          <div className="px-6 pb-4 sm:px-8">
            <div className="-mt-8 mb-1 inline-block rounded-full border-4 border-white bg-white shadow-md">
              <Avatar
                initials={(member?.displayName ?? username ?? '?').slice(0, 2).toUpperCase()}
                label={member?.displayName ?? username ?? 'Member'}
                size={56}
              />
            </div>

            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="flex-1 min-w-0">
                <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">
                  {member?.displayName ?? (username === 'me' ? 'Your profile' : username)}
                </h1>
                <span className="text-sm font-medium text-[var(--sea-ink-soft)]">@{member?.username ?? username}</span>
                <p className="m-0 mt-2 max-w-md text-sm leading-6 text-[var(--sea-ink-soft)]">
                  {member?.bio ?? 'No bio yet.'}
                </p>
                <div className="mt-4 flex items-center gap-5 text-sm">
                  <span><span className="font-semibold text-[var(--sea-ink)]">{member?.followerCount ?? 0}</span> <span className="text-[var(--sea-ink-soft)]">Followers</span></span>
                  <span><span className="font-semibold text-[var(--sea-ink)]">{member?.followingCount ?? 0}</span> <span className="text-[var(--sea-ink-soft)]">Following</span></span>
                </div>
              </div>
              <div className="flex flex-shrink-0 gap-2">
                <button type="button" className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-4 py-2 text-sm font-semibold !text-white shadow-lg shadow-[var(--brand)]/30 transition hover:bg-[var(--brand-dark)]">
                  Edit Profile
                </button>
                <button type="button" className="flex h-9 w-9 items-center justify-center rounded-full border border-[var(--line)] bg-white text-[var(--sea-ink-soft)] transition hover:bg-[var(--brand-light)]" aria-label="Profile settings">
                  <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
                    <path fill="currentColor" d="M12 8a4 4 0 100 8 4 4 0 000-8zm0 6a2 2 0 110-4 2 2 0 010 4zm8.4-2a8.4 8.4 0 00-.1-1.3l2-1.6-2-3.4-2.4 1a8 8 0 00-2.3-1.3L15.2 2H8.8l-.4 2.4A8 8 0 006.1 5.7l-2.4-1-2 3.4 2 1.6a8.4 8.4 0 000 2.6l-2 1.6 2 3.4 2.4-1a8 8 0 002.3 1.3l.4 2.4h6.4l.4-2.4a8 8 0 002.3-1.3l2.4 1 2-3.4-2-1.6c.1-.4.1-.8.1-1.3z" />
                  </svg>
                </button>
              </div>
            </div>

            {error && <p className="mt-4 text-sm text-[#9f3030]">Could not load profile: {error.message}</p>}
            {loading && <div className="mt-4 h-16 animate-pulse rounded-xl" />}
            {!loading && !error && username !== 'me' && !member && (
              <p className="mt-4 text-sm text-[var(--sea-ink-soft)]">Member not found.</p>
            )}

            <div className="mt-5 flex gap-6 border-t border-[var(--line)] pt-3">
              {['Posts', 'Replies', 'Media', 'Likes'].map((tab, i) => (
                <button
                  key={tab}
                  type="button"
                  className={`border-b-2 pb-2 text-sm font-semibold transition ${
                    i === 0
                      ? 'border-[var(--brand)] text-[var(--brand-dark)]'
                      : 'border-transparent text-[var(--sea-ink-soft)] hover:text-[var(--sea-ink)]'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  )
}

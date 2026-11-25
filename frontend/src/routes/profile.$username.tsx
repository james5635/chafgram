import { useQuery } from '@apollo/client/react'
import { createFileRoute, useParams } from '@tanstack/react-router'
import { MapPin, Calendar, Settings, Link as LinkIcon, Code2 } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import AppShell from '@/components/AppShell'
import { profileCoverPlaceholder } from '@/lib/placeholders'
import { MemberDocument, MeDocument } from '@/generated/graphql'

export const Route = createFileRoute('/profile/$username')({
  component: Profile,
})

const COVER = profileCoverPlaceholder()

function ProfilePanels() {
  return (
    <aside className="hidden w-72 flex-shrink-0 space-y-4 py-6 pr-4 xl:block" aria-label="Profile details">
      <section className="island-shell rounded-xl p-5">
        <h3 className="m-0 mb-4 text-base font-bold text-[var(--sea-ink)]">About</h3>
        <ul className="m-0 space-y-3 p-0 list-none">
          <li className="flex items-center gap-2.5 text-sm text-[var(--sea-ink-soft)]">
            <MapPin className="h-4 w-4" aria-hidden="true" /> Phnom Penh, Cambodia
          </li>
          <li className="flex items-center gap-2.5 text-sm text-[var(--sea-ink-soft)]">
            <LinkIcon className="h-4 w-4" aria-hidden="true" /> <span className="text-[var(--brand-dark)]">github.com/james5635</span>
          </li>
          <li className="flex items-center gap-2.5 text-sm text-[var(--sea-ink-soft)]">
            <Calendar className="h-4 w-4" aria-hidden="true" /> Joined March 2024
          </li>
        </ul>
      </section>
      <section className="island-shell rounded-xl p-5">
        <h3 className="m-0 mb-4 flex items-center gap-2 text-base font-bold text-[var(--sea-ink)]">
          <Code2 className="h-4 w-4 text-[var(--brand)]" aria-hidden="true" /> Skills
        </h3>
        <div className="flex flex-wrap gap-2">
          {['Python', 'TypeScript', 'Java', 'React'].map((skill) => (
            <span key={skill} className="rounded-full bg-[var(--brand-light)] px-2.5 py-1 text-xs font-semibold text-[var(--brand-dark)]">{skill}</span>
          ))}
        </div>
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
  const { data: meData } = useQuery(MeDocument, { skip: username !== 'me' })
  const member = data?.member ?? meData?.me

  return (
    <AppShell rightRail={<ProfilePanels />}>
      <div className="page-wrap px-4 pb-8 pt-6">
        <section className="island-shell rise-in overflow-hidden rounded-xl">
          <div className="relative h-28 sm:h-32">
            <img src={COVER} alt="Profile cover" className="h-full w-full object-cover" />
          </div>

          <div className="px-6 pb-4 sm:px-8">
            <div className="-mt-8 mb-1 inline-block rounded-full border-4 border-white bg-white shadow-md">
              <Avatar
                className="h-14 w-14"
                aria-label={member?.displayName ?? username ?? 'Member'}
              >
                <AvatarFallback>
                  {(member?.displayName ?? username ?? '?').slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
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
                <Button className="rounded-full bg-[var(--brand)] border border-[var(--brand)] px-4 py-2 text-sm font-semibold !text-white shadow-lg shadow-[var(--brand)]/30 transition hover:bg-[var(--brand-dark)]" asChild>
                  <span>Edit Profile</span>
                </Button>
                <Button variant="ghost" size="icon" aria-label="Profile settings">
                  <Settings className="h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
            </div>

            {error && <p className="mt-4 text-sm text-[#9f3030]">Could not load profile: {error.message}</p>}
            {loading && <div className="mt-4 h-16 animate-pulse rounded-xl" />}
            {!loading && !error && username !== 'me' && !member && (
              <p className="mt-4 text-sm text-[var(--sea-ink-soft)]">Member not found.</p>
            )}

            <div className="mt-5 flex gap-6 border-t border-[var(--line)] pt-3">
              {['Posts', 'Replies', 'Media', 'Likes'].map((tab, i) => (
                <Button
                  key={tab}
                  type="button"
                  variant="ghost"
                  className={`border-b-2 pb-2 text-sm font-semibold transition ${
                    i === 0
                      ? 'border-[var(--brand)] text-[var(--brand-dark)]'
                      : 'border-transparent text-[var(--sea-ink-soft)] hover:text-[var(--sea-ink)]'
                  }`}
                >
                  {tab}
                </Button>
              ))}
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  )
}

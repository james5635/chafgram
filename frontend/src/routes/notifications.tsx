import { createFileRoute } from '@tanstack/react-router'
import Avatar from '../components/Avatar'
import AppShell from '../components/AppShell'
import { gradientPlaceholder } from '../lib/placeholders'

export const Route = createFileRoute('/notifications')({
  component: Notifications,
})

const TABS = ['All', 'Likes', 'Comments', 'Follows', 'Mentions'] as const

const THUMB = gradientPlaceholder({ colors: ['#7aa7d8', '#c2dcf3'], width: 80, height: 80 })

const ITEMS = [
  { actor: 'Sokha', action: 'liked your post', target: '', time: '2m ago', unread: true, initials: 'SK', thumb: THUMB },
  { actor: 'Dara Chan', action: 'commented:', target: 'Great post! ❤️', time: '5m ago', unread: true, initials: 'DR', thumb: THUMB },
  { actor: 'Vichea', action: 'started following you', target: '', time: '10m ago', unread: false, initials: 'VC', thumb: null, follow: true },
  { actor: 'Mey Sokha', action: 'liked your photo', target: '', time: '1h ago', unread: false, initials: 'MY', thumb: THUMB },
  { actor: 'Chanha', action: 'mentioned you in a comment', target: '', time: '2h ago', unread: false, initials: 'CH', thumb: THUMB },
]

function Notifications() {
  const unreadCount = ITEMS.filter((i) => i.unread).length

  return (
    <AppShell>
      <div className="page-wrap px-4 pb-8 pt-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Notifications</h1>
            {unreadCount > 0 && (
              <p className="m-0 mt-1 text-xs text-[var(--sea-ink-soft)]">{unreadCount} unread</p>
            )}
          </div>
          <button type="button" className="rounded-full border border-[var(--line)] bg-white/80 px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--brand-light)]">
            Mark all as read
          </button>
        </div>

        <div className="flex gap-5 mb-5 border-b border-[var(--line)] overflow-x-auto no-scrollbar">
          {TABS.map((tab, i) => (
            <button
              key={tab}
              type="button"
              className={`whitespace-nowrap border-b-2 pb-2 text-sm font-semibold transition ${
                i === 0
                  ? 'border-[var(--brand)] text-[var(--brand-dark)]'
                  : 'border-transparent text-[var(--sea-ink-soft)] hover:text-[var(--sea-ink)]'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <section className="island-shell overflow-hidden rounded-2xl">
          {ITEMS.map((item, i) => (
            <article key={i} className={`flex items-center gap-3 px-4 py-3.5 sm:px-5 ${i < ITEMS.length - 1 ? 'border-b border-[var(--line)]' : ''} ${item.unread ? 'bg-[var(--brand-light)]/30' : ''}`}>
              <div className="flex-shrink-0">
                <Avatar initials={item.initials} label={item.actor} size={32} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="m-0 text-sm leading-6 text-[var(--sea-ink)]">
                  <span className="font-semibold">{item.actor}</span>{' '}
                  {item.action}{' '}
                  {item.target && <span className="text-[var(--sea-ink-soft)]">{item.target}</span>}
                </p>
                <span className="text-[0.7rem] text-[var(--sea-ink-soft)]">{item.time}</span>
              </div>
              {item.follow && (
                <button type="button" className="rounded-full border border-[var(--line)] bg-white px-3 py-1 text-xs font-semibold text-[var(--sea-ink)] transition hover:bg-[var(--brand-light)] hover:text-[var(--brand-dark)]">
                  Follow
                </button>
              )}
              {item.thumb && (
                <img src={item.thumb} alt="" className="h-10 w-10 flex-shrink-0 rounded-lg border border-[var(--line)] object-cover" />
              )}
              {item.unread && <span className="h-2 w-2 flex-shrink-0 rounded-full bg-[var(--brand)]" aria-hidden="true" />}
            </article>
          ))}
        </section>
      </div>
    </AppShell>
  )
}

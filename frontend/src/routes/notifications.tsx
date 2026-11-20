import { createFileRoute } from '@tanstack/react-router'
import Avatar from '../components/Avatar'

export const Route = createFileRoute('/notifications')({
  component: Notifications,
})

const ITEMS = [
  { actor: 'Mey', action: 'liked your post', target: '“A beautiful day to build something great!”', time: '1h ago', unread: true, avatar: undefined, initials: 'MY' },
  { actor: 'Dara', action: 'commented on your post', target: '“Learning never stops.”', time: '3h ago', unread: true, avatar: undefined, initials: 'DR' },
  { actor: 'Sokha', action: 'started following you', target: '', time: '5h ago', unread: false, avatar: undefined, initials: 'SK' },
  { actor: 'Rojame', action: 'mentioned you in a comment', target: '“What do you think about the new flow?”', time: '1d ago', unread: false, avatar: undefined, initials: 'RJ' },
  { actor: 'Dara', action: 'replied to your comment', target: '“Great point — agreed.”', time: '2d ago', unread: false, avatar: undefined, initials: 'DR' },
]

function Notifications() {
  const unreadCount = ITEMS.filter((i) => i.unread).length

  return (
    <main className="page-wrap px-4 pb-8 pt-14">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Notifications</h1>
          {unreadCount > 0 && (
            <p className="m-0 mt-1 text-sm text-[var(--sea-ink-soft)]">{unreadCount} unread</p>
          )}
        </div>
        <button type="button" className="rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]">
          Mark all as read
        </button>
      </div>

      <section className="space-y-3">
        {ITEMS.map((item, i) => (
          <article key={i} className="island-shell rise-in flex items-center gap-3 p-4 sm:p-5 rounded-xl" style={{ animationDelay: `${i * 90}ms` }}>
            <div className="flex-shrink-0">
              <Avatar src={item.avatar} initials={item.initials} label={item.actor} size={32} />
            </div>
            <div className="flex-1 min-w-0">
              <p className="m-0 text-base text-[var(--sea-ink)] leading-6">
                <span className="font-semibold">{item.actor}</span>{' '}
                {item.action}{' '}
                {item.target ? `<span className="text-[var(--sea-ink-soft)] font-normal">“${item.target}”</span>` : ''}
              </p>
              <span className="text-[0.75rem] text-[var(--sea-ink-soft)] block mt-0.5">{item.time}</span>
            </div>
            {item.unread && (
              <div className="ml-auto flex-shrink-0">
                <span className="inline-block h-2 w-2 rounded-full bg-[var(--lagoon)] shadow-[0_0_0_2px_rgba(255,255,255,0.9)]" aria-hidden="true" />
              </div>
            )}
          </article>
        ))}
      </section>

      <div className="mt-10 flex items-center justify-center text-[var(--sea-ink-soft)] text-sm">
        That's everything so far.
      </div>
    </main>
  )
}

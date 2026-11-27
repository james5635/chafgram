import { useMutation, useQuery } from '@apollo/client/react'
import { createFileRoute } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { gradientPlaceholder } from '@/lib/placeholders'
import Avatar from '../components/Avatar'
import AppShell from '../components/AppShell'
import { BusyOverlay } from '../components/BusyOverlay'
import { useBusy } from '../hooks/useBusy'
import {
  MarkNotificationsReadDocument,
  MyNotificationsDocument,
  UnreadNotificationsDocument,
  type MyNotificationsQuery,
} from '../generated/graphql'

export const Route = createFileRoute('/notifications')({
  component: Notifications,
})

const TABS = ['All', 'Likes', 'Comments', 'Follows', 'Mentions'] as const
const THUMBNAILS = [
  gradientPlaceholder({ colors: ['#93c5fd', '#2563eb'], width: 96, height: 64 }),
  gradientPlaceholder({ colors: ['#bfdbfe', '#60a5fa'], width: 96, height: 64 }),
  gradientPlaceholder({ colors: ['#dbeafe', '#3b82f6'], width: 96, height: 64 }),
]

const ACTIONS: Record<string, string> = {
  FOLLOW: 'started following you',
  LIKE: 'liked your post',
  COMMENT: 'commented on your post',
  REPLY: 'replied to your comment',
  MENTION: 'mentioned you in a comment',
  MESSAGE: 'sent you a message',
  SYSTEM: 'sent an update',
}

type Notification = MyNotificationsQuery['myNotifications'][number]

function actionText(notification: Notification): string {
  return ACTIONS[notification.type] ?? notification.type.toLowerCase().replace(/_/g, ' ')
}

export function Notifications() {
  const { data, loading, error } = useQuery(MyNotificationsDocument, { variables: { first: 30 } })
  const { data: unreadData } = useQuery(UnreadNotificationsDocument)
  const [markAllReadMutation] = useMutation(MarkNotificationsReadDocument, {
    refetchQueries: [MyNotificationsDocument, UnreadNotificationsDocument],
  })
  const { busy, run } = useBusy()

  const notifications = data?.myNotifications ?? []
  const unread = unreadData?.unreadNotifications ?? 0

  async function markAllRead() {
    await run(() => markAllReadMutation())
  }

  return (
    <AppShell>
      <div className="page-wrap px-4 pb-8 pt-6">
        <div className="relative mb-4 flex items-center justify-between" aria-busy={busy}>
          <div inert={busy ? true : undefined} className="flex w-full items-center justify-between">
            <div>
              <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Notifications</h1>
              {unread > 0 && <p className="m-0 mt-1 text-xs text-[var(--sea-ink-soft)]">{unread} unread</p>}
            </div>
            <button
              type="button"
              onClick={() => void markAllRead()}
              disabled={unread === 0 || busy}
              className="rounded-full border border-[var(--line)] bg-white/80 px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--brand-light)] disabled:opacity-50"
            >
              Mark all as read
            </button>
          </div>
          {busy && <BusyOverlay label="Updating…" />}
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

        <section className="island-shell overflow-hidden rounded-xl" aria-busy={loading}>
          {error && (
            <p className="m-0 p-5 text-sm text-[#9f3030]">Could not load notifications: {error.message}</p>
          )}
          {loading && !data && <div className="h-40 animate-pulse" />}
          {!loading && !error && notifications.length === 0 && (
            <p className="m-0 p-6 text-center text-sm text-[var(--sea-ink-soft)]">No notifications yet.</p>
          )}
          {notifications.map((notification, i) => (
            <article
              key={notification.id}
              className={`flex items-center gap-3 px-4 py-3.5 sm:px-5 ${i < notifications.length - 1 ? 'border-b border-[var(--line)]' : ''} ${notification.readAt ? '' : 'bg-[var(--brand-light)]/30'}`}
            >
              <Avatar
                initials={notification.actor?.displayName?.slice(0, 2).toUpperCase() ?? '??'}
                label={notification.actor?.displayName ?? 'Unknown member'}
                size={32}
              />
              <div className="flex-1 min-w-0">
                <p className="m-0 text-sm leading-6 text-[var(--sea-ink)]">
                  <span className="font-semibold">{notification.actor?.displayName ?? 'A member'}</span>{' '}
                  {actionText(notification)}
                </p>
                <span className="text-[0.7rem] text-[var(--sea-ink-soft)]">{notification.createdAt}</span>
              </div>
              {notification.type === 'FOLLOW' ? (
                <Button variant="outline" size="sm" className="rounded-full border-[var(--brand)] px-4 text-xs text-[var(--brand-dark)]">Follow</Button>
              ) : (
                <img src={THUMBNAILS[i % THUMBNAILS.length]} alt="" className="h-9 w-12 shrink-0 rounded-md object-cover" />
              )}
              {!notification.readAt && <span className="h-2 w-2 flex-shrink-0 rounded-full bg-[var(--brand)]" aria-hidden="true" />}
            </article>
          ))}
        </section>
      </div>
    </AppShell>
  )
}

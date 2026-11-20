import { createFileRoute, Link } from '@tanstack/react-router'
import Avatar from '../components/Avatar'

export const Route = createFileRoute('/messages')({
  component: Messages,
})

const CONVERSATIONS = [
  { id: 'c1', name: 'Dara', avatar: undefined, initials: 'DR', preview: "Sounds good — let's sync tomorrow.", time: '10:42 AM', unread: 2, muted: false },
  { id: 'c2', name: 'Sokha', avatar: undefined, initials: 'SK', preview: 'Did you see the latest design review?', time: 'Yesterday', unread: 0, muted: true },
  { id: 'c3', name: 'Product Team', avatar: undefined, initials: 'PT', preview: 'Ship It! 🚀', time: 'Mon', unread: 0, muted: false },
  { id: 'c4', name: 'Mey', avatar: undefined, initials: 'MY', preview: 'Thanks for the help earlier.', time: 'Fri', unread: 0, muted: false },
]

function Messages() {
  return (
    <main className="page-wrap px-4 pb-8 pt-14">
      <div className="flex items-center gap-4 mb-6">
        <h1 className="m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl">Messages</h1>
        <button type="button" className="ml-auto rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]">
          New message
        </button>
      </div>

      <section className="island-shell rise-in rounded-2xl overflow-hidden border border-[var(--line)]">
        <div className="border-b border-[var(--line)] px-4 sm:px-6 py-3 text-xs font-semibold uppercase tracking-wider text-[var(--sea-ink-soft)]">
          Direct messages
        </div>
        <div className="divide-y divide-[var(--line)]">
          {CONVERSATIONS.map((conv) => (
            <Link
              key={conv.id}
              to="/messages/$conversationId"
              params={{ conversationId: conv.id }}
              className="block px-4 sm:px-6 py-4 hover:bg-[color-mix(in_oklab,var(--surface-strong)60%,white_40%)] transition"
            >
              <div className="flex items-center gap-3">
                <div className="flex-shrink-0">
                  <Avatar src={conv.avatar} initials={conv.initials} label={conv.name} size={40} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-baseline gap-2">
                    <span className="text-sm font-semibold text-[var(--sea-ink)] block">{conv.name}</span>
                    {conv.muted && (
                      <span className="inline-flex items-center gap-1 rounded-full border border-[rgba(47,106,74,0.18)] bg-[color-mix(in_oklab,var(--palm)10%,var(--chip-bg))] px-2 py-0.5 text-[0.65rem] font-bold text-[var(--palm)]">
                        <span className="h-1.5 w-1.5 rounded-full bg-[var(--palm)]" aria-hidden="true" />
                        Muted
                      </span>
                    )}
                    {conv.unread > 0 && (
                      <span className="ml-auto inline-flex h-5 w-5 items-center justify-center rounded-full border border-white bg-[var(--lagoon)] px-1 text-[0.625rem] font-bold text-white shadow-sm">
                        {conv.unread}
                      </span>
                    )}
                  </div>
                  <p className="m-0 text-[var(--sea-ink-soft)] text-sm truncate">{conv.preview}</p>
                </div>
                <span className="text-[0.7rem] text-[var(--sea-ink-soft)] whitespace-nowrap">{conv.time}</span>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </main>
  )
}

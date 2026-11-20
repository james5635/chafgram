import { createFileRoute, Link } from '@tanstack/react-router'
import Avatar from '../components/Avatar'
import AppShell from '../components/AppShell'

export const Route = createFileRoute('/messages/$conversationId')({
  component: MessageThread,
})

const MESSAGES = [
  { id: 'm1', sender: { name: 'Dara', initials: 'DR' }, text: "Hey! Did you get a chance to look at the new designs?", time: '9:14 AM', mine: false },
  { id: 'm2', sender: { name: 'You', initials: 'YD' }, text: "Just saw them — they look great!", time: '9:16 AM', mine: true },
  { id: 'm3', sender: { name: 'Dara', initials: 'DR' }, text: "Awesome. I'll push the final assets to the folder shortly.", time: '9:18 AM', mine: false },
  { id: 'm4', sender: { name: 'Dara', initials: 'DR' }, text: "...and done. Let me know what you think.", time: '9:22 AM', mine: false },
  { id: 'm5', sender: { name: 'You', initials: 'YD' }, text: "Received them. Looks perfect, thanks!", time: '9:25 AM', mine: true, edited: true },
]

function MessageThread() {
  return (
    <AppShell>
    <div className="page-wrap px-4 pb-8 pt-6">
      <header className="mb-6 flex items-center gap-3">
        <Link to="/messages" className="text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]">
          <svg viewBox="0 0 16 16" className="h-5 w-5" aria-hidden="true"><path fill="currentColor" d="M11.25 4.75l-4 4 4 4 1.06-1.06L7.81 8.5l4.5-4.5-1.06-1.06z"/></svg>
          Back
        </Link>
        <div className="flex items-center gap-2">
          <Avatar initials="DR" label="Dara" size={32} />
          <span className="text-sm font-semibold text-[var(--sea-ink)]">Dara</span>
        </div>
        <div className="ml-auto flex items-center gap-2">
          <span className="hidden sm:inline-block text-[0.75rem] text-[var(--sea-ink-soft)]">Delivered</span>
          <button type="button" className="p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="More options">
            <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M10 7a1 1 0 1 1-2 0 1 1 0 0 1 2 0zm-5 0a1 1 0 1 1-2 0 1 1 0 0 1 2 0zM7 11a1 1 0 1 0 0-2 1 1 0 0 0 0 2zm4 0a1 1 0 1 0 0-2 1 1 0 0 0 0 2z"/></svg>
          </button>
        </div>
      </header>

      <section className="island-shell rise-in rounded-2xl h-[60vh] flex flex-col overflow-hidden border border-[var(--line)]">
        <div className="flex-1 overflow-y-auto p-4 sm:p-6">
          <div className="space-y-4">
            {MESSAGES.map((msg) => (
              <div key={msg.id} className={`flex ${msg.mine ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] rounded-2xl p-3 ${msg.mine ? 'bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] text-[var(--sea-ink)]' : 'bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-[var(--sea-ink)] border border-[var(--line)]'}`}>
                  {!msg.mine && (
                    <p className="m-0 mb-1 text-xs font-semibold text-[var(--sea-ink-soft)] uppercase tracking-wider">{msg.sender.name}</p>
                  )}
                  <p className="m-0 text-sm leading-6 whitespace-pre-wrap">{msg.text}</p>
                  <p className="m-0 mt-1 text-[0.65rem] text-[var(--sea-ink-soft)] text-right">{msg.time}{msg.edited ? ' · edited' : ''}</p>
                </div>
              </div>
            ))}
            <div className="h-8" />
          </div>
        </div>

        <div className="border-t border-[var(--line)] p-4 sm:p-6 bg-[color-mix(in_oklab,var(--surface-strong)90%,white_10%)]">
          <div className="relative">
            <input
              type="text"
              placeholder="Message…"
              className="w-full pl-10 pr-4 py-2 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[color-mix(in_oklab,var(--lagoon-deep)58%,var(--line))] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--lagoon)24%,transparent)]"
            />
            <button type="button" className="absolute left-2 top-1/2 -translate-y-1/2 p-1 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Attach">
              <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M11.8 12c0-.7-.6-1.3-1.3-1.3h-.8c-.1 0-.3-.1-.4-.2l-2.3-2.3c-.4-.4-1-.4-1.4 0s-.4 1 0 1.4l.6.6c.2.2.4.3.7.3h.1c.3 0 .6-.1.8-.3l2.3-2.3c.4-.4.4-1 0-1.4v-.6c0-.7-.6-1.3-1.3-1.3h-3c-.7 0-1.3.6-1.3 1.3v3c0 .7.6 1.3 1.3 1.3h3c.7 0 1.3-.6 1.3-1.3v-.8zM6.5 6c-.3 0-.5-.2-.5-.5s.2-.5.5-.5.5.2.5.5-.2.5-.5.5zm7.5-4.5H2.5C1.1 1.5 0 2.6 0 4v8c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.4-.9-2.5-2-2.5z"/></svg>
            </button>
            <button type="button" className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full border border-[var(--line)] bg-[var(--lagoon)] p-1.5 text-[var(--lagoon-deep)] font-bold transition hover:bg-[var(--lagoon-deep)] hover:text-white shadow-sm" aria-label="Send">
              <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="m14 8.2 2-3.6-3.6-2-2.9 3.5-.7-.1-1.2 1L3 7.9l1.2 1.3L8 8.8l2.9-3.5L14 8.2z"/></svg>
            </button>
          </div>
        </div>
      </section>
    </div>
    </AppShell>
  )
}

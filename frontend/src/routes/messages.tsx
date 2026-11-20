import { createFileRoute } from '@tanstack/react-router'
import Avatar from '../components/Avatar'
import AppShell from '../components/AppShell'

export const Route = createFileRoute('/messages')({
  component: Messages,
})

const CONVERSATIONS = [
  { id: 'c1', name: 'Sokha', preview: 'Hey! How are you? 😊', time: '2m', initials: 'SK', unread: 2 },
  { id: 'c2', name: 'Dara Chan', preview: 'Sounds great!', time: '1h', initials: 'DR', unread: 0 },
  { id: 'c3', name: 'Vichea', preview: 'See you there! ❤️', time: '3h', initials: 'VC', unread: 0 },
  { id: 'c4', name: 'Mey Sokha', preview: 'Alright, thank!', time: '1d', initials: 'MY', unread: 0 },
]

const MESSAGES = [
  { from: 'them', text: 'Hey! How are you? 😊', time: '9:14 AM' },
  { from: 'me', text: "I'm good! How about you?", time: '9:16 AM' },
  { from: 'them', text: "Great! Let's catch up soon.", time: '10:26 AM' },
]

function Messages() {
  return (
    <AppShell>
      <div className="page-wrap px-4 pb-8 pt-6">
        <div className="island-shell rise-in flex h-[70vh] min-h-[520px] overflow-hidden rounded-2xl">
          <div className="flex w-full flex-col border-r border-[var(--line)] sm:w-72 lg:w-80">
            <div className="border-b border-[var(--line)] px-4 py-4">
              <h1 className="m-0 text-lg font-bold text-[var(--sea-ink)]">Messages</h1>
              <div className="relative mt-3">
                <span className="absolute left-2.5 top-1/2 -translate-y-1/2 text-[var(--sea-ink-soft)]">
                  <svg viewBox="0 0 16 16" className="h-3.5 w-3.5" aria-hidden="true"><path fill="currentColor" d="M11.742 10.344a6.5 6.5 0 1 0 0-9.188 6.5 6.5 0 0 0 0 9.188zm3.156-4.188L9.375 11.25l-.563-.563a4.485 4.485 0 0 1-6.343 0L3.375 6.156l.563-.563a4.484 4.484 0 0 1 6.344 0l.875.876z"/></svg>
                </span>
                <input type="search" placeholder="Search conversations..." className="w-full rounded-full border border-[var(--line)] bg-white/80 py-1.5 pl-8 pr-3 text-xs text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none focus:border-[var(--brand)]" />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto">
              {CONVERSATIONS.map((conv, i) => (
                <button key={conv.id} type="button" className={`flex w-full items-center gap-3 border-b border-[var(--line)] px-3 py-3 text-left transition ${i === 0 ? 'bg-[var(--brand-light)]/70' : 'hover:bg-[var(--brand-light)]/40'}`}>
                  <Avatar initials={conv.initials} label={conv.name} size={32} />
                  <span className="flex-1 min-w-0">
                    <span className="flex items-baseline gap-2">
                      <span className="text-sm font-semibold text-[var(--sea-ink)]">{conv.name}</span>
                      <span className="ml-auto text-[0.65rem] text-[var(--sea-ink-soft)]">{conv.time}</span>
                    </span>
                    <span className="block truncate text-xs text-[var(--sea-ink-soft)]">{conv.preview}</span>
                  </span>
                  {conv.unread > 0 && (
                    <span className="inline-flex h-5 w-5 items-center justify-center rounded-full border border-white bg-[var(--brand)] px-1 text-[0.625rem] font-bold text-white shadow-sm">{conv.unread}</span>
                  )}
                </button>
              ))}
            </div>
          </div>

          <div className="hidden flex-1 flex-col sm:flex">
            <div className="flex items-center gap-3 border-b border-[var(--line)] px-4 py-3">
              <Avatar initials="SK" label="Sokha" size={32} />
              <div className="flex-1">
                <p className="m-0 text-sm font-semibold text-[var(--sea-ink)]">Sokha</p>
                <p className="m-0 text-[0.7rem] text-[var(--sea-ink-soft)]">2m ago</p>
              </div>
              <button type="button" className="p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Video call">
                <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true"><path fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M15 10l4.5-3v10L15 14m-10 4h9a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
              </button>
              <button type="button" className="p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Message info">
                <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true"><circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.8"/><path fill="currentColor" d="M11 11h2v6h-2zm0-4h2v2h-2z"/></svg>
              </button>
            </div>

            <div className="flex-1 space-y-3 overflow-y-auto bg-[var(--foam)]/40 p-4">
              {MESSAGES.map((m, i) => (
                <div key={i} className={`flex ${m.from === 'me' ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${m.from === 'me' ? 'bg-[var(--brand)] text-white' : 'border border-[var(--line)] bg-white text-[var(--sea-ink)]'}`}>
                    <p className="m-0 leading-6">{m.text}</p>
                    <p className={`m-0 mt-0.5 text-right text-[0.65rem] ${m.from === 'me' ? 'text-white/80' : 'text-[var(--sea-ink-soft)]'}`}>{m.time}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="border-t border-[var(--line)] p-3">
              <div className="relative flex items-center gap-2">
                <input type="text" placeholder="Type a message..." className="flex-1 rounded-full border border-[var(--line)] bg-white py-2 pl-4 pr-10 text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none focus:border-[var(--brand)]" />
                <button type="button" className="absolute right-2 flex h-7 w-7 items-center justify-center rounded-full bg-[var(--brand)] text-white transition hover:bg-[var(--brand-dark)]" aria-label="Send message">
                  <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M2 2l12 6-12 6 2-6-2-6z"/></svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  )
}

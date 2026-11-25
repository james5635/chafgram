import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft, MoreHorizontal, Paperclip, Send } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import AppShell from '@/components/AppShell'

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
            <ArrowLeft className="h-5 w-5 mr-1" aria-hidden="true" />
            Back
          </Link>
          <div className="flex items-center gap-2">
            <Avatar className="h-8 w-8" aria-label="Dara">
              <AvatarFallback name="Dara" />
            </Avatar>
            <span className="text-sm font-semibold text-[var(--sea-ink)]">Dara</span>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <span className="hidden sm:inline-block text-[0.75rem] text-[var(--sea-ink-soft)]">Delivered</span>
            <Button variant="ghost" size="icon" aria-label="More options">
              <MoreHorizontal className="h-4 w-4" aria-hidden="true" />
            </Button>
          </div>
        </header>

      <section className="island-shell rise-in rounded-xl h-[60vh] flex flex-col overflow-hidden border border-[var(--line)]">
        <div className="flex-1 overflow-y-auto p-4 sm:p-6">
          <div className="space-y-4">
            {MESSAGES.map((msg) => (
              <div key={msg.id} className={`flex ${msg.mine ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] rounded-xl p-3 ${msg.mine ? 'bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] text-[var(--sea-ink)]' : 'bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-[var(--sea-ink)] border border-[var(--line)]'}`}>
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
            <Input
              type="text"
              placeholder="Message…"
              className="w-full pl-10 pr-4 py-2 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[color-mix(in_oklab,var(--lagoon-deep)58%,var(--line))] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--lagoon)24%,transparent)]"
            />
            <Button variant="ghost" size="icon" className="absolute left-2 top-1/2 -translate-y-1/2 p-1 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Attach">
              <Paperclip className="h-4 w-4" aria-hidden="true" />
            </Button>
            <Button variant="default" size="icon" className="absolute right-2 top-1/2 -translate-y-1/2 rounded-full border border-[var(--line)] bg-[var(--lagoon)] p-1.5 text-[var(--lagoon-deep)] font-bold transition hover:bg-[var(--lagoon-deep)] hover:text-white shadow-sm" aria-label="Send">
              <Send className="h-4 w-4" aria-hidden="true" />
            </Button>
          </div>
        </div>
      </section>
    </div>
    </AppShell>
  )
}

import { useMutation, useQuery } from '@apollo/client/react'
import { createFileRoute } from '@tanstack/react-router'
import { useEffect, useState } from 'react'
import { Search, Send, ChevronLeft, Phone, Video, Plus } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import AppShell from '@/components/AppShell'
import {
  ConversationsDocument,
  MarkConversationReadDocument,
  MessagesDocument,
  SendMessageDocument,
  MeDocument,
} from '@/generated/graphql'

export const Route = createFileRoute('/messages')({
  component: Messages,
})

export function Messages() {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data: meData } = useQuery(MeDocument)
  const { data: conversationsData, loading: conversationsLoading } = useQuery(ConversationsDocument)
  const conversations = conversationsData?.conversations ?? []

  const activeId = selectedId ?? conversations[0]?.id ?? null
  const { data: messagesData, loading: messagesLoading } = useQuery(MessagesDocument, {
    variables: { conversationId: activeId ?? '', first: 50 },
    skip: !activeId,
  })
  const messages = messagesData?.messages ?? []

  const [sendMessage] = useMutation(SendMessageDocument, {
    refetchQueries: [MessagesDocument],
  })
  const [markRead] = useMutation(MarkConversationReadDocument)

  useEffect(() => {
    if (!activeId) return
    void markRead({ variables: { id: activeId } })
  }, [activeId, markRead])

  async function send() {
    const body = draft.trim()
    if (!body || !activeId || sending) return

    setSending(true)
    setError(null)
    try {
      await sendMessage({ variables: { conversationId: activeId, body } })
      setDraft('')
    } catch {
      setError('Could not send that message.')
    } finally {
      setSending(false)
    }
  }

  return (
    <AppShell>
      <div className="page-wrap px-4 pb-8 pt-6">
        <div className="island-shell rise-in flex h-[70vh] min-h-[520px] overflow-hidden rounded-xl">
          <div className="flex w-full flex-col border-r border-[var(--line)] sm:w-72 lg:w-80">
            <div className="border-b border-[var(--line)] px-4 py-4">
              <h1 className="m-0 text-lg font-bold text-[var(--sea-ink)]">Messages</h1>
              <div className="relative mt-3">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-[var(--sea-ink-soft)]" aria-hidden="true" />
                <Input
                  type="search"
                  placeholder="Search conversations..."
                  aria-label="Search conversations"
                  className="w-full rounded-full border border-[var(--line)] bg-white/80 py-1.5 pl-8 pr-3 text-xs text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none focus:border-[var(--brand)]"
                />
              </div>
            </div>

            <ul className="flex-1 overflow-y-auto" aria-label="Conversations" aria-busy={conversationsLoading}>
              {conversationsLoading && <li className="m-3 h-20 animate-pulse rounded-xl" />}
              {!conversationsLoading && conversations.length === 0 && (
                <li className="px-4 py-6 text-center text-xs text-[var(--sea-ink-soft)]">No conversations yet.</li>
              )}
              {conversations.map((conversation, i) => (
                <li key={conversation.id} className="border-b border-[var(--line)]">
                  <button
                    type="button"
                    onClick={() => setSelectedId(conversation.id)}
                    aria-current={conversation.id === activeId ? 'true' : undefined}
                    className={`flex w-full items-center gap-3 px-3 py-3 text-left transition ${conversation.id === activeId ? 'bg-[var(--brand-light)]/70' : 'hover:bg-[var(--brand-light)]/40'}`}
                  >
                    <Avatar className="h-8 w-8" aria-label={conversation.title ?? 'Conversation'}>
                      <AvatarFallback>{(conversation.title ?? 'C').slice(0, 2).toUpperCase()}</AvatarFallback>
                    </Avatar>
                    <span className="flex-1 min-w-0">
                      <span className="block truncate text-sm font-semibold text-[var(--sea-ink)]">
                        {conversation.title ?? (conversation.type === 'direct' ? 'Direct message' : 'Group')}
                      </span>
                      <span className="block truncate text-xs text-[var(--sea-ink-soft)]">
                        {conversation.updatedAt}
                      </span>
                    </span>
                    {i === 0 && <span className="h-2 w-2 flex-shrink-0 rounded-full bg-[var(--brand)]" aria-hidden="true" />}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div className="hidden flex-1 flex-col sm:flex">
              <div className="flex items-center gap-2 border-b border-[var(--line)] px-4 py-3">
              <ChevronLeft className="h-5 w-5 text-[var(--sea-ink-soft)] sm:hidden" aria-hidden="true" />
              <div className="flex-1">
                <p className="m-0 text-sm font-semibold text-[var(--sea-ink)]">
                  {conversations.find((conversation) => conversation.id === activeId)?.title ?? 'Select a conversation'}
                </p>
                <p className="m-0 text-[0.7rem] text-[var(--sea-ink-soft)]">Direct message</p>
              </div>
                <Button variant="ghost" size="icon-sm" aria-label="Start voice call"><Phone /></Button>
                <Button variant="ghost" size="icon-sm" aria-label="Start video call"><Video /></Button>
                <Button variant="ghost" size="icon-sm" aria-label="Conversation options"><Plus /></Button>
            </div>

            <div className="flex-1 space-y-3 overflow-y-auto bg-[var(--foam)]/40 p-4" aria-busy={messagesLoading}>
              {!activeId && <p className="text-center text-sm text-[var(--sea-ink-soft)]">Choose a conversation to start reading.</p>}
              {messagesLoading && <div className="h-20 animate-pulse rounded-xl" />}
              {!messagesLoading && activeId && messages.length === 0 && (
                <p className="text-center text-sm text-[var(--sea-ink-soft)]">No messages yet.</p>
              )}
              {messages.map((message) => (
                <div key={message.id} className={`flex ${message.senderId === meData?.me.id ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[75%] rounded-xl px-3 py-2 text-sm ${message.senderId === meData?.me.id ? 'bg-[var(--brand)] text-white' : 'border border-[var(--line)] bg-white text-[var(--sea-ink)]'}`}>
                    <p className="m-0 leading-6">{message.body}</p>
                    <p className={`m-0 mt-0.5 text-right text-[0.65rem] ${message.senderId === meData?.me.id ? 'text-white/75' : 'text-[var(--sea-ink-soft)]'}`}>{message.createdAt}</p>
                  </div>
                </div>
              ))}
            </div>

            {error && (
              <p className="m-0 px-4 pb-2 text-sm text-[#9f3030]" role="alert">
                {error}
              </p>
            )}

            <div className="border-t border-[var(--line)] p-3">
              <div className="relative flex items-center gap-2">
                <Input
                  type="text"
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') void send()
                  }}
                  placeholder="Type a message..."
                  aria-label="Type a message"
                  disabled={!activeId}
                  className="flex-1 rounded-full border border-[var(--line)] bg-white py-2 pl-4 pr-10 text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none focus:border-[var(--brand)] disabled:opacity-50"
                />
                <Button
                  type="button"
                  onClick={() => void send()}
                  disabled={!activeId || !draft.trim() || sending}
                  aria-label="Send message"
                  variant="default"
                  size="icon"
                  className="absolute right-1.5 h-7 w-7 rounded-full bg-[var(--brand)] text-white transition hover:bg-[var(--brand-dark)] disabled:opacity-50"
                >
                  <Send className="h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  )
}

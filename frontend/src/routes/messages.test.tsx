import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import {
  ConversationsDocument,
  MessagesDocument,
  MarkConversationReadDocument,
  SendMessageDocument,
} from '../generated/graphql'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/messages' }),
    useParams: () => ({ username: 'rojame' }),
    useNavigate: () => navigate,
    Link: ({
      to,
      params,
      children,
      activeProps: _activeProps,
      ...rest
    }: {
      to: string
      params?: Record<string, string>
      children: ReactNode
      activeProps?: unknown
      [key: string]: unknown
    }) => {
      let href = String(to)
      for (const [key, value] of Object.entries(params ?? {})) {
        href = href.replace(`$${key}`, String(value)).replace(`:${key}`, String(value))
      }
      return <a href={href} {...rest}>{children}</a>
    },
  }
})

import { Messages } from './messages'

const mocks = [
  {
    request: { query: ConversationsDocument, variables: { first: 50 } },
    result: {
      data: {
        conversations: [
          { __typename: 'Conversation', id: 'c1', type: 'direct', title: 'Sokha', updatedAt: '2026-11-21T10:00:00Z' },
        ],
      },
    },
  },
  {
    request: { query: MessagesDocument, variables: { conversationId: 'c1', first: 50 } },
    result: {
      data: {
        messages: [
          {
            __typename: 'Message',
            id: 'm1',
            conversationId: 'c1',
            senderId: '2',
            type: 'TEXT',
            body: 'Hey! How are you?',
            createdAt: '2026-11-21T10:00:00Z',
            media: [],
          },
        ],
      },
    },
  },
  {
    request: { query: MarkConversationReadDocument, variables: { id: 'c1' } },
    result: { data: { markConversationRead: true } },
  },
  {
    request: { query: SendMessageDocument, variables: { conversationId: 'c1', body: 'On my way' } },
    result: {
      data: {
        sendMessage: {
          __typename: 'Message',
          id: 'm2',
          conversationId: 'c1',
          senderId: '1',
          body: 'On my way',
          createdAt: '2026-11-21T10:05:00Z',
        },
      },
    },
  },
]

function renderScreen(ui: ReactNode) {
  return render(<MockedProvider mocks={mocks}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
})

describe('Messages screen', () => {
  it('renders the two-pane layout with list and thread', async () => {
    renderScreen(<Messages />)

    expect(screen.getByRole('heading', { name: 'Messages' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Search conversations...')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Type a message...')).toBeInTheDocument()
    expect(await screen.findByText('Hey! How are you?')).toBeInTheDocument()
  })

  it('lists conversations from the backend', async () => {
    renderScreen(<Messages />)

    const list = await screen.findByRole('list', { name: 'Conversations' })
    expect(await within(list).findByText('Sokha')).toBeInTheDocument()
  })

  it('sends a message to the selected conversation', async () => {
    const user = userEvent.setup()
    renderScreen(<Messages />)

    const input = await screen.findByLabelText('Type a message')
    await user.type(input, 'On my way')
    await user.click(screen.getByRole('button', { name: 'Send message' }))

    expect(input).toHaveValue('')
  })
})
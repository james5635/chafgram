import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ComponentProps, ReactNode } from 'react'
import {
  ConversationsDocument,
  MessagesDocument,
  MarkConversationReadDocument,
  SendMessageDocument,
  MeDocument,
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

const MEMBER = {
  __typename: 'Member' as const,
  id: '1',
  username: 'rojame',
  displayName: 'Rojame',
  bio: 'Software Engineer',
  avatarMediaId: null,
  role: 'MEMBER',
  status: 'ACTIVE',
  followerCount: 128,
  followingCount: 320,
  isFollowing: false,
}

const ME_MOCK = {
  request: { query: MeDocument },
  result: { data: { me: MEMBER } },
}

const CONVERSATIONS_MOCK = {
  request: { query: ConversationsDocument, variables: { first: 50 } },
  result: {
    data: {
      conversations: [
        { __typename: 'Conversation', id: 'c1', type: 'direct', title: 'Sokha', updatedAt: '2026-11-21T10:00:00Z' },
      ],
    },
  },
}

const MESSAGES_MOCK = {
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
}

const MARK_READ_MOCK = {
  request: { query: MarkConversationReadDocument, variables: { id: 'c1' } },
  result: { data: { markConversationRead: true } },
}

const SEND_RESULT = {
  data: {
    sendMessage: {
      __typename: 'Message' as const,
      id: 'm2',
      conversationId: 'c1',
      senderId: '1',
      body: 'On my way',
      createdAt: '2026-11-21T10:05:00Z',
    },
  },
}

const SEND_MOCK = {
  request: { query: SendMessageDocument, variables: { conversationId: 'c1', body: 'On my way' } },
  result: SEND_RESULT,
}

const CONVERSATIONS_WITH_SECOND_MOCK = {
  request: { query: ConversationsDocument, variables: { first: 50 } },
  result: {
    data: {
      conversations: [
        { __typename: 'Conversation', id: 'c1', type: 'direct', title: 'Sokha', updatedAt: '2026-11-21T10:00:00Z' },
        { __typename: 'Conversation', id: 'c2', type: 'direct', title: 'Dara', updatedAt: '2026-11-21T11:00:00Z' },
      ],
    },
  },
}

const MESSAGES_C2_MOCK = {
  request: { query: MessagesDocument, variables: { conversationId: 'c2', first: 50 } },
  result: {
    data: {
      messages: [
        {
          __typename: 'Message',
          id: 'm3',
          conversationId: 'c2',
          senderId: '2',
          type: 'TEXT',
          body: 'Hi from Dara',
          createdAt: '2026-11-21T11:00:00Z',
          media: [],
        },
      ],
    },
  },
}

const MARK_READ_C2_MOCK = {
  request: { query: MarkConversationReadDocument, variables: { id: 'c2' } },
  result: { data: { markConversationRead: true } },
}

function mocks() {
  return [ME_MOCK, CONVERSATIONS_MOCK, MESSAGES_MOCK, MARK_READ_MOCK, SEND_MOCK]
}

type ScreenMocks = ComponentProps<typeof MockedProvider>['mocks']

function renderScreen(ui: ReactNode, screenMocks: ScreenMocks = mocks()) {
  return render(<MockedProvider mocks={screenMocks}>{ui}</MockedProvider>)
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

  it('shows Sending… while a send is pending and ignores a second rapid click', async () => {
    const sendRequest = vi.fn(() => true)
    const slowMocks = [
      ME_MOCK,
      CONVERSATIONS_MOCK,
      MESSAGES_MOCK,
      MARK_READ_MOCK,
      {
        request: { query: SendMessageDocument, variables: sendRequest },
        result: SEND_RESULT,
        delay: Infinity,
        maxUsageCount: 5,
      },
    ]
    const user = userEvent.setup()
    renderScreen(<Messages />, slowMocks)

    await screen.findByText('Hey! How are you?')
    const input = screen.getByLabelText('Type a message')
    await user.type(input, 'On my way')
    const sendButton = screen.getByRole('button', { name: 'Send message' })
    await waitFor(() => expect(sendButton).toBeEnabled())
    fireEvent.click(sendButton)
    fireEvent.click(sendButton)

    await waitFor(() => expect(sendRequest).toHaveBeenCalledTimes(1))
    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Sending…')
    const region = sendButton.closest('[aria-busy="true"]')
    expect(region).toHaveClass('relative')
    expect(region).not.toContainElement(status)
    expect(status.closest('[aria-busy]')).toBeNull()
    expect(status.closest('[inert]')).toBeNull()
    const inertContent = sendButton.closest('[inert]')
    expect(inertContent).not.toBeNull()
    expect(inertContent).toContainElement(input)
    expect(inertContent).not.toContainElement(status)
    expect(sendButton).toBeDisabled()
    expect(input).toBeDisabled()
  })

  it('does not show an overlay when switching conversations marks one read', async () => {
    const markReadRequest = vi.fn(() => true)
    const user = userEvent.setup()
    renderScreen(<Messages />, [
      ME_MOCK,
      CONVERSATIONS_WITH_SECOND_MOCK,
      MESSAGES_MOCK,
      MARK_READ_MOCK,
      MESSAGES_C2_MOCK,
      {
        ...MARK_READ_C2_MOCK,
        request: { query: MarkConversationReadDocument, variables: markReadRequest },
        delay: Infinity,
        maxUsageCount: 5,
      },
    ])

    const list = await screen.findByRole('list', { name: 'Conversations' })
    await user.click(await within(list).findByRole('button', { name: /Dara/ }))

    expect(await screen.findByText('Hi from Dara')).toBeInTheDocument()
    await waitFor(() => expect(markReadRequest).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })
})

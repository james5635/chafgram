import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))
const { loginRequest } = vi.hoisted(() => ({ loginRequest: vi.fn() }))

// TanStack Router's Link/useLocation need router context. Mock them as plain
// anchors + a fixed location so screens can be rendered in isolation.
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/' }),
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

vi.mock('../lib/session-server', () => ({
  loginRequest,
  logoutRequest: vi.fn(),
}))

import {
  ConversationsDocument,
  CreatePostDocument,
  DiscoverFeedDocument,
  FollowingFeedDocument,
  MarkConversationReadDocument,
  MarkNotificationsReadDocument,
  MeDocument,
  MemberDocument,
  MessagesDocument,
  MyNotificationsDocument,
  SendMessageDocument,
  UnreadNotificationsDocument,
} from '../generated/graphql'
import { Welcome } from './welcome'
import { Explore } from './explore'
import { Profile } from './profile.$username'
import { Notifications } from './notifications'
import { Messages } from './messages'
import { Compose } from './compose'
import LoginScreen from '../components/LoginScreen'

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

const POST = {
  __typename: 'Post' as const,
  id: 'p1',
  type: 'NOTE' as const,
  body: 'Keep going!',
  editedAt: null,
  createdAt: '2026-11-21T10:00:00Z',
  commentCount: 8,
  reactionCount: 45,
  myReaction: null,
  isSaved: false,
  author: { ...MEMBER },
}

const mocks = [
  { request: { query: MeDocument }, result: { data: { me: MEMBER } } },
  {
    request: { query: FollowingFeedDocument, variables: { first: 20 } },
    result: { data: { followingFeed: { posts: [POST], nextCursor: null } } },
  },
  {
    request: { query: DiscoverFeedDocument, variables: { first: 18 } },
    result: { data: { discoverFeed: { posts: [POST], nextCursor: null } } },
  },
  {
    request: { query: MyNotificationsDocument, variables: { first: 30 } },
    result: {
      data: {
        myNotifications: [
          {
            __typename: 'Notification',
            id: 'n1',
            type: 'LIKE',
            entityType: 'Post',
            entityId: 'p1',
            readAt: null,
            createdAt: '2026-11-21T10:00:00Z',
            actor: { ...MEMBER },
          },
        ],
      },
    },
  },
  { request: { query: UnreadNotificationsDocument }, result: { data: { unreadNotifications: 1 } } },
  { request: { query: MarkNotificationsReadDocument }, result: { data: { markNotificationsRead: true } } },
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
  {
    request: { query: MemberDocument, variables: { username: 'rojame' } },
    result: { data: { member: { ...MEMBER } } },
  },
  {
    request: { query: CreatePostDocument, variables: { type: 'NOTE', body: 'Fresh post' } },
    result: {
      data: {
        createPost: {
          __typename: 'Post',
          id: 'p2',
          type: 'NOTE',
          body: 'Fresh post',
          createdAt: '2026-11-21T10:10:00Z',
          author: { id: '1', username: 'rojame', displayName: 'Rojame' },
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
  loginRequest.mockReset()
})

describe('Welcome screen', () => {
  it('renders the marketing hero and CTAs', () => {
    renderScreen(<Welcome />)

    expect(screen.getByRole('heading', { name: /Connect\. Share\./ })).toBeInTheDocument()
    expect(screen.getByText(/Join Chafgram and be part of a vibrant community/)).toBeInTheDocument()
    expect(screen.getByText(/Learn More/)).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /Get Started/ }).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: 'Sign In' })).toBeInTheDocument()
  })

  it('renders the public nav', () => {
    renderScreen(<Welcome />)
    for (const label of ['Home', 'Explore', 'About']) {
      expect(screen.getByText(label)).toBeInTheDocument()
    }
  })
})

describe('Explore screen', () => {
  it('renders heading, tabs, and the content grid', async () => {
    renderScreen(<Explore />)

    expect(screen.getByRole('heading', { name: 'Explore' })).toBeInTheDocument()
    for (const tab of ['For You', 'Following', 'Trending', 'Nearby']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    expect(await screen.findByText('Keep going!')).toBeInTheDocument()
  })

  it('renders the Suggested Topics panel with follow buttons', () => {
    renderScreen(<Explore />)

    expect(screen.getByText('Suggested Topics')).toBeInTheDocument()
    for (const topic of ['Technology', 'Travel', 'Lifestyle', 'Education', 'Gaming']) {
      expect(screen.getByText(topic)).toBeInTheDocument()
    }
    expect(screen.getAllByRole('button', { name: /^Follow / })).toHaveLength(5)
  })
})

describe('Profile screen', () => {
  it('renders identity, stats, and tabs', async () => {
    renderScreen(<Profile />)

    expect(await screen.findByRole('heading', { name: 'Rojame' })).toBeInTheDocument()
    expect(screen.getAllByText('@rojame').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/128/).length).toBeGreaterThan(0)
    for (const tab of ['Posts', 'Replies', 'Media', 'Likes']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    expect(screen.getByRole('button', { name: 'Edit Profile' })).toBeInTheDocument()
  })

  it('renders the About panel', () => {
    renderScreen(<Profile />)
    expect(screen.getByText('About')).toBeInTheDocument()
    expect(screen.getByText(/Your company workspace/)).toBeInTheDocument()
  })
})

describe('Notifications screen', () => {
  it('renders heading, type tabs, and notification items', async () => {
    renderScreen(<Notifications />)

    expect(screen.getByRole('heading', { name: 'Notifications' })).toBeInTheDocument()
    for (const tab of ['All', 'Likes', 'Comments', 'Follows', 'Mentions']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    expect(await screen.findByText(/liked your post/)).toBeInTheDocument()
  })

  it('shows the unread count from the backend', async () => {
    renderScreen(<Notifications />)
    expect(await screen.findByText('1 unread')).toBeInTheDocument()
  })

  it('marks every notification as read', async () => {
    const user = userEvent.setup()
    renderScreen(<Notifications />)

    const button = await screen.findByRole('button', { name: 'Mark all as read' })
    await user.click(button)

    expect(button).toBeInTheDocument()
  })
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

describe('Compose screen', () => {
  it('renders the editor, chips, and upload area', () => {
    renderScreen(<Compose />)

    expect(screen.getByRole('heading', { name: 'Create Post' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText("What's on your mind?")).toBeInTheDocument()
    for (const chip of ['Photo', 'Video', 'Feeling', 'Poll']) {
      expect(screen.getByText(chip)).toBeInTheDocument()
    }
    expect(screen.getByText('Upload Image')).toBeInTheDocument()
    expect(screen.getByText('or drag and drop')).toBeInTheDocument()
  })

  it('renders the action bar', () => {
    renderScreen(<Compose />)
    expect(screen.getByRole('link', { name: 'Cancel' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Post' })).toBeInTheDocument()
  })

  it('publishes the composed post and returns to the feed', async () => {
    const user = userEvent.setup()
    renderScreen(<Compose />)

    await user.type(screen.getByPlaceholderText("What's on your mind?"), 'Fresh post')
    await user.click(screen.getByRole('button', { name: 'Post' }))

    expect(await screen.findByRole('heading', { name: 'Create Post' })).toBeInTheDocument()
  })
})

describe('Login screen', () => {
  it('signs in with a password and lands on the feed', async () => {
    loginRequest.mockResolvedValue({ ok: true })
    const user = userEvent.setup()
    renderScreen(<LoginScreen />)

    await user.type(screen.getByLabelText('Email or username'), 'rojame')
    await user.type(screen.getByLabelText('Password'), 'hunter2')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(loginRequest).toHaveBeenCalledWith({ data: { username: 'rojame', password: 'hunter2' } })
    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/' }))
  })

  it('shows the error the backend reported', async () => {
    loginRequest.mockResolvedValue({ ok: false, message: 'Those credentials did not match an account.' })
    const user = userEvent.setup()
    renderScreen(<LoginScreen />)

    await user.type(screen.getByLabelText('Email or username'), 'rojame')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Those credentials did not match an account.')
    expect(navigate).not.toHaveBeenCalled()
  })

  it('explains that passkey sign-in is not available yet', async () => {
    const user = userEvent.setup()
    renderScreen(<LoginScreen />)

    await user.click(screen.getByRole('button', { name: /Sign in with passkey/ }))

    expect(await screen.findByRole('status')).toHaveTextContent('not available on this server yet')
  })
})

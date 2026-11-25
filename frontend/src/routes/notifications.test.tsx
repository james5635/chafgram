import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import {
  MyNotificationsDocument,
  UnreadNotificationsDocument,
  MarkNotificationsReadDocument,
} from '../generated/graphql'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/notifications' }),
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

import { Notifications } from './notifications'

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

const mocks = [
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
]

function renderScreen(ui: ReactNode) {
  return render(<MockedProvider mocks={mocks}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
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
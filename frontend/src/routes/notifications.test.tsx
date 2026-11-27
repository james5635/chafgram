import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ComponentProps, ReactNode } from 'react'
import {
  MyNotificationsDocument,
  UnreadNotificationsDocument,
  MarkNotificationsReadDocument,
  MeDocument,
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
  { request: { query: MeDocument }, result: { data: { me: MEMBER } } },
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

type ScreenMocks = ComponentProps<typeof MockedProvider>['mocks']

function renderScreen(ui: ReactNode, screenMocks: ScreenMocks = mocks) {
  return render(<MockedProvider mocks={screenMocks}>{ui}</MockedProvider>)
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

  it('shows Updating… while marking all read and ignores a second rapid click', async () => {
    const markAllRequest = vi.fn(() => true)
    const slowMocks = [
      mocks[0],
      mocks[1],
      mocks[2],
      {
        request: { query: MarkNotificationsReadDocument, variables: markAllRequest },
        result: { data: { markNotificationsRead: true } },
        delay: Infinity,
        maxUsageCount: 5,
      },
    ]
    renderScreen(<Notifications />, slowMocks)

    await screen.findByText('1 unread')
    const button = screen.getByRole('button', { name: 'Mark all as read' })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    fireEvent.click(button)

    await waitFor(() => expect(markAllRequest).toHaveBeenCalledTimes(1))
    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Updating…')
    const region = button.closest('[aria-busy="true"]')
    expect(region).toHaveClass('relative')
    expect(status.parentElement).toBe(region)
    const inertContent = button.closest('[inert]')
    expect(inertContent).not.toBeNull()
    expect(inertContent).toContainElement(button)
    expect(inertContent).not.toContainElement(status)
    expect(button).toBeDisabled()
  })
})
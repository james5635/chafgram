import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { DiscoverFeedDocument } from '../generated/graphql'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/explore' }),
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

import { Explore } from './explore'

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
  {
    request: { query: DiscoverFeedDocument, variables: { first: 18 } },
    result: { data: { discoverFeed: { posts: [POST], nextCursor: null } } },
  },
]

function renderScreen(ui: ReactNode) {
  return render(<MockedProvider mocks={mocks}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
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
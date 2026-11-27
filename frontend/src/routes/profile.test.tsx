import { MockedProvider } from '@apollo/client/testing/react'
import type { MockedResponse } from '@apollo/client/testing'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemberDocument, MeDocument, MemberPostsDocument } from '../generated/graphql'

const { navigate, params } = vi.hoisted(() => ({
  navigate: vi.fn(),
  params: { username: 'rojame' },
}))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/profile/rojame' }),
    useParams: () => params,
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

import { Profile } from './profile.$username'

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
  createdAt: '2024-03-01T00:00:00Z',
}

const MEMBER_POST = {
  __typename: 'Post' as const,
  id: 'post-1',
  type: 'NOTE',
  body: 'My profile post',
  editedAt: null,
  createdAt: '2026-11-27T10:00:00Z',
  commentCount: 0,
  reactionCount: 0,
  myReaction: null,
  isSaved: false,
  mediaIds: [],
  mediaUrls: [],
  author: {
    __typename: 'Member' as const,
    id: '1',
    username: 'rojame',
    displayName: 'Rojame',
    avatarMediaId: null,
  },
}

const memberMock = {
  request: { query: MemberDocument, variables: { username: 'rojame' } },
  result: { data: { member: { ...MEMBER } } },
}
const meMock = { request: { query: MeDocument }, result: { data: { me: { ...MEMBER } } } }

function postsMock(result: unknown) {
  return {
    request: { query: MemberPostsDocument, variables: { username: 'rojame', first: 20 } },
    result: { data: { memberPosts: result } },
  }
}

const mocks = [memberMock, meMock, postsMock([MEMBER_POST])]

function renderScreen(ui: ReactNode, screenMocks: ReadonlyArray<MockedResponse> = mocks) {
  return render(<MockedProvider mocks={screenMocks}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
  params.username = 'rojame'
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
    expect(screen.getByText(/Phnom Penh, Cambodia/)).toBeInTheDocument()
    expect(screen.getByText(/github.com\/james5635/)).toBeInTheDocument()
    expect(screen.getByText(/Joined March 2024/)).toBeInTheDocument()
  })

  it('shows the members posts on the Posts tab', async () => {
    renderScreen(<Profile />)

    expect(await screen.findByText('My profile post')).toBeInTheDocument()
  })

  it('queries by resolved username on the me route', async () => {
    params.username = 'me'
    renderScreen(<Profile />, [meMock, postsMock([MEMBER_POST])])

    expect(await screen.findByText('My profile post')).toBeInTheDocument()
  })

  it('renders posts as a full-width list outside the profile card', async () => {
    renderScreen(<Profile />)
    const body = await screen.findByText('My profile post')
    const profileCard = screen.getByRole('heading', { name: 'Rojame' }).closest('.island-shell')

    expect(profileCard).not.toContainElement(body)
    expect(body.closest('.space-y-6')).not.toBeNull()
  })

  it('shows an empty state when the member has no posts', async () => {
    renderScreen(<Profile />, [memberMock, postsMock([])])

    expect(await screen.findByText('No posts yet.')).toBeInTheDocument()
  })

  it('does not show the empty state while the member query is still loading', () => {
    const { container } = renderScreen(<Profile />, [
      { ...memberMock, delay: 5000 },
      postsMock([MEMBER_POST]),
    ])

    expect(screen.queryByText('No posts yet.')).not.toBeInTheDocument()
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
  })

  it('shows an error state when the query fails', async () => {
    renderScreen(<Profile />, [
      memberMock,
      {
        request: { query: MemberPostsDocument, variables: { username: 'rojame', first: 20 } },
        error: new Error('Network error'),
      },
    ])

    expect(await screen.findByText(/Could not load posts/)).toBeInTheDocument()
  })
})
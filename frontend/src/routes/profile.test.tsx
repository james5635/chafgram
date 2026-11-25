import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemberDocument, MeDocument } from '../generated/graphql'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/profile/rojame' }),
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

const mocks = [
  {
    request: { query: MemberDocument, variables: { username: 'rojame' } },
    result: { data: { member: { ...MEMBER } } },
  },
  { request: { query: MeDocument }, result: { data: { me: { ...MEMBER } } } },
]

function renderScreen(ui: ReactNode) {
  return render(<MockedProvider mocks={mocks}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
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
})
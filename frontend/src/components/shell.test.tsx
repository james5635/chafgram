import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MockedProvider } from '@apollo/client/testing/react'
import { CombinedGraphQLErrors } from '@apollo/client/errors'
import type { ReactNode } from 'react'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useLocation: () => ({ pathname: '/' }),
    useNavigate: () => navigate,
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

import LeftRail from './LeftRail'
import AppShell from './AppShell'
import { MeDocument } from '../generated/graphql'

const MEMBER = {
  __typename: 'Member' as const,
  id: '1',
  username: 'rojame',
  displayName: 'Rojame',
  bio: null,
  avatarMediaId: null,
  role: 'MEMBER',
  status: 'ACTIVE',
  followerCount: 1,
  followingCount: 2,
  isFollowing: false,
}

const sessionMocks = [
  { request: { query: MeDocument }, result: { data: { me: MEMBER } } },
]

function renderShell(ui: ReactNode) {
  return render(<MockedProvider mocks={sessionMocks}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
})

describe('LeftRail', () => {
  it('renders the navigation rail and all nav links', () => {
    render(<LeftRail />)

    expect(screen.getByRole('complementary', { name: 'Main navigation' })).toBeInTheDocument()
    for (const label of ['Home', 'Explore', 'Messages', 'Notifications', 'Profile']) {
      expect(screen.getByText(label)).toBeInTheDocument()
    }
  })

  it('shows unread badges for messages and notifications', () => {
    render(<LeftRail />)

    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  it('renders the Create Post action and the user chip', () => {
    render(<LeftRail />)

    expect(screen.getByRole('button', { name: 'Create post' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Your profile' })).toBeInTheDocument()
    expect(screen.getByText('Rojame')).toBeInTheDocument()
    expect(screen.getByText('@rojame')).toBeInTheDocument()
  })
})

describe('AppShell', () => {
  it('renders children alongside the rail and optional right rail', () => {
    renderShell(
      <AppShell rightRail={<aside data-testid="right-rail">Right</aside>}>
        <p data-testid="content">Content</p>
      </AppShell>
    )

    expect(screen.getByTestId('content')).toBeInTheDocument()
    expect(screen.getByTestId('right-rail')).toBeInTheDocument()
    expect(screen.getByRole('complementary', { name: 'Main navigation' })).toBeInTheDocument()
  })

  it('works without a right rail', () => {
    renderShell(
      <AppShell>
        <p data-testid="content">Only content</p>
      </AppShell>
    )

    expect(screen.getByTestId('content')).toBeInTheDocument()
    expect(screen.queryByTestId('right-rail')).not.toBeInTheDocument()
  })

  it('sends the visitor to the sign-in screen when the session is missing', async () => {
    render(
      <MockedProvider
        mocks={[
          {
            request: { query: MeDocument },
            error: new CombinedGraphQLErrors({
              data: null,
              errors: [{ message: 'Unauthorized', extensions: { classification: 'UNAUTHORIZED' } }],
            }),
          },
        ]}
      >
        <AppShell>
          <p data-testid="content">Content</p>
        </AppShell>
      </MockedProvider>
    )

    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/login' }))
  })
})
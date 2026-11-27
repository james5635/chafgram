import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
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
import { RailProvider, useRail } from '../hooks/useRail'
import { MeDocument } from '../generated/graphql'

function CollapseControl() {
  const { toggle } = useRail()
  return <button type="button" onClick={toggle}>Toggle rail</button>
}

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
  return render(
    <RailProvider>
      <MockedProvider mocks={sessionMocks}>
        <div>
          {ui}
          <CollapseControl />
        </div>
      </MockedProvider>
    </RailProvider>,
  )
}

beforeEach(() => {
  navigate.mockClear()
  localStorage.removeItem('rail-collapsed')
})

describe('LeftRail', () => {
  it('renders the navigation rail and all nav links', () => {
    renderShell(<LeftRail />)

    expect(screen.getByRole('complementary', { name: 'Main navigation' })).toBeInTheDocument()
    for (const label of ['Home', 'Explore', 'Messages', 'Notifications', 'Profile']) {
      expect(screen.getByText(label)).toBeInTheDocument()
    }
  })

  it('collapses the rail to labelled icons', async () => {
    renderShell(<LeftRail />)

    fireEvent.click(screen.getByRole('button', { name: 'Toggle rail' }))

    const rail = screen.getByRole('complementary', { name: 'Main navigation' })
    expect(rail).toHaveClass('w-16')
    expect(rail).not.toHaveClass('w-64')

    for (const label of ['Home', 'Explore', 'Messages', 'Notifications', 'Profile']) {
      const link = screen.getByRole('link', { name: label })
      expect(link).toBeInTheDocument()
      expect(link).not.toHaveTextContent(label)
    }
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create post' })).toBeInTheDocument()
    expect(screen.queryByText('Create Post')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Account menu' })).toBeInTheDocument()
  })

  it('shows unread badges for messages and notifications', () => {
    renderShell(<LeftRail />)

    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  it('renders the Create Post action and the user chip', async () => {
    renderShell(<LeftRail />)

    expect(screen.getByRole('button', { name: 'Create post' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Account menu' })).toBeInTheDocument()
    expect(await screen.findByText('Rojame')).toBeInTheDocument()
    expect(screen.getByText('@rojame')).toBeInTheDocument()
  })

  it('opens the account menu instead of showing a More or profile link', () => {
    renderShell(<LeftRail />)

    expect(screen.queryByRole('link', { name: 'More' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Your profile' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Profile' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    expect(screen.getByRole('link', { name: /Settings/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Sign out/ })).toBeInTheDocument()
  })

  it('shows the signed-in member instead of a hardcoded name', async () => {
    render(
      <MockedProvider
        mocks={[
          {
            request: { query: MeDocument },
            result: {
              data: { me: { ...MEMBER, displayName: 'Dev Admin', username: 'devadmin' } },
            },
          },
        ]}
      >
        <LeftRail />
      </MockedProvider>,
    )

    expect(await screen.findByText('Dev Admin')).toBeInTheDocument()
    expect(screen.getByText('@devadmin')).toBeInTheDocument()
    expect(screen.getByLabelText('Dev Admin')).toBeInTheDocument()
    expect(screen.queryByText('Rojame')).not.toBeInTheDocument()
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

  it('shrinks the content offset when the rail collapses', () => {
    const { container } = renderShell(
      <AppShell>
        <p data-testid="content">Content</p>
      </AppShell>,
    )
    const offset = screen.getByTestId('content').parentElement!.parentElement!

    expect(offset).toHaveClass('lg:pl-64')

    fireEvent.click(screen.getByRole('button', { name: 'Toggle rail' }))

    expect(offset).toHaveClass('lg:pl-16')
    expect(offset).not.toHaveClass('lg:pl-64')
    expect(container).toBeInTheDocument()
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
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useLocation: () => ({ pathname: '/' }),
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

import LeftRail from './LeftRail'
import AppShell from './AppShell'

describe('LeftRail', () => {
  it('renders the brand and all nav links', () => {
    render(<LeftRail />)

    expect(screen.getByText('Chafgram')).toBeInTheDocument()
    for (const label of ['Home', 'Explore', 'Messages', 'Notifications', 'Profile', 'More']) {
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

    expect(screen.getByRole('link', { name: 'Create post' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Your profile' })).toBeInTheDocument()
    expect(screen.getByText('Rojame')).toBeInTheDocument()
    expect(screen.getByText('@rojame')).toBeInTheDocument()
  })
})

describe('AppShell', () => {
  it('renders children alongside the rail and optional right rail', () => {
    render(
      <AppShell rightRail={<aside data-testid="right-rail">Right</aside>}>
        <p data-testid="content">Content</p>
      </AppShell>
    )

    expect(screen.getByTestId('content')).toBeInTheDocument()
    expect(screen.getByTestId('right-rail')).toBeInTheDocument()
    expect(screen.getByText('Chafgram')).toBeInTheDocument()
  })

  it('works without a right rail', () => {
    render(
      <AppShell>
        <p data-testid="content">Only content</p>
      </AppShell>
    )

    expect(screen.getByTestId('content')).toBeInTheDocument()
    expect(screen.queryByTestId('right-rail')).not.toBeInTheDocument()
  })
})

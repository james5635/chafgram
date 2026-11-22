import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))

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

import { Welcome } from './welcome'

function renderScreen(ui: ReactNode) {
  return render(<MockedProvider mocks={[]}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
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
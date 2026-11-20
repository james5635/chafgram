import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'

// TanStack Router's Link/useLocation need router context. Mock them as plain
// anchors + a fixed location so screens can be rendered in isolation.
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/' }),
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
import { Explore } from './explore'
import { Profile } from './profile.$username'
import { Notifications } from './notifications'
import { Messages } from './messages'
import { Compose } from './compose'

describe('Welcome screen', () => {
  it('renders the marketing hero and CTAs', () => {
    render(<Welcome />)

    expect(screen.getByRole('heading', { name: /Connect\. Share\./ })).toBeInTheDocument()
    expect(screen.getByText(/Join Chafgram and be part of a vibrant community/)).toBeInTheDocument()
    expect(screen.getByText(/Learn More/)).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /Get Started/ }).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: 'Sign In' })).toBeInTheDocument()
  })

  it('renders the public nav', () => {
    render(<Welcome />)

    for (const label of ['Home', 'Explore', 'About']) {
      expect(screen.getByText(label)).toBeInTheDocument()
    }
  })
})

describe('Explore screen', () => {
  it('renders heading, tabs, and the content grid', () => {
    render(<Explore />)

    expect(screen.getByRole('heading', { name: 'Explore' })).toBeInTheDocument()
    for (const tab of ['For You', 'Following', 'Trending', 'Nearby']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    expect(screen.getByText('Better Together')).toBeInTheDocument()
    expect(screen.getByText('Good Morning ☀️')).toBeInTheDocument()
  })

  it('renders the Suggested Topics panel with follow buttons', () => {
    render(<Explore />)

    expect(screen.getByText('Suggested Topics')).toBeInTheDocument()
    for (const topic of ['Technology', 'Travel', 'Lifestyle', 'Education', 'Gaming']) {
      expect(screen.getByText(topic)).toBeInTheDocument()
    }
    expect(screen.getAllByRole('button', { name: /^Follow / })).toHaveLength(5)
  })
})

describe('Profile screen', () => {
  it('renders identity, stats, and tabs', () => {
    render(<Profile />)

    expect(screen.getByRole('heading', { name: 'Rojame' })).toBeInTheDocument()
    expect(screen.getAllByText('@rojame').length).toBeGreaterThan(0)
    expect(screen.getByText(/128/)).toBeInTheDocument()
    expect(screen.getByText(/2\.4K/)).toBeInTheDocument()
    for (const tab of ['Posts', 'Replies', 'Media', 'Likes']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    expect(screen.getByRole('button', { name: 'Edit Profile' })).toBeInTheDocument()
  })

  it('renders About and Skills panels', () => {
    render(<Profile />)

    expect(screen.getByText('About')).toBeInTheDocument()
    expect(screen.getByText(/Phnom Penh, Cambodia/)).toBeInTheDocument()
    expect(screen.getByText(/Joined March 2024/)).toBeInTheDocument()
    expect(screen.getByText('Skills')).toBeInTheDocument()
    for (const skill of ['Python', 'TypeScript', 'Java', 'React']) {
      expect(screen.getByText(new RegExp(`^${skill}`))).toBeInTheDocument()
    }
  })
})

describe('Notifications screen', () => {
  it('renders heading, type tabs, and notification items', () => {
    render(<Notifications />)

    expect(screen.getByRole('heading', { name: 'Notifications' })).toBeInTheDocument()
    for (const tab of ['All', 'Likes', 'Comments', 'Follows', 'Mentions']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    expect(screen.getAllByText(/Sokha/).length).toBeGreaterThan(0)
    expect(screen.getByText(/started following you/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Follow' })).toBeInTheDocument()
  })

  it('shows the unread count', () => {
    render(<Notifications />)
    expect(screen.getByText('2 unread')).toBeInTheDocument()
  })
})

describe('Messages screen', () => {
  it('renders the two-pane layout with list and thread', () => {
    render(<Messages />)

    expect(screen.getByRole('heading', { name: 'Messages' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Search conversations...')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Type a message...')).toBeInTheDocument()
    expect(screen.getAllByText(/Hey! How are you/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/I'm good! How about you/).length).toBeGreaterThan(0)
  })

  it('lists all conversations', () => {
    render(<Messages />)

    for (const name of ['Sokha', 'Dara Chan', 'Vichea', 'Mey Sokha']) {
      expect(screen.getAllByText(name).length).toBeGreaterThan(0)
    }
  })
})

describe('Compose screen', () => {
  it('renders the editor, chips, and upload area', () => {
    render(<Compose />)

    expect(screen.getByRole('heading', { name: 'Create Post' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText("What's on your mind?")).toBeInTheDocument()
    for (const chip of ['Photo', 'Video', 'Feeling', 'Poll']) {
      expect(screen.getByText(chip)).toBeInTheDocument()
    }
    expect(screen.getByText('Upload Image')).toBeInTheDocument()
    expect(screen.getByText('or drag and drop')).toBeInTheDocument()
  })

  it('renders the action bar', () => {
    render(<Compose />)

    expect(screen.getByRole('link', { name: 'Cancel' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Post' })).toBeInTheDocument()
  })

  it('accepts body text', () => {
    render(<Compose />)
    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    expect(textarea.value).toBe('')
  })
})

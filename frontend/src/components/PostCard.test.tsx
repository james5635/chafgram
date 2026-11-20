import { vi, describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import PostCard from './PostCard'

// TanStack Router's Link requires router context; mock it as a plain anchor
// so PostCard can be tested in isolation.
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ to, params, children }: Record<string, any>) => {
      let href = String(to)
      if (params) {
        for (const [key, value] of Object.entries(params)) {
          href = href.replace(`$${key}`, String(value)).replace(`:${key}`, String(value))
        }
      }
      return <a href={href}>{children}</a>
    },
  }
})

const post = {
  id: '1',
  author: { name: 'Dara', username: 'dara', initials: 'DR' },
  createdAt: '2h ago',
  body: 'A beautiful day to build something great! 🚀',
  media: [{ url: 'https://example.com/img.jpg', alt: 'Mountain landscape' }],
  reactions: { count: 248 },
  comments: 32,
  shares: 12,
}

describe('PostCard', () => {
  it('renders author, body, and engagement counts', () => {
    render(<PostCard post={post} />)

    expect(screen.getByText('Dara')).toBeInTheDocument()
    expect(
      screen.getByText('A beautiful day to build something great! 🚀')
    ).toBeInTheDocument()
    expect(screen.getByText('248')).toBeInTheDocument()
    expect(screen.getByText('32 comments')).toBeInTheDocument()
    expect(screen.getByText('12 shares')).toBeInTheDocument()
  })

  it('renders the media image with alt text', () => {
    render(<PostCard post={post} />)

    expect(screen.getByAltText('Mountain landscape')).toBeInTheDocument()
  })

  it('links the author name to their profile with interpolated username', () => {
    render(<PostCard post={post} />)

    const link = screen.getByRole('link')
    expect(link).toHaveAttribute('href', '/profile/dara')
  })

  it('renders Like, Comment, Share and Save actions', () => {
    render(<PostCard post={post} />)

    for (const label of ['Like', 'Comment', 'Share', 'Save']) {
      expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
    }
  })

  it('renders a post with no media', () => {
    render(
      <PostCard
        post={{
          ...post,
          id: '2',
          media: [],
        }}
      />
    )

    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText('A beautiful day to build something great! 🚀')).toBeInTheDocument()
  })
})

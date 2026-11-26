import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { ReactNode } from 'react'

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

import Header from './Header'

describe('Header', () => {
  it('shows the brand and search without a theme button or avatar', () => {
    const { container } = render(<Header />)

    expect(screen.getByRole('link', { name: 'Chafgram home' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Search Chafgram…')).toBeInTheDocument()
    expect(screen.queryByText('Auto')).not.toBeInTheDocument()
    expect(container.querySelector('button[title^="Theme mode"]')).toBeNull()
    expect(container.querySelector('[data-slot="avatar"]')).toBeNull()
  })
})

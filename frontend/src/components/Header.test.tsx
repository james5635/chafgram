import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
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
import { RailProvider } from '../hooks/useRail'

beforeEach(() => {
  localStorage.removeItem('rail-collapsed')
})

describe('Header', () => {
  it('shows the brand and search without a theme button or avatar', () => {
    const { container } = render(<Header />)

    expect(screen.getByRole('link', { name: 'Chafgram home' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Search Chafgram…')).toBeInTheDocument()
    expect(screen.queryByText('Auto')).not.toBeInTheDocument()
    expect(container.querySelector('button[title^="Theme mode"]')).toBeNull()
    expect(container.querySelector('[data-slot="avatar"]')).toBeNull()
  })

  it('toggles the rail width from the header', () => {
    const { container } = render(
      <RailProvider>
        <Header />
      </RailProvider>,
    )
    const inner = container.querySelector('header > div')!

    expect(screen.getByRole('button', { name: 'Collapse navigation' })).toBeInTheDocument()
    expect(inner).toHaveClass('lg:pl-64')

    fireEvent.click(screen.getByRole('button', { name: 'Collapse navigation' }))

    expect(screen.getByRole('button', { name: 'Expand navigation' })).toBeInTheDocument()
    expect(inner).toHaveClass('pl-16')
    expect(inner).not.toHaveClass('lg:pl-64')
  })
})

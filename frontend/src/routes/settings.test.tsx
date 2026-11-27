import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'

// TanStack Router's Link needs router context; render it as a plain anchor.
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

// ThemeToggle reads localStorage/matchMedia; the passkey check does not need it.
vi.mock('../components/ThemeToggle', () => ({
  default: () => <button type="button">Theme mode</button>,
}))

import { Settings } from './settings'

describe('Settings screen without passkeys', () => {
  it('does not show a passkeys section', () => {
    render(<Settings />)

    expect(screen.queryByText('Passkeys')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add a passkey' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remove passkey' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Settings' })).toBeInTheDocument()
    expect(screen.getByText('Appearance')).toBeInTheDocument()
  })
})

import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useNavigate: () => vi.fn(),
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

vi.mock('../lib/session-server', () => ({
  loginRequest: vi.fn(),
  logoutRequest: vi.fn(),
}))

import LoginScreen from './LoginScreen'

describe('Login screen without passkeys', () => {
  it('offers only the password form', () => {
    render(<LoginScreen />)

    expect(screen.queryByRole('button', { name: /Sign in with passkey/ })).not.toBeInTheDocument()
    expect(screen.queryByText(/Passkey sign-in is not available/)).not.toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument()
  })
})

import { beforeEach, describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'

const mocks = vi.hoisted(() => ({
  navigate: vi.fn(),
  loginRequest: vi.fn(),
  setAccessToken: vi.fn(),
}))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useNavigate: () => mocks.navigate,
    Link: ({ to, children, ...rest }: { to: string; children: ReactNode; [key: string]: unknown }) => (
      <a href={String(to)} {...rest}>{children}</a>
    ),
  }
})

vi.mock('../lib/session-server', () => ({
  loginRequest: mocks.loginRequest,
  logoutRequest: vi.fn(),
}))

vi.mock('../lib/auth-client', () => ({
  setAccessToken: mocks.setAccessToken,
  getAccessToken: vi.fn(() => null),
}))

import LoginScreen from './LoginScreen'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('Login screen without passkeys', () => {
  it('offers only the password form', () => {
    render(<LoginScreen />)

    expect(screen.queryByRole('button', { name: /Sign in with passkey/ })).not.toBeInTheDocument()
    expect(screen.queryByText(/Passkey sign-in is not available/)).not.toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument()
  })

  it('stores the returned access token in memory and navigates home on success', async () => {
    mocks.loginRequest.mockResolvedValue({
      ok: true,
      accessToken: 'jwt-access',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    const user = userEvent.setup()

    render(<LoginScreen />)
    await user.type(screen.getByLabelText('Email or username'), 'rojame')
    await user.type(screen.getByLabelText('Password'), 'hunter2')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(mocks.loginRequest).toHaveBeenCalledWith({
      data: { username: 'rojame', password: 'hunter2' },
    })
    expect(mocks.setAccessToken).toHaveBeenCalledWith('jwt-access')
    await waitFor(() => expect(mocks.navigate).toHaveBeenCalledWith({ to: '/' }))
  })

  it('does not store a token or navigate when credentials are rejected', async () => {
    mocks.loginRequest.mockResolvedValue({ ok: false, message: 'Wrong credentials' })
    const user = userEvent.setup()

    render(<LoginScreen />)
    await user.type(screen.getByLabelText('Email or username'), 'rojame')
    await user.type(screen.getByLabelText('Password'), 'bad')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Wrong credentials')
    expect(mocks.setAccessToken).not.toHaveBeenCalled()
    expect(mocks.navigate).not.toHaveBeenCalled()
  })
})

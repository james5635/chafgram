import { beforeEach, describe, it, expect, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
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

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

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

  it('blocks the form while signing in and ignores a second rapid submit', async () => {
    const gate = deferred<{
      ok: true
      accessToken: string
      tokenType: string
      expiresIn: number
    }>()
    mocks.loginRequest.mockReturnValue(gate.promise)

    render(<LoginScreen />)
    await userEvent.type(screen.getByLabelText('Email or username'), 'rojame')
    await userEvent.type(screen.getByLabelText('Password'), 'hunter2')

    const submit = screen.getByRole('button', { name: 'Continue' })
    fireEvent.click(submit)
    fireEvent.click(submit)

    expect(mocks.loginRequest).toHaveBeenCalledTimes(1)

    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Signing in…')
    const region = submit.closest('[aria-busy="true"]')
    expect(region).not.toBeNull()
    expect(region).toHaveClass('relative')
    expect(status.parentElement).toBe(region)

    const inertContent = submit.closest('[inert]')
    expect(inertContent).not.toBeNull()
    expect(inertContent).toContainElement(screen.getByLabelText('Email or username'))
    expect(inertContent).not.toContainElement(status)
    expect(submit).toBeDisabled()

    await act(async () => {
      gate.resolve({ ok: true, accessToken: 'jwt-access', tokenType: 'Bearer', expiresIn: 900 })
    })

    expect(mocks.navigate).toHaveBeenCalledWith({ to: '/' })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(region).toHaveAttribute('aria-busy', 'false')
  })
})

import { MockedProvider } from '@apollo/client/testing/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }))
const { loginRequest } = vi.hoisted(() => ({ loginRequest: vi.fn() }))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    createFileRoute: (_path: string) => (config: unknown) => config,
    useLocation: () => ({ pathname: '/login' }),
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

vi.mock('../lib/session-server', () => ({
  loginRequest,
  logoutRequest: vi.fn(),
}))

import LoginScreen from '../components/LoginScreen'

function renderScreen(ui: ReactNode) {
  return render(<MockedProvider mocks={[]}>{ui}</MockedProvider>)
}

beforeEach(() => {
  navigate.mockClear()
  loginRequest.mockReset()
})

describe('Login screen', () => {
  it('signs in with a password and lands on the feed', async () => {
    loginRequest.mockResolvedValue({ ok: true })
    const user = userEvent.setup()
    renderScreen(<LoginScreen />)

    await user.type(screen.getByLabelText('Email or username'), 'rojame')
    await user.type(screen.getByLabelText('Password'), 'hunter2')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(loginRequest).toHaveBeenCalledWith({ data: { username: 'rojame', password: 'hunter2' } })
    await waitFor(() => expect(navigate).toHaveBeenCalledWith({ to: '/' }))
  })

  it('shows the error the backend reported', async () => {
    loginRequest.mockResolvedValue({ ok: false, message: 'Those credentials did not match an account.' })
    const user = userEvent.setup()
    renderScreen(<LoginScreen />)

    await user.type(screen.getByLabelText('Email or username'), 'rojame')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Continue' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Those credentials did not match an account.')
    expect(navigate).not.toHaveBeenCalled()
  })

  it('explains that passkey sign-in is not available yet', async () => {
    const user = userEvent.setup()
    renderScreen(<LoginScreen />)

    await user.click(screen.getByRole('button', { name: /Sign in with passkey/ }))

    expect(await screen.findByRole('status')).toHaveTextContent('not available on this server yet')
  })
})
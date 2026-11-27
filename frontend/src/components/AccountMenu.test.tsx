import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { MouseEvent, ReactNode } from 'react'

const mocks = vi.hoisted(() => ({
  location: { pathname: '/' },
  logoutRequest: vi.fn(),
  clearAccessToken: vi.fn(),
}))

vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    useLocation: () => mocks.location,
    Link: ({ to, children, onClick, ...rest }: {
      to: string
      children: ReactNode
      onClick?: (event: MouseEvent<HTMLAnchorElement>) => void
      [key: string]: unknown
    }) => (
      <a
        href={String(to)}
        onClick={(event) => {
          event.preventDefault()
          onClick?.(event)
        }}
        {...rest}
      >
        {children}
      </a>
    ),
  }
})

vi.mock('../lib/session-server', () => ({ logoutRequest: mocks.logoutRequest }))
vi.mock('../lib/auth-client', () => ({ clearAccessToken: mocks.clearAccessToken }))

import AccountMenu from './AccountMenu'

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

function renderMenu() {
  return render(<AccountMenu displayName="Rojame" username="rojame" />)
}

beforeEach(() => {
  vi.clearAllMocks()
  mocks.location.pathname = '/'
  mocks.logoutRequest.mockResolvedValue({ ok: true })
})

describe('AccountMenu', () => {
  it('starts closed with a labelled trigger', () => {
    renderMenu()

    const trigger = screen.getByRole('button', { name: 'Account menu' })
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByRole('link', { name: /Settings/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Sign out/ })).not.toBeInTheDocument()
  })

  it('opens the menu with only Settings and Sign out', () => {
    renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))

    expect(screen.getByRole('button', { name: 'Account menu' })).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByRole('link', { name: /Settings/ })).toHaveAttribute('href', '/settings')
    expect(screen.getByRole('button', { name: /Sign out/ })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /profile/i })).not.toBeInTheDocument()
  })

  it('closes on Escape and returns focus to the trigger', () => {
    renderMenu()
    const trigger = screen.getByRole('button', { name: 'Account menu' })

    fireEvent.click(trigger)
    fireEvent.keyDown(document, { key: 'Escape' })

    expect(screen.queryByRole('link', { name: /Settings/ })).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  it('closes on a pointerdown outside the menu', () => {
    renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    fireEvent.pointerDown(document.body)

    expect(screen.queryByRole('link', { name: /Settings/ })).not.toBeInTheDocument()
  })

  it('keeps the menu open for a pointerdown inside it', () => {
    renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    fireEvent.pointerDown(screen.getByRole('link', { name: /Settings/ }))

    expect(screen.getByRole('link', { name: /Settings/ })).toBeInTheDocument()
  })

  it('closes when the Settings link is activated', () => {
    renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    fireEvent.click(screen.getByRole('link', { name: /Settings/ }))

    expect(screen.queryByRole('link', { name: /Settings/ })).not.toBeInTheDocument()
  })

  it('shows only the avatar when collapsed', () => {
    render(<AccountMenu displayName="Rojame" username="rojame" collapsed />)

    const trigger = screen.getByRole('button', { name: 'Account menu' })
    expect(trigger).toBeInTheDocument()
    expect(screen.queryByText('Rojame')).not.toBeInTheDocument()
    expect(screen.queryByText('@rojame')).not.toBeInTheDocument()

    fireEvent.click(trigger)
    expect(screen.getByRole('link', { name: /Settings/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Sign out/ })).toBeInTheDocument()
  })

  it('closes when the route changes while open', () => {
    const view = renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    mocks.location.pathname = '/settings'
    view.rerender(<AccountMenu displayName="Rojame" username="rojame" />)

    expect(screen.queryByRole('link', { name: /Settings/ })).not.toBeInTheDocument()
  })

  it('fires one logout for rapid clicks and clears the token after success', async () => {
    const gate = deferred<{ ok: boolean }>()
    mocks.logoutRequest.mockReturnValue(gate.promise)
    renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    const signOut = screen.getByRole('button', { name: /Sign out/ })
    fireEvent.click(signOut)
    fireEvent.click(signOut)

    expect(mocks.logoutRequest).toHaveBeenCalledTimes(1)
    expect(mocks.logoutRequest).toHaveBeenCalledWith({})
    expect(signOut).toBeDisabled()
    expect(signOut).toHaveTextContent('Signing out…')
    expect(screen.getByRole('link', { name: /Settings/ })).toHaveAttribute('aria-disabled', 'true')
    expect(mocks.clearAccessToken).not.toHaveBeenCalled()

    gate.resolve({ ok: true })
    await waitFor(() => expect(mocks.clearAccessToken).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('button', { name: /Sign out/ })).toBeEnabled()
    expect(screen.getByRole('button', { name: /Sign out/ })).toHaveTextContent('Sign out')
  })

  it('clears the token and re-enables the menu when logout fails', async () => {
    mocks.logoutRequest.mockRejectedValue(new Error('offline'))
    renderMenu()

    fireEvent.click(screen.getByRole('button', { name: 'Account menu' }))
    fireEvent.click(screen.getByRole('button', { name: /Sign out/ }))

    await waitFor(() => expect(mocks.clearAccessToken).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('button', { name: /Sign out/ })).toBeEnabled()
    expect(screen.getByRole('link', { name: /Settings/ })).not.toHaveAttribute('aria-disabled')
  })
})

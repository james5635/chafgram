import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import BusyOverlay from './BusyOverlay'

describe('BusyOverlay', () => {
  function renderRegion() {
    return render(
      <div className="relative" aria-busy="true">
        <div inert>
          <input aria-label="Name" />
        </div>
        <BusyOverlay label="Saving…" />
      </div>,
    )
  }

  it('renders a polite status with the action label', () => {
    renderRegion()

    const status = screen.getByRole('status')
    expect(status).toBeInTheDocument()
    expect(status).toHaveAttribute('aria-live', 'polite')
    expect(status).toHaveTextContent('Saving…')
    expect(screen.getByText('Saving…')).toHaveClass(
      'text-sm',
      'font-semibold',
      'text-[var(--sea-ink)]',
    )
  })

  it('lays an absolute, blurred token surface over the relative parent', () => {
    renderRegion()

    const status = screen.getByRole('status')
    expect(status).toHaveClass('absolute', 'inset-0', 'z-10', 'backdrop-blur-[2px]')
    expect(status.parentElement).toHaveClass('relative')
  })

  it('shows a spinning ring alongside the label', () => {
    renderRegion()

    const status = screen.getByRole('status')
    expect(status.querySelector('.animate-spin')).not.toBeNull()
  })

  it('sits outside the inert content subtree containing the controls', () => {
    renderRegion()

    const input = screen.getByLabelText('Name')
    const inertSubtree = input.closest('[inert]')
    expect(inertSubtree).not.toBeNull()
    expect(inertSubtree).toContainElement(input)

    const status = screen.getByRole('status')
    expect(inertSubtree).not.toContainElement(status)
    expect(status.parentElement).not.toBe(inertSubtree)
  })
})

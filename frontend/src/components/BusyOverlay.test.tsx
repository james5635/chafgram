import { describe, it, expect } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { BusyOverlay } from './BusyOverlay'

describe('BusyOverlay', () => {
  const label = 'Saving…'

  function renderRegion() {
    return render(
      <>
        <span className="sr-only" role="status" aria-live="polite">{label}</span>
        <div className="relative" aria-busy="true">
          <div inert>
            <input aria-label="Name" />
          </div>
          <BusyOverlay label={label} />
        </div>
      </>,
    )
  }

  it('renders one polite live status outside the busy root', () => {
    renderRegion()

    const statuses = screen.getAllByRole('status')
    expect(statuses).toHaveLength(1)
    const status = statuses[0]
    expect(status).toHaveAttribute('aria-live', 'polite')
    expect(status).toHaveClass('sr-only')
    expect(status).toHaveTextContent(label)

    const busyRoot = screen.getByLabelText('Name').closest('[aria-busy="true"]')
    expect(busyRoot).not.toBeNull()
    expect(busyRoot).not.toContainElement(status)
  })

  it('lays an absolute, blurred, hidden token surface over the relative parent', () => {
    renderRegion()

    const overlay = document.querySelector('[aria-hidden="true"].absolute')
    expect(overlay).not.toBeNull()
    expect(overlay).toHaveClass('absolute', 'inset-0', 'z-10', 'backdrop-blur-[2px]')
    expect(overlay).toHaveAttribute('aria-hidden', 'true')
    expect(overlay?.parentElement).toHaveClass('relative')
    expect(within(overlay as HTMLElement).getByText(label)).toHaveClass(
      'text-sm',
      'font-semibold',
      'text-[var(--sea-ink)]',
    )
  })

  it('shows a spinning ring alongside the label', () => {
    renderRegion()

    const overlay = document.querySelector('[aria-hidden="true"].absolute')
    expect(overlay?.querySelector('.animate-spin')).not.toBeNull()
  })

  it('sits outside the inert content subtree containing the controls', () => {
    renderRegion()

    const input = screen.getByLabelText('Name')
    const inertSubtree = input.closest('[inert]')
    expect(inertSubtree).not.toBeNull()
    expect(inertSubtree).toContainElement(input)

    const status = screen.getByRole('status')
    expect(inertSubtree).not.toContainElement(status)
    const overlay = document.querySelector('[aria-hidden="true"].absolute')
    expect(inertSubtree).not.toContainElement(overlay as HTMLElement | null)
    expect(overlay?.parentElement).not.toBe(inertSubtree)
  })
})

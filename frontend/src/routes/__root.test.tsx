import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { ReactNode } from 'react'
import { AppProviders } from './__root'
import { useRail } from '../hooks/useRail'

function RailProbe() {
  const { collapsed, toggle } = useRail()
  return (
    <>
      <span data-testid="collapsed">{String(collapsed)}</span>
      <button type="button" onClick={toggle}>Probe toggle</button>
    </>
  )
}

describe('AppProviders', () => {
  it('provides rail state to the app tree', () => {
    const children: ReactNode = <RailProbe />
    render(<AppProviders>{children}</AppProviders>)

    expect(screen.getByTestId('collapsed')).toHaveTextContent('false')

    fireEvent.click(screen.getByRole('button', { name: 'Probe toggle' }))

    expect(screen.getByTestId('collapsed')).toHaveTextContent('true')
  })
})

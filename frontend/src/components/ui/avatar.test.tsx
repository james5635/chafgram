import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Avatar } from './avatar'
import { AvatarFallback } from './avatar'

describe('AvatarFallback', () => {
  it('renders one initial with a stable tint when given a name', () => {
    render(
      <Avatar aria-label="Dara Chan">
        <AvatarFallback name="Dara Chan" />
      </Avatar>,
    )

    const fallback = screen.getByText('D')
    expect(fallback).toBeInTheDocument()
    expect(fallback.style.backgroundColor).toMatch(/^var\(--avatar-tint-[1-6]\)$/)
  })

  it('keeps explicit children when no name is provided', () => {
    render(
      <Avatar aria-label="Fallback">
        <AvatarFallback>Custom</AvatarFallback>
      </Avatar>,
    )

    expect(screen.getByText('Custom')).toBeInTheDocument()
  })
})

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import Avatar from './Avatar'

describe('Avatar', () => {
  it('renders only the first initial when no image src is provided', () => {
    render(<Avatar initials="JD" label="Jane Doe" size={40} />)

    const el = screen.getByLabelText('Jane Doe')
    expect(el).toBeInTheDocument()
    expect(el).toHaveTextContent('J')
    expect(el).not.toHaveTextContent('JD')
  })

  it('uses a deterministic tint for the name', () => {
    const { rerender } = render(<Avatar label="Jane Doe" />)
    const first = screen.getByLabelText('Jane Doe').style.backgroundColor

    rerender(<Avatar label="Jane Doe" />)
    expect(screen.getByLabelText('Jane Doe').style.backgroundColor).toBe(first)
    expect(first).toMatch(/^var\(--avatar-tint-\d\)$/)
  })

  it('renders an img element when a src is provided', () => {
    const { container } = render(
      <Avatar src="/assets/avatar.png" initials="JD" label="Jane" size={32} />
    )

    expect(container.querySelector('img')).not.toBeNull()
    expect(container.querySelector('img')?.getAttribute('src')).toBe(
      '/assets/avatar.png'
    )
  })

  it('applies the requested size class', () => {
    render(<Avatar initials="A" label="A" size={56} />)

    expect(screen.getByLabelText('A')).toHaveClass('h-14', 'w-14')
  })

  it('defaults to size 40 when no size is given', () => {
    render(<Avatar initials="A" label="A" />)

    expect(screen.getByLabelText('A')).toHaveClass('h-10', 'w-10')
  })
})

import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import Composer from './Composer'

const MEMBER = { name: 'Dara', username: 'dara', initials: 'DR' }

describe('Composer', () => {
  it('renders the composer with a solid, enabled Post button', () => {
    render(<Composer member={MEMBER} />)

    expect(screen.getByPlaceholderText("What's on your mind?")).toBeInTheDocument()
    const postButton = screen.getByRole('button', { name: 'Post' })
    expect(postButton).not.toBeDisabled()
    expect(postButton).toHaveClass('bg-[var(--brand)]')
  })

  it('accepts text input', () => {
    render(<Composer member={MEMBER} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    fireEvent.input(textarea, { target: { value: 'Hello world' } })

    expect(textarea.value).toBe('Hello world')
  })

  it('renders chips as static buttons with icons', () => {
    render(<Composer member={MEMBER} />)

    const photoChip = screen.getByRole('button', { name: 'Photo' })
    expect(photoChip.tagName).toBe('BUTTON')
    expect(photoChip.querySelector('span')).not.toBeNull()
  })

  it('renders all four media chips', () => {
    render(<Composer member={MEMBER} />)

    for (const chip of ['Photo', 'Video', 'Feeling', 'Poll']) {
      expect(screen.getByRole('button', { name: chip })).toBeInTheDocument()
    }
  })
})

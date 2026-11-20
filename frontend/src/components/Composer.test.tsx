import { describe, it, expect } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import Composer from './Composer'

const MEMBER = { name: 'Dara', username: 'dara', initials: 'DR' }

describe('Composer', () => {
  it('renders the composer with a disabled Post button', () => {
    render(<Composer member={MEMBER} />)

    expect(screen.getByPlaceholderText("What's on your mind?")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Post' })).toBeDisabled()
  })

  it('enables the Post button once text is entered', () => {
    render(<Composer member={MEMBER} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?")
    fireEvent.input(textarea, { target: { value: 'Hello world' } })

    expect(screen.getByRole('button', { name: 'Post' })).not.toBeDisabled()
  })

  it('clears the disabled state back when text is removed', () => {
    render(<Composer member={MEMBER} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?")
    fireEvent.input(textarea, { target: { value: 'Hello world' } })
    fireEvent.input(textarea, { target: { value: '' } })

    expect(screen.getByRole('button', { name: 'Post' })).toBeDisabled()
  })

  it('renders chips as static buttons (not toggleable)', () => {
    render(<Composer member={MEMBER} />)

    const photoChip = screen.getByRole('button', { name: 'Photo' })
    expect(photoChip).toHaveClass('bg-white')
    expect(photoChip).not.toHaveClass('bg-[var(--chip-bg)]')
  })

  it('renders all four media chips', () => {
    render(<Composer member={MEMBER} />)

    for (const chip of ['Photo', 'Video', 'Feeling', 'Poll']) {
      expect(screen.getByRole('button', { name: chip })).toBeInTheDocument()
    }
  })
})

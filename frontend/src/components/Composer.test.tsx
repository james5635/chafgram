import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Composer from './Composer'

const MEMBER = { name: 'Dara', username: 'dara', initials: 'DR' }

describe('Composer', () => {
  it('renders the composer with a solid Post button', () => {
    render(<Composer member={MEMBER} />)

    expect(screen.getByPlaceholderText("What's on your mind?")).toBeInTheDocument()
    const postButton = screen.getByRole('button', { name: 'Post' })
    expect(postButton).toHaveClass('bg-[var(--brand)]')
  })

  it('keeps the Post button disabled until there is text', () => {
    render(<Composer member={MEMBER} />)

    const postButton = screen.getByRole('button', { name: 'Post' })
    expect(postButton).toBeDisabled()

    const textarea = screen.getByPlaceholderText("What's on your mind?")
    fireEvent.input(textarea, { target: { value: 'Hello world' } })

    expect(postButton).not.toBeDisabled()
  })

  it('accepts text input', () => {
    render(<Composer member={MEMBER} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    fireEvent.input(textarea, { target: { value: 'Hello world' } })

    expect(textarea.value).toBe('Hello world')
  })

  it('publishes the trimmed text and clears the editor', async () => {
    const onSubmit = vi.fn().mockResolvedValue(true)
    render(<Composer member={MEMBER} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    fireEvent.input(textarea, { target: { value: '  Hello world  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Post' }))

    expect(onSubmit).toHaveBeenCalledWith('Hello world')
    await waitFor(() => expect(textarea.value).toBe(''))
  })

  it('keeps the text when publishing fails', async () => {
    const onSubmit = vi.fn().mockResolvedValue(false)
    render(<Composer member={MEMBER} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    fireEvent.input(textarea, { target: { value: 'Hello world' } })
    fireEvent.click(screen.getByRole('button', { name: 'Post' }))

    expect(onSubmit).toHaveBeenCalledWith('Hello world')
    await waitFor(() => expect(screen.getByRole('button', { name: 'Post' })).not.toBeDisabled())
    expect(textarea.value).toBe('Hello world')
  })

  it('renders chips as static buttons with icons', () => {
    render(<Composer member={MEMBER} />)

    const photoChip = screen.getByRole('button', { name: 'Photo' })
    expect(photoChip.tagName).toBe('BUTTON')
    // Check for the SVG icon from lucide-react
    expect(photoChip.querySelector('svg')).not.toBeNull()
  })

  it('renders all four media chips', () => {
    render(<Composer member={MEMBER} />)

    for (const chip of ['Photo', 'Video', 'Feeling', 'Poll']) {
      expect(screen.getByRole('button', { name: chip })).toBeInTheDocument()
    }
  })
})
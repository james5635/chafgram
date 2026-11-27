import { describe, it, expect, vi, beforeAll } from 'vitest'
import { act, render, screen, fireEvent, waitFor } from '@testing-library/react'
import Composer from './Composer'

const { uploadMedia } = vi.hoisted(() => ({ uploadMedia: vi.fn() }))
vi.mock('@/lib/media-server', () => ({ uploadMedia }))

beforeAll(() => {
  Object.defineProperty(URL, 'createObjectURL', {
    configurable: true,
    value: () => 'blob:preview',
  })
  Object.defineProperty(URL, 'revokeObjectURL', {
    configurable: true,
    value: () => undefined,
  })
})

const MEMBER = { name: 'Dara', username: 'dara', initials: 'DR' }

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

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

  it('opens the photo picker when the Photo chip is clicked', () => {
    render(<Composer member={MEMBER} />)
    const input = screen.getByLabelText('Choose photos') as HTMLInputElement
    const click = vi.spyOn(input, 'click')

    fireEvent.click(screen.getByRole('button', { name: 'Photo' }))

    expect(click).toHaveBeenCalledTimes(1)
  })

  it('uploads a photo chosen from the Photo chip and attaches it to the post', async () => {
    uploadMedia.mockReset()
    uploadMedia.mockResolvedValue({ id: 'media-1', kind: 'image', status: 'ready' })
    const onSubmit = vi.fn().mockResolvedValue(true)
    render(<Composer member={MEMBER} onSubmit={onSubmit} />)

    const file = new File(['image bytes'], 'photo.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText('Choose photos'), { target: { files: [file] } })

    expect(await screen.findByAltText('photo.png')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Post' })).not.toBeDisabled()

    fireEvent.click(screen.getByRole('button', { name: 'Post' }))

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith('', { mediaIds: ['media-1'], type: 'PHOTO' }),
    )
    expect(uploadMedia).toHaveBeenCalledWith({
      data: { filename: 'photo.png', contentType: 'image/png', data: expect.any(String) },
    })
  })

  it('keeps the composer open and shows an error when the upload fails', async () => {
    uploadMedia.mockReset()
    uploadMedia.mockRejectedValue(new Error('Media upload failed'))
    const onSubmit = vi.fn().mockResolvedValue(true)
    render(<Composer member={MEMBER} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    fireEvent.input(textarea, { target: { value: 'A photo' } })
    fireEvent.change(screen.getByLabelText('Choose photos'), {
      target: { files: [new File(['bytes'], 'broken.png', { type: 'image/png' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Post' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Media upload failed')
    expect(onSubmit).not.toHaveBeenCalled()
    expect(textarea.value).toBe('A photo')
  })

  it('shows Posting… while publishing and ignores a second rapid Post click', async () => {
    const gate = deferred<boolean>()
    const onSubmit = vi.fn(() => gate.promise)
    render(<Composer member={MEMBER} onSubmit={onSubmit} />)

    const textarea = screen.getByPlaceholderText("What's on your mind?") as HTMLTextAreaElement
    fireEvent.input(textarea, { target: { value: 'Hello world' } })
    const postButton = screen.getByRole('button', { name: 'Post' })
    fireEvent.click(postButton)
    fireEvent.click(postButton)

    expect(onSubmit).toHaveBeenCalledTimes(1)

    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Posting…')
    const region = postButton.closest('[aria-busy="true"]')
    expect(region).not.toBeNull()
    expect(region).toHaveClass('relative')
    expect(status.parentElement).toBe(region)

    const inertContent = postButton.closest('[inert]')
    expect(inertContent).not.toBeNull()
    expect(inertContent).toContainElement(textarea)
    expect(inertContent).not.toContainElement(status)
    expect(postButton).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Photo' })).toBeDisabled()

    await act(async () => {
      gate.resolve(true)
    })

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(region).toHaveAttribute('aria-busy', 'false')
  })
})
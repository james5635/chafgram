import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import ImageLightbox from './ImageLightbox'

const onClose = vi.fn()

const images = [
  { url: '/first.jpg', alt: 'First image' },
  { url: '/second.jpg', alt: 'Second image' },
  { url: '/third.jpg', alt: 'Third image' },
]

function renderLightbox(props: {
  images?: { url: string; alt: string }[]
  initialIndex?: number
  open?: boolean
} = {}) {
  return render(
    <ImageLightbox
      images={images}
      initialIndex={1}
      open
      onClose={onClose}
      {...props}
    />,
  )
}

beforeEach(() => {
  onClose.mockClear()
})

describe('ImageLightbox', () => {
  it('shows the initial image with its alt text and a counter', () => {
    renderLightbox()

    expect(screen.getByRole('img', { name: 'Second image' })).toHaveAttribute('src', '/second.jpg')
    expect(screen.getByText('2 / 3')).toBeInTheDocument()
  })

  it('navigates with the previous and next buttons', () => {
    renderLightbox()

    fireEvent.click(screen.getByRole('button', { name: 'Next image' }))
    expect(screen.getByRole('img', { name: 'Third image' })).toBeInTheDocument()
    expect(screen.getByText('3 / 3')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Previous image' }))
    expect(screen.getByRole('img', { name: 'Second image' })).toBeInTheDocument()
  })

  it('navigates with the arrow keys', () => {
    renderLightbox()

    fireEvent.keyDown(document, { key: 'ArrowRight' })
    expect(screen.getByRole('img', { name: 'Third image' })).toBeInTheDocument()

    fireEvent.keyDown(document, { key: 'ArrowLeft' })
    expect(screen.getByRole('img', { name: 'Second image' })).toBeInTheDocument()
  })

  it('disables navigation at both ends', () => {
    renderLightbox({ initialIndex: 0 })

    expect(screen.getByRole('button', { name: 'Previous image' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next image' })).toBeEnabled()

    fireEvent.click(screen.getByRole('button', { name: 'Next image' }))
    fireEvent.click(screen.getByRole('button', { name: 'Next image' }))
    expect(screen.getByRole('button', { name: 'Next image' })).toBeDisabled()
  })

  it('closes on a backdrop click', () => {
    renderLightbox()

    fireEvent.click(screen.getByTestId('lightbox-backdrop'))
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('closes on Escape', () => {
    renderLightbox()

    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('starts at the new index when reopened', () => {
    const view = renderLightbox({ initialIndex: 0 })

    fireEvent.click(screen.getByRole('button', { name: 'Next image' }))
    expect(screen.getByRole('img', { name: 'Second image' })).toBeInTheDocument()

    view.rerender(
      <ImageLightbox images={images} initialIndex={0} open={false} onClose={onClose} />,
    )
    view.rerender(
      <ImageLightbox images={images} initialIndex={2} open onClose={onClose} />,
    )

    expect(screen.getByRole('img', { name: 'Third image' })).toBeInTheDocument()
  })
})

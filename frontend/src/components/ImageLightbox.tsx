import { useEffect, useState } from 'react'
import { Dialog } from '@base-ui/react/dialog'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'

export interface LightboxImage {
  url: string
  alt: string
}

interface ImageLightboxProps {
  images: LightboxImage[]
  initialIndex: number
  open: boolean
  onClose: () => void
}

/** Full-size image preview with previous/next navigation. */
export default function ImageLightbox({ images, initialIndex, open, onClose }: ImageLightboxProps) {
  const [index, setIndex] = useState(initialIndex)

  useEffect(() => {
    if (open) setIndex(initialIndex)
  }, [open, initialIndex])

  useEffect(() => {
    if (!open) return

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'ArrowRight') {
        setIndex((current) => Math.min(current + 1, images.length - 1))
      } else if (event.key === 'ArrowLeft') {
        setIndex((current) => Math.max(current - 1, 0))
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, images.length])

  const current = images[index]
  if (!current) return null

  return (
    <Dialog.Root open={open} onOpenChange={(next) => { if (!next) onClose() }}>
      <Dialog.Portal>
        <Dialog.Backdrop
          data-testid="lightbox-backdrop"
          className="fixed inset-0 z-50 bg-[color-mix(in_oklab,var(--sea-ink)70%,transparent)] backdrop-blur-sm"
        />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 flex max-h-[90vh] w-[min(92vw,72rem)] -translate-x-1/2 -translate-y-1/2 flex-col gap-3 rounded-xl border border-[var(--line)] bg-[var(--surface-strong)] p-4 shadow-xl">
          <Dialog.Title className="m-0 text-sm font-semibold text-[var(--sea-ink)]">
            Image preview
          </Dialog.Title>

          <div className="flex min-h-0 flex-1 items-center justify-center">
            <img
              src={current.url}
              alt={current.alt}
              className="max-h-[70vh] max-w-full rounded-lg object-contain"
            />
          </div>

          <div className="flex items-center justify-center gap-4">
            <button
              type="button"
              aria-label="Previous image"
              disabled={index === 0}
              onClick={() => setIndex((value) => Math.max(value - 1, 0))}
              className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-[var(--line)] text-[var(--sea-ink)] transition hover:bg-[var(--brand-light)] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronLeft className="h-5 w-5" aria-hidden="true" />
            </button>
            <span aria-live="polite" className="text-sm font-semibold text-[var(--sea-ink)]">
              {index + 1} / {images.length}
            </span>
            <button
              type="button"
              aria-label="Next image"
              disabled={index === images.length - 1}
              onClick={() => setIndex((value) => Math.min(value + 1, images.length - 1))}
              className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-[var(--line)] text-[var(--sea-ink)] transition hover:bg-[var(--brand-light)] disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ChevronRight className="h-5 w-5" aria-hidden="true" />
            </button>
          </div>

          <Dialog.Close
            aria-label="Close preview"
            className="absolute right-3 top-3 inline-flex h-8 w-8 items-center justify-center rounded-full border border-[var(--line)] text-[var(--sea-ink)] transition hover:bg-[var(--brand-light)]"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </Dialog.Close>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

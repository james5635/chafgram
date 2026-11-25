import { useEffect, useMemo, useRef, useState } from 'react'
import { Image, Video, Heart, BarChart2, X } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { uploadMedia } from '@/lib/media-server'
import { fileToBase64, postTypeFor } from '@/lib/media'

const CHIPS = [
  { label: 'Photo', color: '#2563eb', icon: Image, kind: 'PHOTO' as const },
  { label: 'Video', color: '#2563eb', icon: Video, kind: 'VIDEO' as const },
  { label: 'Feeling', color: '#f59e0b', icon: Heart, kind: undefined },
  { label: 'Poll', color: '#4f46e5', icon: BarChart2, kind: undefined },
]

export interface ComposerMedia {
  mediaIds: string[]
  type: 'PHOTO' | 'VIDEO'
}

interface ComposerProps {
  member: { name: string; username: string; avatar?: string; initials?: string }
  /** Called with media info only when the composer has uploaded attachments. */
  onSubmit?: (body: string, media?: ComposerMedia) => Promise<boolean> | boolean
}

export default function Composer({ member, onSubmit }: ComposerProps) {
  const [body, setBody] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const photoInput = useRef<HTMLInputElement>(null)
  const videoInput = useRef<HTMLInputElement>(null)

  const previews = useMemo(
    () => files.map((file) => ({ name: file.name, url: URL.createObjectURL(file) })),
    [files],
  )

  useEffect(() => () => previews.forEach((preview) => URL.revokeObjectURL(preview.url)), [previews])

  function addFiles(selected: FileList | null) {
    if (!selected?.length) return
    setError(null)
    setFiles((current) => [...current, ...Array.from(selected)])
  }

  function removeFile(index: number) {
    setFiles((current) => current.filter((_, i) => i !== index))
  }

  async function handleSubmit() {
    const text = body.trim()
    if ((!text && files.length === 0) || !onSubmit || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      let media: ComposerMedia | undefined
      if (files.length > 0) {
        const mediaIds: string[] = []
        for (const file of files) {
          const asset = await uploadMedia({
            data: {
              filename: file.name,
              contentType: file.type || 'application/octet-stream',
              data: await fileToBase64(file),
            },
          })
          mediaIds.push(asset.id)
        }
        media = { mediaIds, type: files.some((file) => postTypeFor(file) === 'VIDEO') ? 'VIDEO' : 'PHOTO' }
      }

      const published = media ? await onSubmit(text, media) : await onSubmit(text)
      if (published) {
        setBody('')
        setFiles([])
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not upload your photos. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="island-shell rise-in rounded-xl overflow-hidden border border-[var(--line)] bg-white shadow-sm">
      <div className="flex items-center gap-3 px-5 py-4 sm:px-6 sm:py-5">
        <Avatar className="h-8 w-8" aria-label={member.name}>
          {member.avatar && <AvatarImage src={member.avatar} alt={member.name} />}
          <AvatarFallback name={member.name} />
        </Avatar>
        <Textarea
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="What's on your mind?"
          rows={1}
          aria-label="What's on your mind?"
          className="flex-1 min-w-0 bg-transparent text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline outline-none resize-none focus:ring-0 leading-6"
        />
      </div>

      <input
        ref={photoInput}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        aria-label="Choose photos"
        onChange={(event) => {
          addFiles(event.target.files)
          event.target.value = ''
        }}
      />
      <input
        ref={videoInput}
        type="file"
        accept="video/*"
        className="hidden"
        aria-label="Choose a video"
        onChange={(event) => {
          addFiles(event.target.files)
          event.target.value = ''
        }}
      />

      {previews.length > 0 && (
        <ul className="m-0 flex flex-wrap gap-2 px-5 pb-3 sm:px-6" aria-label="Attached media">
          {previews.map((preview, index) => (
            <li key={`${preview.name}-${index}`} className="relative h-16 w-16 overflow-hidden rounded-lg border border-[var(--line)]">
              <img src={preview.url} alt={preview.name} className="h-full w-full object-cover" />
              <button
                type="button"
                onClick={() => removeFile(index)}
                aria-label={`Remove ${preview.name}`}
                className="absolute right-0 top-0 rounded-bl bg-[var(--sea-ink)] p-0.5 text-white"
              >
                <X className="h-3 w-3" aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {error && (
        <p className="m-0 px-5 pb-3 text-sm text-[#9f3030] sm:px-6" role="alert">
          {error}
        </p>
      )}

      <div className="border-t border-[var(--line)] px-5 py-3 sm:px-6 flex items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-1 sm:gap-2">
          {CHIPS.map((chip) => {
            const Icon = chip.icon
            return (
              <Button
                key={chip.label}
                type="button"
                variant="ghost"
                size="sm"
                disabled={submitting || !chip.kind}
                onClick={
                  chip.kind === 'PHOTO'
                    ? () => photoInput.current?.click()
                    : chip.kind === 'VIDEO'
                      ? () => videoInput.current?.click()
                      : undefined
                }
                className="gap-1.5 rounded-full text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[#f3f4f6] disabled:opacity-60"
                style={{ color: chip.color }}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
                {chip.label}
              </Button>
            )
          })}
        </div>

        <Button
          type="button"
          onClick={() => void handleSubmit()}
          disabled={(!body.trim() && files.length === 0) || submitting}
          className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-5 py-2 text-sm font-bold !text-white no-underline transition hover:bg-[var(--brand-dark)] shadow-lg shadow-[var(--brand)]/30 disabled:opacity-50 disabled:shadow-none"
        >
          {submitting ? 'Posting…' : 'Post'}
        </Button>
      </div>
    </section>
  )
}

import { useMutation, useQuery } from '@apollo/client/react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useRef, useState } from 'react'
import { Image, Video, Heart, BarChart2, ArrowLeft, Upload } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import AppShell from '@/components/AppShell'
import { CreatePostDocument, MeDocument } from '@/generated/graphql'
import { uploadMedia } from '@/lib/media-server'

const CHIPS = [
  { label: 'Photo', color: '#2563eb', icon: Image },
  { label: 'Video', color: '#2563eb', icon: Video },
  { label: 'Feeling', color: '#f59e0b', icon: Heart },
  { label: 'Poll', color: '#4f46e5', icon: BarChart2 },
] as const

/** Read a file as base64 so it can travel through a server function. */
async function fileToBase64(file: File): Promise<string> {
  const bytes = new Uint8Array(await file.arrayBuffer())
  let binary = ''
  const chunkSize = 0x8000
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize))
  }
  return btoa(binary)
}

function postTypeFor(file: File): 'PHOTO' | 'VIDEO' {
  return file.type.startsWith('video/') ? 'VIDEO' : 'PHOTO'
}

export const Route = createFileRoute('/compose')({
  component: Compose,
})

export function Compose() {
  const navigate = useNavigate()
  const fileInput = useRef<HTMLInputElement>(null)
  const { data: meData } = useQuery(MeDocument)
  const [createPost] = useMutation(CreatePostDocument, { refetchQueries: ['FollowingFeed'] })

  const [body, setBody] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [publishing, setPublishing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const me = meData?.me

  async function publish() {
    if (publishing) return
    setPublishing(true)
    setError(null)

    try {
      const mediaIds: string[] = []
      let type: 'NOTE' | 'PHOTO' | 'VIDEO' = 'NOTE'

      if (file) {
        type = postTypeFor(file)
        const asset = await uploadMedia({
          data: {
            filename: file.name,
            contentType: file.type || 'application/octet-stream',
            data: await fileToBase64(file),
          },
        })
        mediaIds.push(asset.id)
      }

      await createPost({
        variables: {
          type,
          body: body.trim() || null,
          mediaIds: mediaIds.length > 0 ? mediaIds : null,
        },
      })

      await navigate({ to: '/' })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not publish your post.')
    } finally {
      setPublishing(false)
    }
  }

  return (
    <AppShell>
      <div className="page-wrap px-4 pb-8 pt-6 max-w-3xl">
        <header className="mb-5 flex items-center gap-3">
          <Link to="/" className="text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]" aria-label="Back to home">
            <ArrowLeft className="h-5 w-5" aria-hidden="true" />
          </Link>
          <h1 className="m-0 text-xl font-bold text-[var(--sea-ink)] sm:text-2xl">Create Post</h1>
        </header>

        <section className="island-shell rise-in overflow-hidden rounded-xl">
          <div className="grid gap-0 md:grid-cols-2">
            <div className="p-5 sm:p-6">
              <div className="flex gap-3">
                <Avatar className="h-10 w-10" aria-label={me?.displayName ?? 'You'}>
                  <AvatarFallback name={me?.displayName ?? 'You'} />
                </Avatar>
                <Textarea
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                  placeholder="What's on your mind?"
                  aria-label="What's on your mind?"
                  rows={4}
                  className="flex-1 min-w-0 bg-transparent pt-1 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline outline-none resize-none focus:ring-0 leading-6"
                />
              </div>

              <div className="mt-6 flex flex-wrap items-center gap-4 border-t border-[var(--line)] pt-4">
                {CHIPS.map((chip) => {
                  const Icon = chip.icon
                  return (
                    <Button
                      key={chip.label}
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="gap-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]"
                      style={{ color: chip.color }}
                    >
                      <Icon className="h-4 w-4" aria-hidden="true" />
                      {chip.label}
                    </Button>
                  )
                })}
              </div>
            </div>

            <div className="border-t border-[var(--line)] p-5 sm:p-6 md:border-l md:border-t-0">
              <input
                ref={fileInput}
                type="file"
                accept="image/*,video/*"
                className="sr-only"
                aria-label="Upload media"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
              <button
                type="button"
                onClick={() => fileInput.current?.click()}
                className="flex h-full min-h-40 w-full flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed border-[var(--line)] bg-[var(--brand-light)]/40 p-6 text-center transition hover:border-[var(--brand)] hover:bg-[var(--brand-light)]/70"
              >
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-[var(--brand-dark)] shadow-sm">
                  <Upload className="h-6 w-6" aria-hidden="true" />
                </span>
                <span className="text-sm font-semibold text-[var(--sea-ink)]">
                  {file ? file.name : 'Upload Image'}
                </span>
                <span className="text-xs text-[var(--sea-ink-soft)]">or drag and drop</span>
              </button>
            </div>
          </div>

          {error && (
            <p className="m-0 border-t border-[var(--line)] px-5 py-3 text-sm text-[#9f3030] sm:px-6" role="alert">
              {error}
            </p>
          )}

          <div className="flex items-center justify-end gap-3 border-t border-[var(--line)] px-5 py-4 sm:px-6">
            <Button variant="outline" size="sm" asChild className="rounded-full">
              <Link to="/">Cancel</Link>
            </Button>
            <Button
              type="button"
              onClick={() => void publish()}
              disabled={publishing || (!body.trim() && !file)}
              className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-6 py-2 text-sm font-bold !text-white shadow-lg shadow-[var(--brand)]/30 transition hover:bg-[var(--brand-dark)] disabled:opacity-50 disabled:shadow-none"
            >
              {publishing ? 'Posting…' : 'Post'}
            </Button>
          </div>
        </section>
      </div>
    </AppShell>
  )
}

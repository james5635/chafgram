import { useState } from 'react'
import { Image, Video, Heart, BarChart2 } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'

const CHIPS = [
  { label: 'Photo', color: '#2563eb', icon: Image },
  { label: 'Video', color: '#2563eb', icon: Video },
  { label: 'Feeling', color: '#f59e0b', icon: Heart },
  { label: 'Poll', color: '#4f46e5', icon: BarChart2 },
] as const

interface ComposerProps {
  member: { name: string; username: string; avatar?: string; initials?: string }
  onSubmit?: (body: string) => Promise<boolean> | boolean
}

export default function Composer({ member, onSubmit }: ComposerProps) {
  const [body, setBody] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit() {
    const text = body.trim()
    if (!text || !onSubmit || submitting) return

    setSubmitting(true)
    try {
      const published = await onSubmit(text)
      if (published) setBody('')
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
                className="gap-1.5 rounded-full text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[#f3f4f6]"
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
          disabled={!body.trim() || submitting}
          className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-5 py-2 text-sm font-bold !text-white no-underline transition hover:bg-[var(--brand-dark)] shadow-lg shadow-[var(--brand)]/30 disabled:opacity-50 disabled:shadow-none"
        >
          {submitting ? 'Posting…' : 'Post'}
        </Button>
      </div>
    </section>
  )
}

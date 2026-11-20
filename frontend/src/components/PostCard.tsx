import { Link } from '@tanstack/react-router'
import Avatar from './Avatar'

interface PostCardProps {
  post: {
    id: string
    author: { name: string; username: string; avatar?: string; initials?: string }
    createdAt: string
    body: string
    media?: { url: string; alt: string }[]
    reactions: { count: number }
    comments: number
    shares: number
    edited?: boolean
  }
}

export default function PostCard({ post }: PostCardProps) {
  return (
    <article
      className="island-shell rise-in rounded-[1.5rem] overflow-hidden border border-[var(--line)] bg-white shadow-sm"
      style={{ animationDelay: '80ms' }}
    >
      <div className="flex gap-3 p-4 sm:p-5">
        <div className="flex-shrink-0">
          <Link
            to="/profile/$username"
            params={{ username: post.author.username }}
            className="flex items-center gap-3"
          >
            <Avatar
              src={post.author.avatar}
              initials={post.author.initials}
              label={post.author.name}
              size={32}
            />
            <div className="flex flex-col leading-none">
              <span className="text-sm font-semibold text-[var(--sea-ink)] block">
                {post.author.name}
              </span>
              <span className="text-[0.75rem] text-[var(--sea-ink-soft)] block">
                {post.createdAt} · public
              </span>
            </div>
          </Link>
        </div>

        <button
          type="button"
          className="ml-auto p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="More options"
        >
          <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true">
            <circle cx="8" cy="8" r="1" fill="currentColor" />
            <circle cx="13" cy="8" r="1" fill="currentColor" />
            <circle cx="3" cy="8" r="1" fill="currentColor" />
          </svg>
        </button>
      </div>

      {post.body && (
        <div className="px-4 sm:px-5 pt-1 sm:pt-0 pb-3">
          <p className="m-0 text-base text-[var(--sea-ink)] leading-7">{post.body}</p>
        </div>
      )}

      {post.media && post.media.length > 0 && (
        <div className="px-4 sm:px-5 pb-5 sm:pb-0">
          <div className="aspect-video rounded-xl overflow-hidden border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)70%,white_30%)]">
            <img src={post.media[0].url} alt={post.media[0].alt} className="h-full w-full object-cover" loading="lazy" decoding="async" />
          </div>
        </div>
      )}

      <div className="px-4 sm:px-5 pt-4 pb-5 sm:pt-0 border-t border-[var(--line)]">
        <div className="flex items-baseline gap-4 sm:gap-6 mb-3">
          <div className="flex items-center gap-1 text-sm font-semibold text-[var(--sea-ink)]">
            <span>👍</span>
            <span>❤️</span>
            <span className="ml-1">{post.reactions.count}</span>
          </div>
          <span className="text-[var(--sea-ink-soft)] text-sm">{post.comments} comments</span>
          <span className="text-[var(--sea-ink-soft)] text-sm">{post.shares} shares</span>
        </div>

        <div className="flex items-center gap-5 sm:gap-8 pt-2">
          <button type="button" className="flex items-center gap-1.5 text-sm font-medium text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]" aria-label="Like">
            <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
            Like
          </button>
          <button type="button" className="flex items-center gap-1.5 text-sm font-medium text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]" aria-label="Comment">
            <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M21 5c-1.1 0-2 .9-2 2v14l-4-4h-6c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 14h-4v-4c0-1.1-.9-2-2-2h-4c-1.1 0-2 .9-2 2v4H4V7c0-2.21 1.79-4 4-4h12c2.21 0 4 1.79 4 4v12c0 1.1-.9 2-2 2z"/></svg>
            Comment
          </button>
          <button type="button" className="flex items-center gap-1.5 text-sm font-medium text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]" aria-label="Share">
            <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.52.5 1.2.78 1.96.77.76 0 1.44-.3 1.96-.77l7.05 4.11c.05.03.09.09.09.15s-.04.12-.09.15l-7.05 4.11c-.52.5-1.24.81-2.05.81z"/></svg>
            Share
          </button>
          <button className="ml-auto p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]" aria-label="Save">
            <svg viewBox="0 0 16 16" className="h-4 w-4" aria-hidden="true">
              <path fill="currentColor" d="M5.5 10.75V5.818l2.5-2.1L8.5 5.818v4.932l2.5-2.177V2.818C11 2.045 9.955 1 8.5 1S6 2.045 6 2.818z" />
            </svg>
          </button>
        </div>
      </div>
    </article>
  )
}

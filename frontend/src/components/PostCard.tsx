import { Link } from '@tanstack/react-router'
import { useMutation, useQuery } from '@apollo/client/react'
import { useEffect, useRef, useState } from 'react'
import { Heart, MessageSquare, Share2, Bookmark, MoreHorizontal } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  CreateCommentDocument,
  PostCommentsDocument,
  ReactToPostDocument,
  UnreactToPostDocument,
} from '@/generated/graphql'
import type { PostCommentsQuery, ReactToPostMutation } from '@/generated/graphql'
import { refreshMediaUrl } from '@/lib/media-server'
import { BusyOverlay } from './BusyOverlay'
import ImageLightbox from './ImageLightbox'
import { useBusy } from '../hooks/useBusy'
import { formatCreatedAt } from '../lib/post-mappers'

type ThreadComment = PostCommentsQuery['postComments'][number]

interface PostCardProps {
  post: {
    id: string
    author: { name: string; username: string; avatar?: string; initials?: string }
    createdAt: string
    body: string
    media?: { id?: string; url: string; alt: string; kind: 'IMAGE' | 'VIDEO' }[]
    reactions: { count: number; myReaction?: string | null }
    comments: number
    shares: number
    edited?: boolean
  }
}

export default function PostCard({ post }: PostCardProps) {
  const postRef = { __typename: 'Post' as const, id: post.id }
  const isLiked = post.reactions.myReaction === 'LIKE'
  // Like and comment share one busy region, so they share one re-entry guard.
  const { busy, run } = useBusy()
  const [busyLabel, setBusyLabel] = useState('Updating…')

  // reactToPost only upserts/changes a reaction. Removing it is a separate
  // mutation, so the Like button has to choose based on the viewer's reaction.
  const [reactToPost] = useMutation(ReactToPostDocument, {
    // The generated mutation type omits __typename, but Apollo needs it to
    // normalize the optimistic ReactionSummary in the cache.
    optimisticResponse: {
      reactToPost: {
        __typename: 'ReactionSummary',
        count: post.reactions.count + (post.reactions.myReaction ? 0 : 1),
        myReaction: 'LIKE',
      },
    } as ReactToPostMutation,
    update(cache, { data }) {
      const result = data?.reactToPost
      if (!result) return
      // Update the normalized Post, so every feed query sees the new count.
      cache.modify({
        id: cache.identify(postRef),
        fields: {
          reactionCount() { return result.count },
          myReaction() { return result.myReaction },
        },
      })
    },
  })
  const [unreactToPost] = useMutation(UnreactToPostDocument, {
    optimisticResponse: { unreactToPost: true },
    update(cache) {
      cache.modify({
        id: cache.identify(postRef),
        fields: {
          reactionCount(count: number) { return Math.max(0, count - 1) },
          myReaction() { return null },
        },
      })
    },
  })
  const [createComment] = useMutation(CreateCommentDocument, {
    optimisticResponse: (vars) => ({
      createComment: {
        id: `temp-${Date.now()}`,
        postId: vars.postId,
        body: vars.body,
        createdAt: new Date().toISOString(),
        author: {
          id: 'me',
          username: 'me',
          displayName: 'You',
          avatarMediaId: null,
          __typename: 'Member',
        },
        reactionCount: 0,
        myReaction: null,
        __typename: 'Comment',
      },
    }),
    update(cache, { data }) {
      const comment = data?.createComment
      if (!comment) return
      cache.modify({
        id: cache.identify(postRef),
        fields: {
          commentCount(count: number) { return count + 1 },
        },
      })
      cache.updateQuery(
        { query: PostCommentsDocument, variables: { postId: post.id, first: 50 } },
        (existing) => {
          if (!existing || existing.postComments.some((c) => c.id === comment.id)) return existing
          const added = {
            __typename: 'Comment',
            id: comment.id,
            body: comment.body,
            createdAt: comment.createdAt,
            author: comment.author,
          }
          return {
            ...existing,
            postComments: [...existing.postComments, added as (typeof existing.postComments)[number]],
          }
        },
      )
    },
  })
  // Signed media URLs are short-lived. A long-open feed can hit an expiry;
  // ask the BFF to re-sign that one asset exactly once, then swap its src.
  const [refreshedUrls, setRefreshedUrls] = useState<Record<number, string>>({})
  const refreshedOnce = useRef<Set<number>>(new Set())

  useEffect(() => {
    refreshedOnce.current.clear()
    setRefreshedUrls({})
  }, [post.id, post.media?.[0]?.url])

  const handleMediaError = async (index: number) => {
    const media = post.media?.[index]
    if (!media?.id || refreshedOnce.current.has(index)) return
    refreshedOnce.current.add(index)
    try {
      const fresh = await refreshMediaUrl({ data: { mediaId: media.id } })
      setRefreshedUrls((current) => ({ ...current, [index]: fresh.url }))
    } catch (error) {
      console.error('Failed to refresh media URL:', error)
    }
  }

  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null)
  const lightboxImages = (post.media ?? [])
    .map((media, index) => ({ media, index }))
    .filter(({ media }) => media.kind === 'IMAGE')
    .map(({ media, index }) => ({
      url: refreshedUrls[index] ?? media.url,
      alt: media.alt,
    }))

  const [showComments, setShowComments] = useState(false)
  const [showCommentInput, setShowCommentInput] = useState(false)
  const [commentText, setCommentText] = useState('')
  const [postedComments, setPostedComments] = useState<ThreadComment[]>([])
  const { data: commentsData, loading: commentsLoading } = useQuery(PostCommentsDocument, {
    variables: { postId: post.id, first: 50 },
    skip: !showComments,
  })
  // Keep just-posted comments in local state as well as the cache: the cache
  // write can race the in-flight PostComments query and be overwritten by its
  // older result, which would make the new comment disappear from the thread.
  const comments = [...(commentsData?.postComments ?? []), ...postedComments].filter(
    (comment, index, all) => all.findIndex((other) => other.id === comment.id) === index,
  )

  const handleLike = async () => {
    setBusyLabel('Updating…')
    await run(async () => {
      try {
        if (isLiked) {
          await unreactToPost({ variables: { id: post.id } })
        } else {
          await reactToPost({ variables: { id: post.id, type: 'LIKE' } })
        }
      } catch (error) {
        console.error('Failed to update reaction:', error)
      }
    })
  }

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!commentText.trim()) return
    setBusyLabel('Saving…')
    await run(async () => {
      try {
        const { data } = await createComment({
          variables: { postId: post.id, body: commentText.trim() },
        })
        const created = data?.createComment
        if (created) {
          setPostedComments((current) => [
            ...current,
            {
              id: created.id,
              body: created.body,
              createdAt: created.createdAt,
              author: {
                id: created.author.id,
                username: created.author.username,
                displayName: created.author.displayName,
              },
            },
          ])
        }
        setCommentText('')
        setShowCommentInput(false)
      } catch (error) {
        console.error('Failed to create comment:', error)
      }
    })
  }

  return (
    <article className="island-shell rise-in rounded-xl overflow-hidden border border-[var(--line)] bg-white shadow-sm" style={{ animationDelay: '80ms' }}>
      <div className="flex gap-3 p-4 sm:p-5">
        <div className="flex-shrink-0">
          <Link to="/profile/$username" params={{ username: post.author.username }} className="flex items-center gap-3">
            <Avatar className="h-8 w-8" aria-label={post.author.name}>
              {post.author.avatar && <AvatarImage src={post.author.avatar} alt={post.author.name} />}
              <AvatarFallback name={post.author.name} />
            </Avatar>
            <div className="flex flex-col leading-none">
              <span className="text-sm font-semibold text-[var(--sea-ink)] block">{post.author.name}</span>
              <span className="text-[0.75rem] text-[var(--sea-ink-soft)] block">{post.createdAt} · public</span>
            </div>
          </Link>
        </div>

        <Button variant="ghost" size="icon" className="ml-auto text-[var(--sea-ink-soft)]" aria-label="More options">
          <MoreHorizontal className="h-4 w-4" aria-hidden="true" />
        </Button>
      </div>

      {post.body && (
        <div className="px-4 sm:px-5 pt-1 sm:pt-0 pb-3">
          <p className="m-0 text-base text-[var(--sea-ink)] leading-7">{post.body}</p>
        </div>
      )}

      {post.media && post.media.length > 0 && (
        <div className="px-4 sm:px-5 pb-5 sm:pb-0">
          <div className="aspect-video rounded-lg overflow-hidden border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)70%,white_30%)]">
            {post.media[0].kind === 'VIDEO' ? (
              <video
                src={refreshedUrls[0] ?? post.media[0].url}
                aria-label={post.media[0].alt}
                className="h-full w-full object-cover"
                controls
                preload="metadata"
                playsInline
                onError={() => void handleMediaError(0)}
              />
            ) : (
              <button
                type="button"
                aria-label={`Preview ${post.media[0].alt}`}
                onClick={() => setLightboxIndex(0)}
                className="block h-full w-full"
              >
                <img
                  src={refreshedUrls[0] ?? post.media[0].url}
                  alt={post.media[0].alt}
                  className="h-full w-full object-cover"
                  loading="lazy"
                  decoding="async"
                  onError={() => void handleMediaError(0)}
                />
              </button>
            )}
          </div>
        </div>
      )}

      <>
        <span className="sr-only" role="status" aria-live="polite">{busy ? busyLabel : ''}</span>
        <div
          className="relative px-4 sm:px-5 pt-4 pb-5 sm:pt-0 border-t border-[var(--line)]"
          aria-busy={busy}
        >
          <div inert={busy ? true : undefined}>
            <div className="flex items-baseline justify-between gap-4 sm:gap-6 mb-3">
              <div className="flex items-center gap-1 text-sm font-semibold text-[var(--sea-ink)]">
                <Heart className="h-4 w-4" aria-hidden="true" />
                <span className="ml-1">{post.reactions.count}</span>
              </div>
              <div className="flex items-center gap-4 sm:gap-6">
                <span className="text-[var(--sea-ink-soft)] text-sm">{post.comments} comments</span>
                <span className="text-[var(--sea-ink-soft)] text-sm">{post.shares} shares</span>
              </div>
            </div>
  
            <div className="flex items-center gap-5 sm:gap-8 pt-2">
              <Button
                variant="ghost"
                size="sm"
                className={`gap-1.5 text-sm font-medium transition hover:text-[var(--brand-dark)] ${isLiked ? 'text-[var(--brand-dark)]' : 'text-[var(--sea-ink-soft)]'}`}
                onClick={handleLike}
                aria-label={isLiked ? 'Unlike' : 'Like'}
                aria-pressed={isLiked}
                disabled={busy}
              >
                <Heart className="h-4 w-4" aria-hidden="true" />
                {isLiked ? 'Unlike' : 'Like'}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="gap-1.5 text-sm font-medium text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]"
                onClick={() => {
                  setShowComments(true)
                  setShowCommentInput(true)
                }}
                aria-label="Comment"
                disabled={busy}
              >
                <MessageSquare className="h-4 w-4" aria-hidden="true" />
                Comment
              </Button>
              <Button
                variant="ghost"
                size="sm"
                className="gap-1.5 text-sm font-medium text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]"
                aria-label="Share"
                disabled={busy}
              >
                <Share2 className="h-4 w-4" aria-hidden="true" />
                Share
              </Button>
              <Button variant="ghost" size="icon" className="ml-auto text-[var(--sea-ink-soft)]" aria-label="Save" disabled={busy}>
                <Bookmark className="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
  
            {showComments && (
              <div className="mt-4 border-t border-[var(--line)] pt-4" aria-label="Comments">
                {commentsLoading && comments.length === 0 && (
                  <p className="m-0 text-sm text-[var(--sea-ink-soft)]">Loading comments…</p>
                )}
                {!commentsLoading && comments.length === 0 && (
                  <p className="m-0 text-sm text-[var(--sea-ink-soft)]">No comments yet.</p>
                )}
                <ul className="m-0 list-none space-y-3 p-0">
                  {comments.map((comment) => (
                    <li key={comment.id} className="flex gap-2">
                      <Avatar size="sm" aria-label={comment.author.displayName}>
                        <AvatarFallback name={comment.author.displayName} />
                      </Avatar>
                      <div className="min-w-0">
                        <p className="m-0 text-xs font-semibold text-[var(--sea-ink)]">
                          {comment.author.displayName}
                          <span className="ml-2 font-normal text-[var(--sea-ink-soft)]">
                            {formatCreatedAt(comment.createdAt)}
                          </span>
                        </p>
                        <p className="m-0 text-sm leading-6 text-[var(--sea-ink)]">{comment.body}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            )}
  
            {showCommentInput && (
              <form onSubmit={handleCommentSubmit} className="mt-4 flex gap-2">
                <Input
                  type="text"
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder="Write a comment…"
                  className="flex-1"
                  autoFocus
                  disabled={busy}
                />
                <Button type="submit" className="gap-1.5" disabled={busy || !commentText.trim()}>
                  Post
                </Button>
                <Button variant="ghost" size="icon" onClick={() => setShowCommentInput(false)} aria-label="Cancel" disabled={busy}>
                  <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true"><path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                </Button>
              </form>
            )}
          </div>
          {busy && <BusyOverlay label={busyLabel} />}
        </div>
      </>
      <ImageLightbox
        images={lightboxImages}
        initialIndex={lightboxIndex ?? 0}
        open={lightboxIndex !== null}
        onClose={() => setLightboxIndex(null)}
      />
    </article>
  )
}

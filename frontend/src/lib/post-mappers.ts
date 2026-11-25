import type { FollowingFeedQuery } from '../generated/graphql'
import { gradientPlaceholder } from './placeholders'

type FeedPost = FollowingFeedQuery['followingFeed']['posts'][number]

export interface PostCardData {
  id: string
  author: { name: string; username: string; avatar?: string; initials?: string }
  createdAt: string
  body: string
  media?: { url: string; alt: string }[]
  reactions: { count: number; myReaction: FeedPost['myReaction'] }
  comments: number
  shares: number
  edited?: boolean
}

/** Map a backend Post to the presentational shape PostCard expects. */
export function toPostCardData(post: FeedPost): PostCardData {
  const type = post.type
  const initials = post.author.displayName
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)

  return {
    id: post.id,
    author: {
      name: post.author.displayName,
      username: post.author.username,
      initials: initials || post.author.username.slice(0, 2).toUpperCase(),
    },
    createdAt: formatCreatedAt(post.createdAt),
    body: post.body ?? '',
    media: type === 'NOTE' ? [] : [
      {
        url: gradientPlaceholder({
          colors: type === 'VIDEO' ? ['#4a6f8c', '#9db8c9'] : ['#6ba8e0', '#bcdcf5'],
        }),
        alt: 'Post media',
      },
    ],
    reactions: { count: post.reactionCount, myReaction: post.myReaction },
    comments: post.commentCount,
    shares: 0,
    edited: Boolean(post.editedAt),
  }
}

/** Coarse relative time — enough for a mock-free feed label. */
export function formatCreatedAt(iso: string): string {
  const timestamp = Date.parse(iso)
  if (Number.isNaN(timestamp)) return iso
  const seconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000))
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

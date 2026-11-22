import { describe, expect, it } from 'vitest'
import { formatCreatedAt, toPostCardData } from './post-mappers'
import type { FollowingFeedQuery } from '../generated/graphql'

type FeedPost = FollowingFeedQuery['followingFeed']['posts'][number]

function post(overrides: Partial<FeedPost> = {}): FeedPost {
  return {
    id: 'p1',
    type: 'NOTE',
    body: 'Hello team',
    editedAt: null,
    createdAt: '2026-11-21T10:00:00Z',
    commentCount: 3,
    reactionCount: 12,
    myReaction: null,
    isSaved: false,
    author: {
      id: '1',
      username: 'rojame',
      displayName: 'Rojame Serey',
      avatarMediaId: null,
    },
    ...overrides,
  }
}

describe('toPostCardData', () => {
  it('maps the author, counts and body', () => {
    const data = toPostCardData(post())

    expect(data.id).toBe('p1')
    expect(data.author.name).toBe('Rojame Serey')
    expect(data.author.username).toBe('rojame')
    expect(data.author.initials).toBe('RS')
    expect(data.body).toBe('Hello team')
    expect(data.reactions.count).toBe(12)
    expect(data.comments).toBe(3)
    expect(data.shares).toBe(0)
    expect(data.edited).toBe(false)
  })

  it('leaves the body empty when the post has none', () => {
    expect(toPostCardData(post({ body: null })).body).toBe('')
  })

  it('flags edited posts', () => {
    expect(toPostCardData(post({ editedAt: '2026-11-21T11:00:00Z' })).edited).toBe(true)
  })

  it('adds no media for a text post but one for a photo post', () => {
    expect(toPostCardData(post({ type: 'NOTE' })).media).toEqual([])
    expect(toPostCardData(post({ type: 'PHOTO' })).media).toHaveLength(1)
    expect(toPostCardData(post({ type: 'VIDEO' })).media).toHaveLength(1)
  })

  it('falls back to the username for the initials', () => {
    const data = toPostCardData(
      post({ author: { ...post().author, displayName: '' } }),
    )
    expect(data.author.initials).toBe('RO')
  })
})

describe('formatCreatedAt', () => {
  it('formats recent timestamps as relative times', () => {
    const now = Date.now()

    expect(formatCreatedAt(new Date(now - 5_000).toISOString())).toBe('just now')
    expect(formatCreatedAt(new Date(now - 5 * 60_000).toISOString())).toBe('5m ago')
    expect(formatCreatedAt(new Date(now - 3 * 3_600_000).toISOString())).toBe('3h ago')
    expect(formatCreatedAt(new Date(now - 2 * 86_400_000).toISOString())).toBe('2d ago')
  })

  it('keeps unparsable timestamps as they are', () => {
    expect(formatCreatedAt('not-a-date')).toBe('not-a-date')
  })

  it('never reports a negative age', () => {
    const future = new Date(Date.now() + 60_000).toISOString()
    expect(formatCreatedAt(future)).toBe('just now')
  })
})

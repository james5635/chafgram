import { gql } from '@apollo/client'
import type { TypedDocumentNode } from '@apollo/client'
import { useQuery } from '@apollo/client/react'
import { MockedProvider } from '@apollo/client/testing/react'
import { vi, describe, it, expect } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import {
  CreateCommentDocument,
  PostCommentsDocument,
  ReactToPostDocument,
  UnreactToPostDocument,
} from '@/generated/graphql'
import PostCard from './PostCard'

// TanStack Router's Link requires router context; mock it as a plain anchor
// so PostCard can be tested in isolation.
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ to, params, children }: Record<string, any>) => {
      let href = String(to)
      if (params) {
        for (const [key, value] of Object.entries(params)) {
          href = href.replace(`$${key}`, String(value)).replace(`:${key}`, String(value))
        }
      }
      return <a href={href}>{children}</a>
    },
  }
})

const post = {
  id: '1',
  author: { name: 'Dara', username: 'dara', initials: 'DR' },
  createdAt: '2h ago',
  body: 'A beautiful day to build something great! 🚀',
  media: [{ url: 'https://example.com/img.jpg', alt: 'Mountain landscape', kind: 'IMAGE' as const }],
  reactions: { count: 248, myReaction: null },
  comments: 32,
  shares: 12,
}

function renderPostCard(ui: ReactNode) {
  return render(<MockedProvider mocks={[]}>{ui}</MockedProvider>)
}

describe('PostCard', () => {
  it('renders author, body, and engagement counts', () => {
    renderPostCard(<PostCard post={post} />)

    expect(screen.getByText('Dara')).toBeInTheDocument()
    expect(
      screen.getByText('A beautiful day to build something great! 🚀')
    ).toBeInTheDocument()
    expect(screen.getByText('248')).toBeInTheDocument()
    expect(screen.getByText('32 comments')).toBeInTheDocument()
    expect(screen.getByText('12 shares')).toBeInTheDocument()
  })

  it('shows one heart reaction icon and no duplicate thumbs-up', () => {
    renderPostCard(<PostCard post={post} />)

    const reactionGroup = screen.getByText('248').parentElement
    expect(reactionGroup).not.toBeNull()
    expect(reactionGroup?.querySelector('.lucide-thumbs-up')).toBeNull()
    expect(reactionGroup?.querySelectorAll('.lucide-heart')).toHaveLength(1)
  })

  it('puts the like count on the left and comment/share counts on the right', () => {
    renderPostCard(<PostCard post={post} />)

    const likeGroup = screen.getByText('248').parentElement
    const detailsGroup = screen.getByText('32 comments').parentElement
    expect(detailsGroup).toBe(screen.getByText('12 shares').parentElement)
    expect(likeGroup?.parentElement).toBe(detailsGroup?.parentElement)
    expect(detailsGroup?.previousElementSibling).toBe(likeGroup)
    expect(likeGroup?.parentElement).toHaveClass('justify-between')
  })

  it('renders the media image with alt text', () => {
    renderPostCard(<PostCard post={post} />)

    expect(screen.getByAltText('Mountain landscape')).toBeInTheDocument()
  })

  it('renders a video attachment with native controls', () => {
    renderPostCard(
      <PostCard
        post={{
          ...post,
          id: 'video-1',
          media: [{ url: '/api/media/9', alt: 'Post video', kind: 'VIDEO' as const }],
        }}
      />,
    )

    const video = screen.getByLabelText('Post video')
    expect(video.tagName).toBe('VIDEO')
    expect(video).toHaveAttribute('src', '/api/media/9')
    expect(video).toHaveProperty('controls', true)
  })

  it('links the author name to their profile with interpolated username', () => {
    renderPostCard(<PostCard post={post} />)

    const link = screen.getByRole('link')
    expect(link).toHaveAttribute('href', '/profile/dara')
  })

  it('renders Like, Comment, Share and Save actions', () => {
    renderPostCard(<PostCard post={post} />)

    for (const label of ['Like', 'Comment', 'Share']) {
      expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
    }
    // Save is an icon-only button, check for aria-label
    expect(screen.getByLabelText('Save')).toBeInTheDocument()
  })

  it('renders a post with no media', () => {
    renderPostCard(
      <PostCard
        post={{
          ...post,
          id: '2',
          media: [],
        }}
      />
    )

    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText('A beautiful day to build something great! 🚀')).toBeInTheDocument()
  })
})

// Feed props come from the Apollo cache, like in the real home screen. These
// tests catch stale counts and mutation/cache mismatches that a static prop
// would hide.
interface TestFeedData {
  followingFeed: {
    posts: Array<{
      id: string
      reactionCount: number
      commentCount: number
      myReaction: string | null
    }>
  }
}

// Apollo 4's gql template returns an untyped DocumentNode, so give the test
// query a typed document to keep useQuery's data shape checked.
const TEST_FEED = gql`
  query TestFeed($first: Int!) {
    followingFeed(first: $first) {
      posts {
        id
        reactionCount
        commentCount
        myReaction
      }
    }
  }
` as TypedDocumentNode<TestFeedData, { first: number }>

function feedMock(myReaction: string | null, reactionCount: number, commentCount = 32) {
  return {
    request: { query: TEST_FEED, variables: { first: 20 } },
    result: {
      data: {
        followingFeed: {
          __typename: 'FeedPage',
          posts: [
            {
              __typename: 'Post',
              id: '1',
              reactionCount,
              commentCount,
              myReaction,
            },
          ],
        },
      },
    },
  }
}

function FeedPostCard() {
  const { data } = useQuery(TEST_FEED, { variables: { first: 20 } })
  const feedPost = data?.followingFeed?.posts?.[0]
  if (!feedPost) return <p>Loading feed</p>

  return (
    <PostCard
      post={{
        id: feedPost.id,
        author: { name: 'Dara', username: 'dara', initials: 'DR' },
        createdAt: '2h ago',
        body: 'A beautiful day to build something great! 🚀',
        reactions: { count: feedPost.reactionCount, myReaction: feedPost.myReaction },
        comments: feedPost.commentCount,
        shares: 12,
      }}
    />
  )
}

const author = {
  __typename: 'Member',
  id: '2',
  username: 'dara',
  displayName: 'Dara',
  avatarMediaId: null,
}

describe('PostCard engagement', () => {
  it('unlikes a post when the liked button is clicked again', async () => {
    render(
      <MockedProvider
        mocks={[
          feedMock('LIKE', 249),
          {
            request: { query: UnreactToPostDocument, variables: { id: '1' } },
            result: { data: { unreactToPost: true } },
          },
        ]}
      >
        <FeedPostCard />
      </MockedProvider>,
    )

    const unlike = await screen.findByLabelText('Unlike')
    expect(screen.getByText('249')).toBeInTheDocument()

    fireEvent.click(unlike)

    expect(await screen.findByLabelText('Like')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('248')).toBeInTheDocument())
  })

  it('likes an unreacted post and updates the cached count', async () => {
    render(
      <MockedProvider
        mocks={[
          feedMock(null, 248),
          {
            request: { query: ReactToPostDocument, variables: { id: '1', type: 'LIKE' } },
            result: {
              data: {
                reactToPost: {
                  __typename: 'ReactionSummary',
                  count: 249,
                  myReaction: 'LIKE',
                },
              },
            },
          },
        ]}
      >
        <FeedPostCard />
      </MockedProvider>,
    )

    const like = await screen.findByLabelText('Like')
    fireEvent.click(like)

    expect(await screen.findByLabelText('Unlike')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('249')).toBeInTheDocument())
  })

  it('shows the comment thread and a newly posted comment', async () => {
    render(
      <MockedProvider
        mocks={[
          feedMock(null, 248, 32),
          {
            request: { query: PostCommentsDocument, variables: { postId: '1', first: 50 } },
            result: {
              data: {
                postComments: [
                  {
                    __typename: 'Comment',
                    id: 'c1',
                    body: 'Nice work',
                    createdAt: '2026-11-25T10:00:00Z',
                    author,
                  },
                ],
              },
            },
          },
          {
            request: {
              query: CreateCommentDocument,
              variables: { postId: '1', body: 'Looks good' },
            },
            result: {
              data: {
                createComment: {
                  __typename: 'Comment',
                  id: 'c2',
                  postId: '1',
                  body: 'Looks good',
                  createdAt: '2026-11-25T11:00:00Z',
                  author,
                  reactionCount: 0,
                  myReaction: null,
                },
              },
            },
          },
        ]}
      >
        <FeedPostCard />
      </MockedProvider>,
    )

    fireEvent.click(await screen.findByLabelText('Comment'))

    expect(await screen.findByText('Nice work')).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('Write a comment…'), {
      target: { value: 'Looks good' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Post' }))

    // The composer closes only after the mutation has resolved, which avoids
    // asserting against the transient optimistic cache layer.
    await waitFor(() =>
      expect(screen.queryByPlaceholderText('Write a comment…')).not.toBeInTheDocument(),
    )
    expect(screen.getByText('Looks good')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('33 comments')).toBeInTheDocument())
  })
})
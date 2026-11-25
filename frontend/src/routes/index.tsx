import { useMutation, useQuery } from '@apollo/client/react'
import { createFileRoute } from '@tanstack/react-router'
import AppShell from '../components/AppShell'
import Composer from '../components/Composer'
import PostCard from '../components/PostCard'
import RightRail from '../components/RightRail'
import { CreatePostDocument, FollowingFeedDocument } from '../generated/graphql'
import { useSessionGuard } from '../hooks/useSessionGuard'
import { toPostCardData } from '../lib/post-mappers'

export const Route = createFileRoute('/')({
  component: Home,
})

function Home() {
  const { me } = useSessionGuard()
  const { data, loading, error } = useQuery(FollowingFeedDocument, {
    variables: { first: 20 },
  })
  const [createPost, { error: publishError }] = useMutation(CreatePostDocument, {
    refetchQueries: [FollowingFeedDocument],
  })

  const posts = (data?.followingFeed?.posts ?? []).map(toPostCardData)

  async function publish(body: string): Promise<boolean> {
    try {
      await createPost({ variables: { type: 'NOTE', body } })
      return true
    } catch {
      return false
    }
  }

  return (
    <AppShell rightRail={<RightRail />}>
      <div className="px-4 pb-8 pt-4 sm:pt-6">
        <div className="page-wrap">
          <Composer
            member={{
              name: me?.displayName ?? 'You',
              username: me?.username ?? 'me',
              initials: me ? me.displayName.slice(0, 2).toUpperCase() : 'YD',
              avatar: undefined,
            }}
            onSubmit={publish}
          />

          {publishError && (
            <p className="mt-3 text-sm text-[#9f3030]" role="alert">
              Could not publish your post. Please try again.
            </p>
          )}

          <section className="mt-8 space-y-6" aria-busy={loading}>
            {error && (
              <div className="rounded-2xl border border-[rgba(196,71,71,0.3)] bg-[rgba(196,71,71,0.1)] p-5 text-sm text-[#9f3030]">
                Could not load your feed: {error.message}
              </div>
            )}

            {loading && !data && (
              <>
                {[0, 1, 2].map((n) => (
                  <div key={n} className="island-shell h-48 animate-pulse rounded-xl" />
                ))}
              </>
            )}

            {!loading && !error && posts.length === 0 && (
              <div className="island-shell rounded-xl p-8 text-center">
                <p className="island-kicker mb-2">Nothing here yet</p>
                <p className="m-0 text-sm text-[var(--sea-ink-soft)]">
                  Follow a few colleagues or be the first to share something with your workspace.
                </p>
              </div>
            )}

            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </section>
        </div>
      </div>
    </AppShell>
  )
}

import { createFileRoute } from '@tanstack/react-router'
import Composer from '../components/Composer'
import PostCard from '../components/PostCard'
import LeftRail from '../components/LeftRail'
import RightRail from '../components/RightRail'

export const Route = createFileRoute('/')({
  component: Home,
})

const MEMBER = { name: 'You', username: 'me', initials: 'YD', avatar: undefined }

const POSTS = [
  {
    id: '1',
    author: { name: 'Dara', username: 'dara', avatar: undefined, initials: 'DR' },
    createdAt: '2h ago',
    body: 'A beautiful day to build something great! 🚀',
    media: [{ url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80', alt: 'Mountain landscape' }],
    reactions: { count: 248 },
    comments: 32,
    shares: 12,
    edited: false,
  },
  {
    id: '2',
    author: { name: 'Sokha', username: 'sokha', avatar: undefined, initials: 'SK' },
    createdAt: '4h ago',
    body: 'Learning never stops. 💙',
    media: [],
    reactions: { count: 89 },
    comments: 14,
    shares: 6,
    edited: false,
  },
]

function Home() {
  return (
    <div className="flex min-h-[calc(100vh-14rem)]">
      <LeftRail />

      <div className="flex-1 pt-4 sm:pt-6">
        <div className="page-wrap px-4 pb-8">
          <Composer member={MEMBER} />

          <section className="mt-8 space-y-6">
            {POSTS.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
            <div className="h-24 flex items-center justify-center">
              <p className="text-[var(--sea-ink-soft)] text-sm">End of feed</p>
            </div>
          </section>
        </div>
      </div>

      <RightRail />
    </div>
  )
}

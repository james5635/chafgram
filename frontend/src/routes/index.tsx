import { createFileRoute } from '@tanstack/react-router'
import Composer from '../components/Composer'
import PostCard from '../components/PostCard'

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
  {
    id: '3',
    author: { name: 'Mey', username: 'mey', avatar: undefined, initials: 'MY' },
    createdAt: '6h ago',
    body: 'Just shipped the new onboarding flow. What do you think?',
    media: [
      { url: 'https://images.unsplash.com/photo-1460925895917-afc68e22c688?w=800&q=80', alt: 'Laptop on desk' },
      { url: 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&q=80', alt: 'Code on screen' },
    ],
    reactions: { count: 156 },
    comments: 28,
    shares: 9,
    edited: true,
  },
]

function Home() {
  return (
    <main className="page-wrap px-4 pb-8 pt-14">
      <Composer member={MEMBER} />

      <section className="mt-8 space-y-6">
        {POSTS.map((post) => (
          <PostCard key={post.id} post={post} />
        ))}
        <div className="h-24 flex items-center justify-center">
          <p className="text-[var(--sea-ink-soft)] text-sm">End of feed</p>
        </div>
      </section>
    </main>
  )
}

import { createFileRoute } from '@tanstack/react-router'
import Composer from '../components/Composer'
import PostCard from '../components/PostCard'
import LeftRail from '../components/LeftRail'
import RightRail from '../components/RightRail'

export const Route = createFileRoute('/')({
  component: Home,
})

const MEMBER = { name: 'You', username: 'me', initials: 'YD', avatar: undefined }

const LANDSCAPE = `data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 450">
<defs>
<linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8ec5f7"/><stop offset="1" stop-color="#cfe8ff"/></linearGradient>
<linearGradient id="m1" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#6b9bd2"/><stop offset="1" stop-color="#3f6fa8"/></linearGradient>
<linearGradient id="m2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3f6fa8"/><stop offset="1" stop-color="#244a78"/></linearGradient>
<linearGradient id="water" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#5a8fc7"/><stop offset="1" stop-color="#a9ccef"/></linearGradient>
</defs>
<rect width="800" height="450" fill="url(#sky)"/>
<circle cx="640" cy="100" r="38" fill="#fff5de" opacity="0.95"/>
<path d="M0 260 L120 180 L230 250 L360 160 L500 240 L620 150 L800 240 L800 320 L0 320 Z" fill="url(#m1)"/>
<path d="M0 300 L160 230 L300 300 L450 220 L600 290 L760 210 L800 250 L800 330 L0 330 Z" fill="url(#m2)"/>
<rect y="320" width="800" height="130" fill="url(#water)"/>
<path d="M0 322 Q200 335 400 322 T800 322" fill="none" stroke="#ffffff" stroke-opacity="0.25" stroke-width="3"/>
<ellipse cx="640" cy="360" rx="30" ry="6" fill="#fff5de" opacity="0.35"/>
</svg>`)}`

const POSTS = [
  {
    id: '1',
    author: { name: 'Dara', username: 'dara', avatar: undefined, initials: 'DR' },
    createdAt: '2h ago',
    body: 'A beautiful day to build something great! 🚀',
    media: [{ url: LANDSCAPE, alt: 'Mountain landscape' }],
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
    <>
      <LeftRail />

      <div className="flex min-h-[calc(100vh-14rem)] lg:pl-64">
        <div className="flex-1 min-w-0 pt-4 sm:pt-6">
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
    </>
  )
}

import { createFileRoute, Link } from '@tanstack/react-router'

export const Route = createFileRoute('/welcome')({
  component: Welcome,
})

export function Welcome() {
  return (
    <main className="min-h-screen bg-[var(--bg-base)] px-4 py-4 sm:py-6">
      <section className="island-shell rise-in relative mx-auto min-h-[calc(100vh-4rem)] max-w-[1500px] overflow-hidden rounded-xl">
        <div className="pointer-events-none absolute -right-10 top-20 h-96 w-96 rounded-full bg-[radial-gradient(circle,rgba(147,197,253,0.55),transparent_65%)]" />
        <svg className="pointer-events-none absolute inset-x-0 bottom-0 h-56 w-full" viewBox="0 0 1600 300" preserveAspectRatio="none" aria-hidden="true">
          <path d="M0 170c190-68 300 13 475-10s237-111 429-82 265 62 395 28 212-86 301-41v235H0Z" fill="#dbeafe" opacity=".62" />
          <path d="M0 229c210-53 325-22 498-38s286-72 470-45 276 52 408 31 170-47 224-32v155H0Z" fill="#bfdbfe" opacity=".54" />
          <path d="M0 270c236-30 369-15 583-29s311-35 496-20 340 35 521 12v67H0Z" fill="#eff6ff" opacity=".8" />
        </svg>

        <nav className="relative z-10 flex items-center justify-between gap-4 px-6 py-5 sm:px-12">
          <Link to="/welcome" className="flex items-center gap-2 no-underline">
            <span className="h-7 w-7 rounded-full bg-[var(--brand)] flex items-center justify-center text-white font-bold text-base shadow-lg shadow-[var(--brand)]/30">
              C
            </span>
            <span className="text-lg font-bold tracking-tight text-[var(--sea-ink)]">Chafgram</span>
          </Link>

          <div className="hidden items-center gap-7 text-sm font-medium text-[var(--sea-ink-soft)] md:flex">
            <Link to="/" className="no-underline transition hover:text-[var(--brand-dark)]">Home</Link>
            <Link to="/explore" className="no-underline transition hover:text-[var(--brand-dark)]">Explore</Link>
            <a href="#about" className="no-underline transition hover:text-[var(--brand-dark)]">About</a>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <Link
              to="/login"
              className="rounded-full border border-[var(--line)] bg-white px-4 py-1.5 text-xs font-semibold text-[var(--brand-dark)] no-underline transition hover:bg-[var(--brand-light)] sm:px-5 sm:py-2 sm:text-sm"
            >
              Sign In
            </Link>
            <Link
              to="/login"
              className="rounded-full border border-[var(--brand)] bg-[var(--brand)] px-4 py-1.5 text-xs font-bold text-white no-underline shadow-lg shadow-[var(--brand)]/30 transition hover:bg-[var(--brand-dark)] sm:px-5 sm:py-2 sm:text-sm"
            >
              Get Started
            </Link>
          </div>
        </nav>

        <div id="about" className="relative z-10 grid items-center gap-8 px-6 pb-16 pt-8 sm:px-12 lg:grid-cols-2 lg:gap-4 lg:pt-12">
          <div className="max-w-xl">
            <h1 className="display-title m-0 text-5xl font-bold leading-[1.02] tracking-tight text-[var(--sea-ink)] sm:text-6xl lg:text-7xl">
              Connect. Share.<br />Belong.
            </h1>
            <p className="mt-6 max-w-md text-lg leading-8 text-[var(--sea-ink-soft)]">
              Join Chafgram and be part of a vibrant community.
            </p>
            <div className="mt-8 flex flex-col gap-5 sm:flex-row sm:items-center sm:gap-8">
              <Link
                to="/login"
                className="inline-flex items-center justify-center gap-2 rounded-full border border-[var(--brand)] bg-[var(--brand)] px-7 py-3 text-sm font-bold text-white no-underline shadow-lg shadow-[var(--brand)]/30 transition hover:bg-[var(--brand-dark)]"
              >
                Get Started <span aria-hidden="true">→</span>
              </Link>
              <a href="#about" className="inline-flex items-center gap-1.5 text-sm font-semibold text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--brand-dark)]">
                Learn More <span aria-hidden="true">↓</span>
              </a>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-lg" aria-hidden="true">
            <svg viewBox="0 0 520 380" className="h-auto w-full">
              <path d="M40 300c30-70 120-110 220-100s160 60 200 130" fill="none" stroke="#dbeafe" strokeWidth="60" strokeLinecap="round" opacity="0.7" />
              <circle cx="300" cy="120" r="90" fill="#dbeafe" opacity="0.5" />
              <circle cx="120" cy="200" r="46" fill="#bfdbfe" opacity="0.6" />

              <g>
                <rect x="80" y="120" width="170" height="92" rx="22" fill="#3b82f6" />
                <rect x="104" y="146" width="92" height="9" rx="4.5" fill="#ffffff" opacity="0.9" />
                <rect x="104" y="168" width="122" height="9" rx="4.5" fill="#ffffff" opacity="0.7" />
                <path d="M100 212l-16 30 44-30z" fill="#3b82f6" />
              </g>

              <g>
                <rect x="250" y="210" width="180" height="96" rx="22" fill="#60a5fa" />
                <rect x="276" y="238" width="100" height="9" rx="4.5" fill="#ffffff" opacity="0.9" />
                <rect x="276" y="260" width="132" height="9" rx="4.5" fill="#ffffff" opacity="0.7" />
                <path d="M404 306l30 26-4-40z" fill="#60a5fa" />
              </g>

              <g>
                <circle cx="356" cy="96" r="30" fill="#3b82f6" />
                <path d="M314 172c0-23 19-42 42-42s42 19 42 42z" fill="#3b82f6" />
                <circle cx="418" cy="112" r="22" fill="#60a5fa" />
                <path d="M386 172c0-18 14-32 32-32s32 14 32 32z" fill="#60a5fa" />
              </g>
            </svg>
          </div>
        </div>

      </section>
    </main>
  )
}

import { createFileRoute, Link } from '@tanstack/react-router'

export const Route = createFileRoute('/welcome')({
  component: Welcome,
})

function Welcome() {
  return (
    <main className="min-h-[calc(100vh-13rem)] flex items-center justify-center px-4 pb-8 pt-14">
      <section className="island-shell rise-in rounded-2xl p-8 sm:p-12 max-w-2xl w-full text-center">
        <div className="flex flex-col items-center gap-4 mb-6">
          <span className="inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow-[0_8px_24px_rgba(30,90,72,0.08)]">
            <span className="h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]" />
            Chafgram
          </span>
          <p className="island-kicker mb-1">Private social for your company</p>
          <h1 className="display-title mb-4 text-4xl font-bold text-[var(--sea-ink)] leading-[1.02] sm:text-6xl">
            Connect. Share. Belong.
          </h1>
          <p className="m-0 max-w-lg text-base leading-8 text-[var(--sea-ink-soft)] sm:text-lg">
            Your company's internal social space — short video, posts, comments,
            follows, and direct messages — running on your own servers.
          </p>
        </div>

        <div className="relative h-64 sm:h-80 rounded-xl overflow-hidden border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)80%,white_20%)] mb-8">
          <div className="inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(79,184,178,0.16),transparent_70%)]" />
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-[var(--sea-ink-soft)]">
            <svg viewBox="0 0 64 64" className="h-32 w-32" aria-hidden="true"><path fill="currentColor" d="M32 2c-3.2 0-6.3.4-9.3 1.2-5.6 1.5-10.8 5.6-14.2 11.2-2.1 3.4-3.3 7.3-3.3 11.3s1.2 7.8 3.3 11.3c3.4 5.6 8.6 9.7 14.2 11.2 3 0.8 6.1 1.2 9.3 1.2s6.3-.4 9.3-1.2c5.6-1.5 10.8-5.6 14.2-11.2 2.1-3.4 3.3-7.3 3.3-11.3s-1.2-7.8-3.3-11.3c-3.4-5.6-8.6-9.7-14.2-11.2-3-0.8-6.1-1.2-9.3-1.2zm0 58c-1.7 0-3.3-.2-4.8-.6-2.8-.8-5.4-2.8-7.1-5.6-1-1.7-1.6-3.6-1.6-5.6s.6-3.9 1.6-5.6c1.7-2.8 4.3-4.8 7.1-5.6 1.5-.4 3.1-.6 4.8-.6s3.3.2 4.8.6c2.8.8 5.4 2.8 7.1 5.6 1 1.7 1.6 3.6 1.6 5.6s-.6 3.9-1.6 5.6c-1.7 2.8-4.3 4.8-7.1 5.6-1.5.4-3.1.6-4.8.6z"/></svg>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/login"
            className="flex-1 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] no-underline transition hover:bg-[color-mix(in_oklab,var(--lagoon)30%,var(--surface-strong))] sm:flex-none sm:w-48"
          >
            Sign in
          </Link>
          <Link
            to="/welcome"
            className="flex-1 rounded-full border border-[var(--line)] bg-transparent px-6 py-3 text-sm font-semibold text-[var(--sea-ink-soft)] no-underline transition hover:bg-[var(--link-bg-hover)] sm:flex-none sm:w-48"
          >
            Contact your admin
          </Link>
        </div>

        <p className="mt-8 text-[0.75rem] text-[var(--sea-ink-soft)]">
          New to Chafgram? Your admin creates your account via invite. Signup is
          invite-only by default.
        </p>
      </section>
    </main>
  )
}

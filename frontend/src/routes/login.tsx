import { createFileRoute, Link } from '@tanstack/react-router'
import { useState } from 'react'

export const Route = createFileRoute('/login')({
  component: Login,
})

function Login() {
  const [mode, setMode] = useState<'passkey' | 'password'>('passkey')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  return (
    <main className="min-h-[calc(100vh-13rem)] flex items-center justify-center px-4 pb-8 pt-14">
      <section className="island-shell rise-in rounded-2xl p-8 sm:p-12 max-w-md w-full">
        <div className="flex flex-col items-center gap-4 mb-6">
          <span className="inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow-[0_8px_24px_rgba(30,90,72,0.08)]">
            <span className="h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]" />
            Chafgram
          </span>
          <p className="island-kicker mb-1">Sign in to your workspace</p>
          <h1 className="display-title mb-2 text-3xl font-bold text-[var(--sea-ink)] leading-[1.02] sm:text-4xl">
            Welcome back
          </h1>
        </div>

        <div className="space-y-3">
          <button
            type="button"
            onClick={() => setMode('passkey')}
            className={`w-full rounded-full border px-6 py-3 text-sm font-semibold transition ${
              mode === 'passkey'
                ? 'border-[var(--chip-line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] text-[var(--sea-ink)]'
                : 'border-transparent bg-transparent text-[var(--sea-ink-soft)] hover:bg-[var(--link-bg-hover)]'
            }`}
          >
            <span className="mr-2">🔑</span>
            Sign in with passkey
          </button>

          <div className="mx-2 mt-1 mb-1 h-[1px] bg-[var(--line)]" />

          <button
            type="button"
            onClick={() => setMode('password')}
            className={`w-full rounded-full border px-6 py-3 text-sm font-semibold transition ${
              mode === 'password'
                ? 'border-[var(--chip-line)] bg-[var(--chip-bg)] text-[var(--sea-ink)]'
                : 'border-transparent bg-transparent text-[var(--sea-ink-soft)] hover:bg-[var(--link-bg-hover)]'
            }`}
          >
            Sign in with password
          </button>
        </div>

        {mode === 'password' && (
          <div className="mt-5 space-y-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-semibold text-[var(--sea-ink)]">Email or username</label>
              <input
                type="text"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
                className="demo-input"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-semibold text-[var(--sea-ink)]">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="demo-input"
              />
            </div>
            <button
              type="button"
              disabled={!email || !password}
              className="w-full rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] transition disabled:opacity-55 disabled:transform-none hover:!bg-[color-mix(in_oklab,var(--lagoon)30%,var(--surface-strong))]"
            >
              Continue
            </button>
          </div>
        )}

        <div className="mt-6 border-t border-[var(--line)] pt-6">
          <p className="mb-3 text-sm text-[var(--sea-ink-soft)]">
            Don't have an account?{' '}
            <span className="font-semibold">Contact your admin</span> to receive an invite.
          </p>
          <Link
            to="/welcome"
            className="text-sm font-semibold text-[var(--lagoon-deep)] no-underline transition hover:text-[var(--lagoon)]"
          >
            ← Back to welcome
          </Link>
        </div>
      </section>
    </main>
  )
}

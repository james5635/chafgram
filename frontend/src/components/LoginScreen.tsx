import { Link, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { setAccessToken } from '../lib/auth-client'
import { loginRequest } from '../lib/session-server'

/** Sign-in screen: password form wired to the token auth endpoint via the BFF. */
export default function LoginScreen() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function signIn(event: React.FormEvent) {
    event.preventDefault()
    if (!email || !password || pending) return

    setPending(true)
    setError(null)
    try {
      const result = await loginRequest({ data: { username: email, password } })
      if (result.ok) {
        // The refresh token never reaches JS; only the access token is held in
        // memory for the BFF server-function calls after navigation.
        if (result.accessToken) setAccessToken(result.accessToken)
        await navigate({ to: '/' })
      } else {
        setError(result.message ?? 'Could not sign you in.')
      }
    } catch {
      setError('Could not reach the server. Please try again.')
    } finally {
      setPending(false)
    }
  }

  return (
    <main className="min-h-[calc(100vh-13rem)] flex items-center justify-center px-4 pb-8 pt-14">
      <section className="island-shell rise-in rounded-xl p-8 sm:p-12 max-w-md w-full">
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

        <form className="mt-5 space-y-3" onSubmit={signIn}>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="login-username" className="text-sm font-semibold text-[var(--sea-ink)]">Email or username</label>
            <input
              id="login-username"
              type="text"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@company.com"
              autoComplete="username"
              className="demo-input"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="login-password" className="text-sm font-semibold text-[var(--sea-ink)]">Password</label>
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              className="demo-input"
            />
          </div>
          {error && (
            <p className="m-0 text-sm text-[#9f3030]" role="alert">
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={!email || !password || pending}
            className="w-full rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--lagoon)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] transition disabled:opacity-55 disabled:transform-none hover:!bg-[color-mix(in_oklab,var(--lagoon)30%,var(--surface-strong))]"
          >
            {pending ? 'Signing in…' : 'Continue'}
          </button>
        </form>

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

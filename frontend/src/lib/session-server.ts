import { createServerFn } from '@tanstack/react-start'
import { callBackend } from './backend-proxy'

export interface LoginInput {
  /** Username or email of the member. */
  username: string
  password: string
}

export interface LoginResult {
  ok: boolean
  message?: string
}

/**
 * Session ceremony (REST keep-list): form login against the backend.
 *
 * Spring Security answers a successful form login with a redirect and a
 * `JSESSIONID` cookie; the BFF forwards that cookie to the browser, so the
 * browser stays authenticated for every later GraphQL call.
 */
export const loginRequest = createServerFn({ method: 'POST' })
  .validator((input: LoginInput) => input)
  .handler(async ({ data }): Promise<LoginResult> => {
    const response = await callBackend({
      path: '/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        username: data.username,
        password: data.password,
      }).toString(),
    })

    const location = response.headers.get('location') ?? ''
    const redirected = response.status >= 300 && response.status < 400

    if (redirected && !location.includes('error')) {
      return { ok: true }
    }

    return { ok: false, message: 'Those credentials did not match an account.' }
  })

/** Ends the backend session and clears the session cookie. */
export const logoutRequest = createServerFn({ method: 'POST' })
  .handler(async (): Promise<{ ok: boolean }> => {
    const response = await callBackend({ path: '/logout', method: 'POST' })
    return { ok: response.status < 400 }
  })

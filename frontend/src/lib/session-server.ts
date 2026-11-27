import { createServerFn } from '@tanstack/react-start'
import { setResponseHeader } from '@tanstack/react-start/server'
import { RELOGIN_HEADER } from './auth-client'
import { callBackend, refreshAccessToken } from './backend-proxy'

export interface LoginInput {
  /** Username or email of the member. */
  username: string
  password: string
}

export interface LoginResult {
  ok: boolean
  message?: string
  /** Returned to browser memory; the refresh token stays in the HttpOnly cookie. */
  accessToken?: string
  tokenType?: string
  expiresIn?: number
}

/**
 * Session ceremony (REST keep-list): exchange credentials for a token pair.
 *
 * Spring Boot returns the access token in JSON and sets the refresh cookie;
 * `callBackend` copies that cookie to the browser. Only the access token is
 * exposed to JavaScript — the refresh token is never a server-function result.
 */
export const loginRequest = createServerFn({ method: 'POST' })
  .validator((input: LoginInput) => input)
  .handler(async ({ data }): Promise<LoginResult> => {
    const response = await callBackend({
      path: '/api/v1/auth/login',
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ username: data.username, password: data.password }),
    })

    if (!response.ok) {
      return { ok: false, message: 'Those credentials did not match an account.' }
    }

    const tokens = (await response.json()) as {
      accessToken?: unknown
      tokenType?: unknown
      expiresIn?: unknown
    }
    if (typeof tokens.accessToken !== 'string') {
      return { ok: false, message: 'Those credentials did not match an account.' }
    }

    return {
      ok: true,
      accessToken: tokens.accessToken,
      tokenType: typeof tokens.tokenType === 'string' ? tokens.tokenType : 'Bearer',
      expiresIn: typeof tokens.expiresIn === 'number' ? tokens.expiresIn : undefined,
    }
  })

/** Silent bootstrap: rotate the refresh cookie and hand the access token to JS. */
export const refreshRequest = createServerFn({ method: 'POST' }).handler(async () =>
  refreshAccessToken(),
)

/**
 * Revoke the refresh token. The access token is supplied by the BFF fetch as a
 * bearer; the response header makes the browser drop its in-memory token.
 */
export const logoutRequest = createServerFn({ method: 'POST' })
  .handler(async (): Promise<{ ok: boolean }> => {
    const response = await callBackend({ path: '/api/v1/auth/logout', method: 'POST' })
    if (response.ok) setResponseHeader(RELOGIN_HEADER, '1')
    return { ok: response.ok }
  })

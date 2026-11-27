# Access + refresh token authentication — design

Date: 2026-11-26
Status: implemented

The repo ignores `docs/superpowers/`, so this design lives under `docs/` to stay tracked.

## Intent and why

Replace the current `JSESSIONID` session + double-submit CSRF authentication with a
token pair:

- **Access token**: JWT, short-lived, never stored in the database. Sent by the
  browser to the BFF and forwarded to Spring Boot as `Authorization: Bearer ...`.
- **Refresh token**: opaque random string, long-lived, stored only as a SHA-256
  hash in MySQL. Kept by the browser in a `Secure`, `HttpOnly`, `SameSite=Strict`
  cookie and exchanged for a fresh token pair when the access token expires.

This keeps Chafgram self-hosted and first-party (no external identity provider),
while making the API stateless and giving short-lived credentials a bounded
window. The BFF remains the only egress point to Spring Boot; the backend URL
and the refresh cookie are never exposed to browser JavaScript.

## Goals

- All application endpoints authenticate with bearer JWTs; no HTTP sessions.
- Access JWT carries `sub`, `iat`, `exp`, `iss`, `aud`, and `roles`.
- Refresh tokens are opaque, hashed at rest, rotatable, revocable, and checked
  in the database on every refresh.
- Password login, first-admin bootstrap, and invite acceptance all issue the
  same token pair. Passkey/WebAuthn support is removed in this change.
- A 401 from an expired access token triggers one refresh-and-retry in the BFF.
- Existing authorization semantics (resolver `@PreAuthorize`, staff checks,
  suspended-member write block) stay unchanged.
- Backend tests remain hermetic (Testcontainers MySQL) and frontend tests mock
  the BFF boundary.

## Non-goals

- No OAuth2/OIDC authorization-code flow, client registry, or Spring
  Authorization Server.
- No third-party IdP, LDAP, or directory sync.
- No changing the domain authorization rules or the GraphQL error shape for
  non-expired authorization failures.
- No changing the Actuator chain (HTTP Basic, admin-only) except verifying it
  still works with stateless security.

## Anti-goals

- Storing JWTs in a database or in `localStorage`.
- Logging token values, including refresh tokens, in application/BFF logs.
- Keeping a fallback session-cookie login path after cutover; two live auth
  mechanisms would double the attack surface and keep CSRF complexity.

## Constraints

- Spring Boot 4.2, Spring Security 7.2, Java 21, Gradle Kotlin DSL, Flyway
  migrations (`ddl-auto=validate`).
- TanStack Start BFF in `frontend/src/lib/backend-proxy.ts`; browser never
  learns `CHAFGRAM_API_URL`.
- GraphQL authorization failures are HTTP 200 + `errors[]`; token validity
  failures are filter-level HTTP 401.
- Dev credentials are throwaway; real secrets must come from env vars and
  never be committed (`.env` is git-ignored).
- Passwords currently use BCrypt via `PasswordEncoder`; keep it.

## Approaches considered

1. **Spring Security OAuth2 Resource Server + custom refresh service
   (chosen).** Add `spring-boot-starter-security-oauth2-resource-server` and
   its test starter. Spring provides `JwtDecoder`, the bearer filter, authority
   mapping, and 401 handling. A small auth module owns refresh-token issuance,
   rotation, revocation, and the login/refresh/logout endpoints. Least custom
   security code; framework handles header parsing and JWT validation.
2. **Custom bearer filter around `JwtDecoder` only.** Add
   `spring-security-oauth2-jose` and write a `OncePerRequestFilter` that
   extracts `Bearer`, decodes, maps roles to authorities, and emits 401s.
   Smaller dependency surface but more security code we own and must test.
3. **Spring Authorization Server.** Rejected: client registration and
   authorization-code flows are unnecessary for a first-party BFF and would
   dominate the work.

## Decisions

### Token format and claims

- HS256 symmetric signing key from `CHAFGRAM_JWT_SECRET` (dev default is a
  documented throwaway value; production must override). Minimum 32 bytes.
- `JwtEncoder`/`JwtDecoder` beans built from the same secret.
- Claims: `sub` = user id, `iat`, `exp`, `iss` = `chafgram`, `aud` =
  `chafgram-web`, `roles` = list like `["admin"]`.
- Access TTL: 15 minutes (`CHAFGRAM_ACCESS_TTL`, default `15m`).
- Refresh TTL: 14 days (`CHAFGRAM_REFRESH_TTL`, default `14d`).
- `roles` claim maps to `ROLE_*` authorities; no permission scopes in v1.

### Refresh token storage

- New Flyway migration `V5__create_refresh_tokens.sql`:
  `id BIGINT AUTO_INCREMENT`, `user_id BIGINT NOT NULL` (FK to `users`),
  `token_hash CHAR(64) NOT NULL UNIQUE`, `expires_at DATETIME(6) NOT NULL`,
  `created_at DATETIME(6) NOT NULL`, `revoked_at DATETIME(6) NULL`,
  `replaced_by BIGINT NULL` (the new row id when rotated).
- Raw token is a secure random 32-byte value, Base64URL encoded. Only its
  SHA-256 hex is stored.
- Issue: insert one row with the hash and expiry.
- Refresh: look up by hash; reject missing/expired/revoked with 401. **Any
  presented revoked row — with or without `replaced_by` — is invalid and never
  mints a new token.** On success, atomically claim the row, mark it revoked
  with `replaced_by` pointing at the new row, issue a new pair, and insert the
  new hash. Rotation happens on every refresh.
- Replay of a row revoked **with** `replaced_by` (normal rotation) returns 401
  but does not revoke other rows — a lost response or concurrent tab must not
  be treated as theft. Replay of a row revoked **without** `replaced_by`
  (logout or suspected theft) returns 401 and revokes all refresh tokens for
  that user, forcing a fresh login.
- Logout: revoke the current refresh row and clear the cookie. The access JWT
  stays valid until `exp` (short by design).

### Endpoints

REST keep-list additions, all under `/api/v1/auth`:

- `POST /login` — public. Accepts username **or** email plus password. Returns
  `{ accessToken, tokenType: "Bearer", expiresIn }` and sets the refresh cookie.
- `POST /refresh` — public at HTTP boundary; authenticates solely by the
  refresh cookie. Returns a new access token and sets the rotated cookie.
- `POST /logout` — requires a valid access token; revokes the refresh cookie's
  row and clears the cookie.
- `POST /bootstrap`, `POST /api/v1/register`, `POST /api/v1/invites/accept`
  keep their current inputs but return the token pair + refresh cookie instead
  of creating a session.
- `GET /api/v1/media/{id}/signed-url` — authenticated by bearer token; returns
  `{ url, expiresAt }` for short-lived media access (see below).

### Media bytes

`<img>` and `<video>` cannot attach a memory-held bearer token, so the backend
issues **short-lived signed URLs** (approved approach):

- `GET /api/v1/media/{id}/signed-url` requires a bearer token, checks
  `MediaAccessService`, and returns a URL with `exp` (Unix seconds) and `sig`
  (HMAC-SHA256 over `id`, `exp`, and the viewer id, using the JWT secret).
- `GET /api/v1/media/{id}/bytes?exp=...&sig=...` validates signature, expiry,
  and viewer id; no bearer token required. Existing authorized-header access
  to the same bytes endpoint remains for API clients.
- TTL: 1 hour (`CHAFGRAM_MEDIA_URL_TTL`, default `1h`), with
  `Cache-Control: private, max-age=300` on bytes.
- Frontend: GraphQL `Post.media` carries a `url` from the signed-URL helper,
  so no extra BFF round trip per image. On media `onError`, the client asks the
  BFF for one fresh signed URL and swaps `src` once. Already-loaded media keeps
  playing after expiry; a very long-open page can reload to refresh URLs.
- Signing/validating a media URL never consumes or rotates the refresh cookie.

### Passkey removal

Approved: remove passkeys entirely in this cutover.

- Backend: drop `spring-security-webauthn`, `.webAuthn(...)` config, WebAuthn
  REST endpoints, `PasskeyService`, passkey DTOs/resolvers, and the
  framework-managed `user_entities` / `user_credentials` tables via a
  forward-only migration (V6 or next available).
- Frontend: remove the "Sign in with passkey" button, passkey notice, and the
  Settings passkeys section.
- Docs: update `product.md`, `README.md`, `docs/architecture.md`, and
  `docs/web-ui.md` so passkeys are no longer described as implemented or planned.

### Spring Security wiring

- Main chain: `SessionCreationPolicy.STATELESS`, CSRF disabled,
  `oauth2ResourceServer(jwt -> ...)` with the `JwtDecoder` bean, public matchers
  unchanged, `.anyRequest().authenticated()`.
- Remove `HttpSessionSecurityContextRepository`, `CookieCsrfTokenRepository`,
  and the CSRF token request handler from the main chain.
- Actuator chain stays HTTP Basic + admin role; verify Basic still works when
  the global chain is stateless.
- GraphQL resolver annotations and `CurrentMember` (SecurityContext) stay; the
  bearer filter populates the context per request.
- Media signed-URL generation and WebSocket upgrades authenticate through the
  same bearer filter; WebSocket may need the token in the connection request
  (builder decides and documents it). Public matchers cover only the signed
  bytes URL and the documented auth endpoints.

### Frontend BFF

- `loginRequest` sends credentials to `/api/v1/auth/login`, copies the refresh
  `Set-Cookie` back to the browser, and returns the access token to browser
  memory.
- Apollo/browser calls carry `Authorization: Bearer <access>` to the BFF; the
  BFF forwards that header via `callBackend`.
- On backend 401, `callBackend` calls `/api/v1/auth/refresh` once with the
  browser's refresh cookie, forwards the rotated `Set-Cookie`, swaps in the new
  access token for the retry, then retries the original request once. A second
  401 clears the in-memory token and redirects to `/login`.
- No `JSESSIONID` or `XSRF-TOKEN` cookies are forwarded after cutover.
- Refresh cookie: name `chafgram_refresh`, path `/`, `HttpOnly`,
  `SameSite=Strict`, `Secure` when `CHAFGRAM_COOKIE_SECURE=true` (default true
  outside dev). Since the BFF calls refresh server-side, cross-site JS cannot
  read or send it.

### Config and docs

- `.env.example` and `application.properties` add:
  `CHAFGRAM_JWT_SECRET`, `CHAFGRAM_JWT_ISSUER`, `CHAFGRAM_JWT_AUDIENCE`,
  `CHAFGRAM_ACCESS_TTL`, `CHAFGRAM_REFRESH_TTL`, `CHAFGRAM_COOKIE_SECURE`,
  `CHAFGRAM_MEDIA_URL_TTL`.
- Update `docs/architecture.md`, `AGENTS.md`, `README.md`, and
  `docs/development-workflow.md`: auth flow, REST keep-list, CSRF section,
  and verification notes all change.
- `product.md` open question about directory sync remains; passkeys are removed
  as part of this change, so the auth section must reflect password-only login.

## Settled during review (designer calls)

- **Reload / SSR**: a reload wipes browser-memory access tokens. The BFF
  silently exchanges the refresh cookie for a new access token during app
  bootstrap, then the Apollo client uses that token. The SSR shell stays public;
  authenticated data loads after hydration. A failed silent refresh clears the
  token and redirects to `/login`.
- **Refresh replay**: any presented revoked row is rejected with 401 and never
  mints a new token, including a rotated row with `replaced_by` set. Only a row
  revoked without a replacement (logout or suspected theft) additionally
  revokes all refresh rows for the user. Access JWTs stay valid until `exp` —
  no denylist in v1.
- **Stale roles/status**: role claims may be up to 15 minutes stale. This is
  accepted; domain services still re-read `User.status` for write checks, and
  refresh rejects deactivated accounts. Document the trade-off in the code.
- **Media**: backend-issued short-lived signed URLs (option B) approved, with
  the 1-hour TTL and `onError` refresh policy described above.
- **Passkeys**: removed entirely (approved), including tables and UI.

## Open questions for the product owner

None — all decisions are approved.

## Left to the builder

- Exact Java package/class names in a new `identity` auth subpackage
  (`TokenService`, `RefreshToken`, `RefreshTokenRepository`, auth controller).
- Refresh cookie path is `/` (designer call) so any BFF route can use it,
  including media.
- Add a `replaced_by` column to distinguish normal rotation from theft/logout
  revocation, as required by the reuse rule above.
- WebSocket handshake token transport (query parameter vs
  `Sec-WebSocket-Protocol` header) — document the choice and its risk.
- Test helper to obtain a valid access token in integration tests without
  going through the HTTP login endpoint.

## Verification

- Backend: token issue on login/bootstrap/invite; JWT claims and signature;
  expired/invalid JWT → 401; refresh rotates and revokes; any revoked hash
  presented again → 401 (user-wide revocation only when it was revoked without
  `replaced_by`); logout revokes; roles still enforce
  `@PreAuthorize`; suspended members still cannot write; signed media URLs
  accept valid signatures and reject expired/tampered/wrong-viewer URLs;
  passkey endpoints and tables are gone.
- Frontend: BFF forwards bearer, retries once on 401 with refresh, fails closed
  on second 401, and no session/CSRF cookies are relayed; media URLs render in
  `<img>`/`<video>` and recover once via `onError`.
- `just verify` must stay green; migrations forward-only.

# Access + Refresh Tokens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Spring Security session/CSRF authentication with stateless JWT access tokens plus database-backed opaque refresh tokens, remove passkeys, and keep all existing authorization rules intact.

**Architecture:** Spring Security OAuth2 Resource Server validates short-lived HS256 JWTs on every request; a new identity/auth package issues, rotates, and revokes opaque refresh tokens stored as SHA-256 hashes in MySQL. The BFF forwards bearer tokens, silently refreshes on 401, and media uses short-lived backend-signed URLs because `<img>`/`<video>` cannot carry a header.

**Tech Stack:** Spring Boot 4.2, Spring Security 7.2 + `spring-boot-starter-security-oauth2-resource-server`, Java 21, Flyway, MySQL 8 (Testcontainers), TanStack Start BFF, Vitest.

**Spec:** `docs/access-refresh-tokens-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- Signing: HS256, symmetric secret `CHAFGRAM_JWT_SECRET` (dev default throwaway, min 32 bytes).
- Claims: `sub` (user id), `iat`, `exp`, `iss=chafgram`, `aud=chafgram-web`, `roles` (e.g. `["admin"]`).
- Lifetimes: access `CHAFGRAM_ACCESS_TTL` default `15m`; refresh `CHAFGRAM_REFRESH_TTL` default `14d`.
- Refresh token: random 32 bytes Base64URL; store only SHA-256 hex; hash column unique.
- Refresh rotation: every successful refresh revokes the old row with `replaced_by`; normal rotation is not theft.
- Replay: **any** presented revoked row (with or without `replaced_by`) returns 401 and never mints a new token. Family-wide revocation applies only to a row revoked **without** `replaced_by`.
- Refresh cookie: name `chafgram_refresh`, path `/`, `HttpOnly`, `SameSite=Strict`, `Secure` when `CHAFGRAM_COOKIE_SECURE=true` (default true outside dev).
- Main Spring Security chain: `STATELESS`, CSRF disabled, no `HttpSessionSecurityContextRepository`.
- GraphQL authorization failures keep HTTP 200 + `errors[]`; token failures are HTTP 401.
- Media URLs: `CHAFGRAM_MEDIA_URL_TTL` default `1h`; bytes `Cache-Control: private, max-age=300`; signing never rotates refresh tokens.
- Passkeys/WebAuthn removed: dependency, config, endpoints, service/DTOs, tables, and UI.
- Tests remain hermetic: backend uses Testcontainers MySQL, frontend mocks the BFF boundary.
- Flyway migrations forward-only; `ddl-auto=validate`.
- Never commit real secrets; `.env` is git-ignored.

## Review Focus

1. Access token expires during the first request after reload — BFF must silently refresh using the cookie and retry once; pinned in Task 6.
2. Rotated refresh token presented twice (concurrent tabs/lost response) must return 401 but must **not** trigger theft revocation — `replaced_by` distinguishes it; pinned in Task 1.
3. A token revoked without replacement must return 401 and revoke all user refresh rows; pinned in Task 1.
4. Tampered, expired, or wrong-viewer signed media URLs must be rejected; pinned in Task 4.
5. A demoted/suspended member holding a valid JWT must still be blocked on writes despite stale role claims; pinned in Task 3.

---

### Task 1: Refresh-token persistence and token service

**Files:**
- Create: `backend/api/src/main/resources/db/migration/V5__create_refresh_tokens.sql`
- Create: `backend/api/src/main/java/io/github/james5635/chafgram/identity/auth/RefreshToken.java`
- Create: `backend/api/src/main/java/io/github/james5635/chafgram/identity/auth/RefreshTokenRepository.java`
- Create: `backend/api/src/main/java/io/github/james5635/chafgram/identity/auth/TokenService.java`
- Create: `backend/api/src/main/java/io/github/james5635/chafgram/config/JwtConfig.java`
- Test: `backend/api/src/test/java/io/github/james5635/chafgram/identity/auth/TokenServiceTest.java`

**Interfaces:**
- Consumes: `User` entity, `UserRepository`; Spring Boot BOM-managed Nimbus/JWT classes from the resource-server starter.
- Produces: `TokenService.issue(User user) -> TokenPair`, `TokenPair(String accessToken, String refreshToken, long expiresIn)`, `TokenService.refresh(String rawRefreshToken) -> TokenPair`, `TokenService.revoke(String rawRefreshToken)`, `TokenService.revokeAllForUser(Long userId)`, `JwtDecoder jwtDecoder()` bean.

- [ ] **Step 1: Write the failing test**

Create `TokenServiceTest` with @SpringBootTest extending `AbstractIntegrationTest`. Bootstrap a member via `MemberService.bootstrap`. Assertions: `issue` returns a JWT whose claims include `sub`, `iss`, `aud`, `roles` and `exp` about 15 minutes out, and a Base64URL refresh token that is **not** equal to any stored hash; `RefreshTokenRepository.findByTokenHash(...)` finds the SHA-256 hex. Add `rotatedTokenIsNotTreatedAsTheft`: call `refresh`, then call `refresh` again with the now-revoked rotated token and assert a 401-equivalent exception (no new token) while the user's other live rows remain usable. Add `revokedTokenReuseRevokesAllUserTokens`: call `revoke`, then `refresh` with that token and assert 401-equivalent exception plus all rows for the user revoked. Add `wrongUserTokenCannotRefresh` with a second member.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :api:test --tests '*TokenServiceTest'`
Expected: FAIL — classes/migration do not exist.

- [ ] **Step 3: Implement the migration and entities**

Create V5 with `id`, `user_id` FK, `token_hash CHAR(64) UNIQUE`, `expires_at`, `created_at`, `revoked_at`, `replaced_by`. Create `RefreshToken` JPA entity and `RefreshTokenRepository` with `findByTokenHash(String)`, `findAllByUserIdAndRevokedAtIsNull(Long)`, `revokeAllForUser(Long)`. Do not store raw tokens.

- [ ] **Step 4: Implement `TokenService` and `JwtConfig`**

`JwtConfig` builds `JwtEncoder` and `JwtDecoder` from `CHAFGRAM_JWT_SECRET` using `NimbusJwtEncoder`/`NimbusJwtDecoder` with HS256. `TokenService` signs access JWTs with the spec claims and generates/stores/rotates refresh hashes. Rotation sets `replaced_by` on the old row; presenting any revoked row throws an auth exception, and a row revoked without `replaced_by` additionally calls `revokeAllForUser`. Refresh rejects deactivated users.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && ./gradlew :api:test --tests '*TokenServiceTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/api/src/main/resources/db/migration/V5__create_refresh_tokens.sql backend/api/src/main/java/io/github/james5635/chafgram/identity/auth backend/api/src/main/java/io/github/james5635/chafgram/config/JwtConfig.java backend/api/src/test/java/io/github/james5635/chafgram/identity/auth/TokenServiceTest.java
git commit -m "feat(backend): add JWT and opaque refresh token service"
```

---

### Task 2: Auth endpoints and token issuance on account flows

**Files:**
- Create: `backend/api/src/main/java/io/github/james5635/chafgram/identity/controller/AuthController.java`
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/identity/controller/OnboardingController.java`
- Test: `backend/api/src/test/java/io/github/james5635/chafgram/identity/controller/AuthControllerIntegrationTest.java`

**Interfaces:**
- Consumes: `TokenService.issue/refresh/revoke`, `MemberService.bootstrap/registerOpen`, `InviteService.accept`, `MemberUserDetailsService`, `PasswordEncoder`.
- Produces: `POST /api/v1/auth/login`, `/refresh`, `/logout`; `TokenPair` responses; refresh cookie header name `chafgram_refresh`.

- [ ] **Step 1: Write the failing test**

Create `AuthControllerIntegrationTest` with MockMvc. Assertions: `POST /api/v1/auth/login` with username OR email + password returns JSON with `accessToken`, `tokenType=Bearer`, `expiresIn` and a `Set-Cookie: chafgram_refresh=...; HttpOnly; SameSite=Strict; Path=/`; bad credentials return 401; `POST /api/v1/auth/refresh` with the cookie returns a new access token and a **different** refresh cookie; old cookie no longer refreshes; `POST /api/v1/auth/logout` with bearer revokes the cookie and subsequent refresh with that cookie returns 401. Add assertions that `/api/v1/bootstrap`, `/api/v1/register`, and `/api/v1/invites/accept` now return the token pair and refresh cookie instead of a session.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :api:test --tests '*AuthControllerIntegrationTest'`
Expected: FAIL — endpoints missing or still session-based.

- [ ] **Step 3: Implement `AuthController`**

`POST /login` accepts `{"username","password"}` where username may be email (reuse `MemberUserDetailsService` lookup), verifies BCrypt hash, calls `TokenService.issue`, sets the refresh cookie, returns `TokenPair`. `POST /refresh` reads only the cookie, calls `TokenService.refresh`, sets rotated cookie. `POST /logout` requires bearer, calls `TokenService.revoke`, clears cookie. Cookie `Secure` flag from `CHAFGRAM_COOKIE_SECURE` (default true unless profile `test`/`dev`).

- [ ] **Step 4: Modify `OnboardingController`**

Replace session authentication with `TokenService.issue` and refresh cookie on bootstrap, register, and invite accept. Keep response bodies compatible for identity fields and add token fields.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && ./gradlew :api:test --tests '*AuthControllerIntegrationTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/api/src/main/java/io/github/james5635/chafgram/identity/controller/AuthController.java backend/api/src/main/java/io/github/james5635/chafgram/identity/controller/OnboardingController.java backend/api/src/test/java/io/github/james5635/chafgram/identity/controller/AuthControllerIntegrationTest.java
git commit -m "feat(backend): issue token pairs from login and onboarding flows"
```

---

### Task 3: Stateless security chain and authorization behavior

**Files:**
- Modify: `backend/api/build.gradle.kts`
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/config/SecurityConfiguration.java`
- Modify: `backend/api/src/main/resources/application.properties`
- Test: `backend/api/src/test/java/io/github/james5635/chafgram/config/TokenSecurityIntegrationTest.java`

**Interfaces:**
- Consumes: `JwtDecoder` bean from Task 1, resource-server starter.
- Produces: bearer-authenticated main chain; public auth endpoints; signed media bytes endpoint public; no sessions/CSRF.

- [ ] **Step 1: Write the failing test**

Create `TokenSecurityIntegrationTest`. Assertions: request to `/graphql` without bearer gets 401; with a valid access token an authorized query succeeds; with an expired/invalid/foreign-signature JWT gets 401; an admin JWT can call an admin-only resolver but a member JWT gets HTTP 200 + `errors[].extensions.classification=FORBIDDEN`; a suspended member JWT can read but a write returns `FORBIDDEN` from the service layer; actuator `/actuator/info` still requires admin Basic and `/actuator/health` stays public; no `JSESSIONID` or `XSRF-TOKEN` cookie appears in responses.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && ./gradlew :api:test --tests '*TokenSecurityIntegrationTest'`
Expected: FAIL — chain still uses sessions/CSRF.

- [ ] **Step 3: Add dependency and rewire the main chain**

Add `implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")` and the matching test starter. In `SecurityConfiguration`, set `SessionCreationPolicy.STATELESS`, disable CSRF, remove `HttpSessionSecurityContextRepository` and CSRF beans, add `.oauth2ResourceServer(jwt -> jwt.jwt(...))`, keep the public matchers from the design, and add the signed media bytes matcher. Leave the actuator chain intact.

- [ ] **Step 4: Configure JWT properties**

Add `CHAFGRAM_JWT_SECRET`, issuer, audience, TTLs, cookie secure flag, and media URL TTL to `application.properties` with throwaway dev defaults. Keep real values environment-overridable.

- [ ] **Step 5: Run test to verify it passes**

Run: `cd backend && ./gradlew :api:test --tests '*TokenSecurityIntegrationTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/api/build.gradle.kts backend/api/src/main/java/io/github/james5635/chafgram/config/SecurityConfiguration.java backend/api/src/main/resources/application.properties backend/api/src/test/java/io/github/james5635/chafgram/config/TokenSecurityIntegrationTest.java
git commit -m "refactor(backend): make main security chain stateless with JWT bearer auth"
```

---

### Task 4: Signed media URLs for feed and player

**Files:**
- Create: `backend/api/src/main/java/io/github/james5635/chafgram/media/service/MediaUrlService.java`
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/media/controller/MediaHttpController.java`
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/social/dto/PostDto.java`
- Modify: `backend/api/src/main/resources/graphql/PostDto.graphqls`
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/social/dto/Views.java`
- Modify: `frontend/src/lib/post-mappers.ts`
- Modify: `frontend/src/components/PostCard.tsx`
- Test: `backend/api/src/test/java/io/github/james5635/chafgram/media/service/MediaUrlServiceTest.java`
- Test: `frontend/src/lib/post-mappers.test.ts`

**Interfaces:**
- Consumes: `MediaAsset`, `MediaAccessService`, JWT secret, `Post.mediaIds`.
- Produces: `MediaUrlService.sign(Long mediaId, Long viewerId) -> String`; `GET /api/v1/media/{id}/signed-url` (bearer-protected) for on-demand re-signing; `PostDto.mediaUrls: List<String>`; frontend `PostCardData.media[].url` already signed.

- [ ] **Step 1: Write the failing backend test**

Create `MediaUrlServiceTest`. Assertions: `sign` produces a URL with `exp` about 1 hour out and a `sig` HMAC over `id`, `exp`, viewer id; validating a tampered `sig` fails; an expired `exp` fails; a URL signed for viewer A fails for viewer B. Add MockMvc cases that fetch bytes via a valid signed URL and get 200 with `Cache-Control: private, max-age=300`, and that `GET /api/v1/media/{id}/signed-url` without a bearer returns 401 but with a bearer returns a fresh `url`.

- [ ] **Step 2: Run it to verify it fails**

Run: `cd backend && ./gradlew :api:test --tests '*MediaUrlServiceTest'`
Expected: FAIL — service/signature validation missing.

- [ ] **Step 3: Implement signing, re-sign endpoint, and bytes validation**

`MediaUrlService` signs/validates with the shared secret. `MediaHttpController` adds `GET /api/v1/media/{id}/signed-url` (bearer-protected) returning `{ url, expiresAt }` and validates `exp`/`sig`/viewer on the signed bytes path before streaming. Keep the existing bearer-protected bytes path for API clients.

- [ ] **Step 4: Expose signed URLs in GraphQL and use them in the frontend**

Add `mediaUrls: [String!]!` to `PostDto` and `PostDto.graphqls`; populate from `Views.post()` using `MediaUrlService` and the viewer id. Update `frontend/src/graphql/feed.graphql` to select `mediaIds` and `mediaUrls`; regenerate types with `pnpm run generate`. Change `post-mappers.ts` to use `mediaUrls[index]` as the media URL. Add a one-shot `onError` refresh in `PostCard.tsx` that asks the BFF for a fresh signed URL and swaps `src` once.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd backend && ./gradlew :api:test --tests '*MediaUrlServiceTest'` and `cd frontend && pnpm run test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/api/src/main/java/io/github/james5635/chafgram/media backend/api/src/main/java/io/github/james5635/chafgram/social/dto backend/api/src/main/resources/graphql/PostDto.graphqls frontend/src/graphql/feed.graphql frontend/src/lib/post-mappers.ts frontend/src/components/PostCard.tsx
git commit -m "feat(media): serve feed media through short-lived signed URLs"
```

---

### Task 5: Remove passkeys/WebAuthn

**Files:**
- Modify: `backend/api/build.gradle.kts`
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/config/SecurityConfiguration.java`
- Delete: `backend/api/src/main/java/io/github/james5635/chafgram/identity/service/PasskeyService.java`
- Delete: passkey DTOs/resolvers under `identity/dto` and `identity/controller`
- Create: `backend/api/src/main/resources/db/migration/V6__drop_webauthn_tables.sql`
- Modify: `frontend/src/components/LoginScreen.tsx`
- Modify: `frontend/src/routes/settings.tsx`
- Test: `backend/api/src/test/java/io/github/james5635/chafgram/identity/IdentityFlowIntegrationTest.java`
- Test: `frontend/src/components/LoginScreen.test.tsx`

**Interfaces:**
- Consumes: nothing new.
- Produces: no `/webauthn/**` or `/login/webauthn` endpoints, no passkey UI, no WebAuthn tables.

- [ ] **Step 1: Write the failing tests**

In `IdentityFlowIntegrationTest`, assert `/webauthn/register/options` and `/login/webauthn` no longer exist (404, not session redirect). In `LoginScreen.test.tsx`, assert no button named "Sign in with passkey" and no passkey notice. In `Settings`, assert no passkey section.

- [ ] **Step 2: Run them to verify they fail**

Run: `cd backend && ./gradlew :api:test --tests '*IdentityFlowIntegrationTest'` and `cd frontend && pnpm exec vitest run src/components/LoginScreen.test.tsx src/routes/settings.test.tsx`
Expected: FAIL — endpoints/UI still present.

- [ ] **Step 3: Remove backend WebAuthn**

Remove the `spring-security-webauthn` dependency, `.webAuthn(...)` config, WebAuthn beans, `PasskeyService`, passkey DTOs, and passkey resolver methods. Add V6 dropping `user_entities` and `user_credentials`.

- [ ] **Step 4: Remove frontend passkey UI**

Delete the passkey mode button and notice from `LoginScreen.tsx` (password-only form remains) and remove the passkeys section from `settings.tsx`.

- [ ] **Step 5: Run tests to verify they pass**

Run the two commands from Step 2.
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(auth): remove passkeys and WebAuthn support"
```

---

### Task 6: BFF token handling, refresh retry, and silent bootstrap

**Files:**
- Modify: `frontend/src/lib/backend-proxy.ts`
- Modify: `frontend/src/lib/session-server.ts`
- Modify: `frontend/src/lib/apollo.ts`
- Modify: `frontend/src/components/LoginScreen.tsx`
- Modify: `frontend/src/hooks/useSessionGuard.ts`
- Modify: `frontend/src/routes/__root.tsx`
- Test: `frontend/src/lib/backend-proxy.test.ts`
- Test: `frontend/src/components/LoginScreen.test.tsx`

**Interfaces:**
- Consumes: `/api/v1/auth/login|refresh|logout` from Task 2; refresh cookie from browser.
- Produces: in-memory `accessToken` held by the Apollo/BFF client; `callBackend` retries once on 401 after refresh.

- [ ] **Step 1: Write the failing test**

In `backend-proxy.test.ts`, mock `callBackend` transport: first `graphqlRequest` returns 401, refresh returns a new access token, retry returns 200; assert exactly one refresh and two `graphqlRequest` sends, and that the retry carries the new bearer. Add a case where refresh itself fails and assert no retry plus a redirect-to-login signal. In `LoginScreen.test.tsx`, assert successful login stores the returned access token in the client auth store and navigates to `/`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/lib/backend-proxy.test.ts src/components/LoginScreen.test.tsx`
Expected: FAIL — refresh path not implemented.

- [ ] **Step 3: Implement BFF bearer and refresh retry**

`session-server.ts` `loginRequest` posts to `/api/v1/auth/login` and copies the refresh `Set-Cookie` back to the browser while returning only the access token to JS. `callBackend` reads `Authorization` from the incoming request and forwards it to Spring Boot. On a 401 from the backend, call `/api/v1/auth/refresh` with the browser's refresh cookie once, append rotated `Set-Cookie`, update the in-memory access token for the retry, then resend the original request once. A second 401 clears the token and signals re-login. Remove all `JSESSIONID`/`XSRF-TOKEN` forwarding and the CSRF retry block.

- [ ] **Step 4: Implement silent refresh during bootstrap**

In the root route/client startup, if an access token is absent but the refresh cookie exists, call the refresh server function once before the first authenticated GraphQL operation. SSR renders the public shell only; authenticated data loads after hydration. `useSessionGuard` redirects to `/login` when refresh fails.

- [ ] **Step 5: Run tests to verify they pass**

Run the Step 2 command plus `cd frontend && pnpm exec tsc --noEmit`.
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/lib/backend-proxy.ts frontend/src/lib/session-server.ts frontend/src/lib/apollo.ts frontend/src/components/LoginScreen.tsx frontend/src/hooks/useSessionGuard.ts frontend/src/routes/__root.tsx frontend/src/lib/backend-proxy.test.ts frontend/src/components/LoginScreen.test.tsx
git commit -m "feat(frontend): send bearer tokens and refresh on 401 in the BFF"
```

---

### Task 7: Configuration and documentation cutover

**Files:**
- Modify: `.env.example`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/architecture.md`
- Modify: `docs/development-workflow.md`
- Modify: `docs/web-ui.md`
- Modify: `product.md`
- Modify: `docs/access-refresh-tokens-design.md` (status: implemented)

**Interfaces:**
- Consumes: all decisions in the spec.
- Produces: docs that no longer describe sessions, CSRF, or passkeys.

- [ ] **Step 1: Update configuration examples**

Add `CHAFGRAM_JWT_SECRET`, `CHAFGRAM_JWT_ISSUER`, `CHAFGRAM_JWT_AUDIENCE`, `CHAFGRAM_ACCESS_TTL`, `CHAFGRAM_REFRESH_TTL`, `CHAFGRAM_COOKIE_SECURE`, `CHAFGRAM_MEDIA_URL_TTL` to `.env.example` with dev-only throwaway values and comments that production must override.

- [ ] **Step 2: Update docs**

Rewrite auth/CSRF/session sections in `docs/architecture.md`, `AGENTS.md`, and `docs/development-workflow.md`; remove passkey claims from `product.md`, `README.md`, and `docs/web-ui.md`; document the signed media URL flow and refresh retry. Mark the design status implemented.

- [ ] **Step 3: Verify full suite**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build all green.

- [ ] **Step 4: Commit**

```bash
git add .env.example README.md AGENTS.md docs product.md
git commit -m "docs: document token auth and remove session/passkey guidance"
```

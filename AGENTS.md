# AGENTS.md — Working in the Chafgram repo

Guidance for AI agents (and humans) contributing to this repository. Read this first, then
[`product.md`](product.md) for what Chafgram is and [`design.md`](design.md) for how it
looks and behaves.

## What this is

Chafgram is an **open-source (MIT), self-hosted private social platform for companies**
(short video, watch, posts, comments, follows, direct messages). See `product.md` — do not
assume public-social-network semantics anywhere in the codebase.

## Repository layout

```
backend/    Spring Boot 4 (Java 21) service — Gradle wrapper, package io.github.james5635.chafgram
frontend/   TanStack Start (React 19, Vite, Tailwind v4) app — file-based routing
compose.yaml  Local dev MySQL 8.0 (dev-only throwaway credentials)
config/     Dev service config (Garage object-storage config)
scripts/hooks/pre-push  Tracked git hook source (installed by just bootstrap)
docs/functional-requirements.md  Functional requirements (FR)
docs/business-rules.md  Business rules (BR)
docs/architecture.md  Architecture (system design)
docs/development-workflow.md  Setup, dev loop, DB, testing, verification
product.md  Product definition (what/who/why, goals, non-goals)
design.md   Design system (tokens, components, motion, a11y)
docs/web-ui.md   Complete web UI spec (wireframe + component what/where/how)
docs/mobile-ui.md Complete mobile UI spec (wireframe + component what/where/how)
README.md   Human-facing overview and quick start
.agents/    Agent tooling/skills — tracked in the repo, dev-only, never shipped
```

`.agents/` holds local agent skills. It is versioned so every contributor gets the
same toolkit, but it is **not** part of the product: no backend or frontend code
may depend on it, and it must not be bundled into deployable artifacts (jar,
`.output/`, container images).

## Commands

A `justfile` at the repo root wraps common dev tasks — `just` lists them,
`just dev` runs backend + frontend (run `just mysql` first for the database), `just test` runs the test suites, `just verify` runs just test + just build.
Per-package recipes live in `backend/justfile` and `frontend/justfile` — `just install`
is a root alias for `backend::install` + `frontend::install`. Run `just bootstrap` once after
cloning to install the tracked pre-push hook (`scripts/hooks/pre-push`), which runs
`just verify` before every push. The raw commands below remain the source of truth.

### Backend (`backend/`)

```bash
just mysql                 # start local MySQL 8.0 via compose.yaml, wait for healthy (stop with: docker compose down)
just seed                  # RESET dev DB: wipe, Flyway-migrate, insert full demo fixtures (admin + members, posts, comments, DMs, notifications, reports, audit log)
docker compose up -d       # start MySQL AND Garage (S3-compatible dev object storage, bucket chafgram-media)
docker compose up garage-init   # one-shot: layout assign, dev key import, bucket create, permissions — idempotent
./gradlew bootRun          # run dev server (default port 8080)
./gradlew test             # run tests (hermetic — Testcontainers MySQL)
just migrate               # apply Flyway migrations only, no data wiped
just unseed                # EMPTY dev DB: wipe, Flyway-migrate, no fixtures
./gradlew bootJar          # build jar (build/libs/)
```

`just dev` / `just backend::dev` / `just frontend::dev` do not start the database — run `just mysql` first.

- Requires JDK 21. Local dev MySQL runs via `compose.yaml`; the datasource in
  `application.properties` defaults to the compose dev credentials
  (`chafgram` / `chafgram`, db `chafgram`, overridable via `SPRING_DATASOURCE_*` env
  vars or a git-ignored `.env`/profile). Never commit real credentials.
- Dev object storage: Garage (S3-compatible API on `http://localhost:3900`, region
  `garage`, bucket `chafgram-media`, dev key + secret in `scripts/garage-init.sh`).
  Bucket/layout/key are created by `garage-init` on first run — re-running is safe.
- `./gradlew test` is hermetic: it starts a throwaway MySQL 8 container via
  Testcontainers (one per test JVM, Docker must be running). The dev database is never
  touched — no `just seed` afterwards. Test-only config lives in
  `backend/src/test/resources/application-test.properties` (profile `test`); never add a
  test-scoped `application.properties`, it shadows the main one on the classpath.
- Identity Backend modules implemented: `identity` (users/invites/workspace/passkeys/account
  flows, REST `/api/v1`), `social` (posts/comments/reactions/follows/feed/search + GraphQL at
  `/graphql`), `messaging` (1:1 + groups, REST), `notifications`, `moderation`
  (reports + audit log), `media` (upload/bytes + in-app async video pipeline), `mail`
  (account mail: logging fallback by default, SMTP opt-in). See `docs/architecture.md`
  for the system design.
- Account mail (email verification, password reset — FR-1.7) is opt-in: unless
  `CHAFGRAM_MAIL_SMTP_ENABLED=true`, mail is written to the app log (no outbound call,
  BR-10.1). `CHAFGRAM_APP_URL` builds the links in those mails.
- Video processing runs in-app: `ffmpeg`/`ffprobe` must be on `PATH` for videos to
  reach `status='ready'`; images are unaffected.
- Actuator has its own security chain: `/actuator/health` is public, `/actuator/info`
  and `/actuator/metrics` are admin-only with HTTP Basic (anonymous → 401).
- Passkeys are handled by Spring Security WebAuthn; the `user_entities` and
  `user_credentials` tables are framework-managed — do not add JPA entities on top of
  them or alter their DDL. Configure via env `CHAFGRAM_WEBAUTHN_RP_ID` (default
  `localhost`) and `CHAFGRAM_WEBAUTHN_ALLOWED_ORIGINS` (default `http://localhost:3000`,
  comma-separated if several).
- Authentication is session-cookie based; passkey ceremonies are at
  `/webauthn/register/*` (authenticated) and `/login/webauthn` +
  `/webauthn/authenticate/options` (unauthenticated). CSRF: double-submit cookie,
  readable JS header `X-XSRF-TOKEN` (CookieCsrfTokenRepository) — BFF clients must
  send it on mutating requests. The cookie value is accepted verbatim: the config
  uses `CsrfTokenRequestAttributeHandler` instead of Spring's default XOR-masked
  handler, because a masked token cannot be obtained from the cookie.
- Public endpoints by design: `GET /api/v1/workspace`, `POST /api/v1/bootstrap`
  (first admin only, no members yet), `POST /api/v1/register` (only when
  `signup_mode=open`), `POST /api/v1/invites/accept`. Admin-only: member role/status
  actions, invite create/list/revoke. Method security (`@PreAuthorize`) + service
  layer checks; suspended members can sign in but cannot write (`MemberAccess`).
- GraphQL client codegen (DGS) runs on `generate` against
  `src/main/resources/graphql-client/` → `build/generated/sources`, package
  `io.github.james5635.chafgram.codegen`. Generated code is not committed source you hand-edit.
- Migrations go in `src/main/resources/db/migration/` as `V<n>__snake_case.sql` (Flyway).
  The migration files are the schema of record. `spring.jpa.hibernate.ddl-auto`
  is `validate` — Hibernate never creates or updates; Flyway owns the schema.
  Baseline: `V1__init.sql`; `V2` aligns media position columns; `V3` adds
  `users.email_verified_at` (FR-1.7).
- Both `spring-boot-starter-webmvc` and `-webflux` are present; prefer the MVC/servlet path
  unless a reactive endpoint is explicitly needed.

### Frontend (`frontend/`)

```bash
npm install
npm run dev                # dev server on port 3000
npm run generate           # GraphQL codegen (also run by `just generate`)
npm run build              # production build (also regenerates the route tree)
npm run preview            # serve the production build
```

- Routes are files in `src/routes/`; `src/routeTree.gen.ts` is **generated — never edit
  it**. Add a route by adding a file; the tree regenerates on dev/build.
- **Backend access goes through the BFF.** `src/lib/backend-proxy.ts` is the only module
  that talks to the Spring Boot API; server functions (`graphqlRequest`,
  `loginRequest`, `logoutRequest`, `uploadMedia`) wrap it. Never `fetch` the backend
  from a component, and never expose the backend URL to the browser.
- **Cookies and CSRF are relayed by the BFF.** The browser holds the session cookie
  (`JSESSIONID`) and the double-submit `XSRF-TOKEN`; the proxy forwards them and copies
  the backend's `Set-Cookie` headers back. A 403 that carries a freshly issued CSRF
  token is retried once — do not bypass that retry.
- **GraphQL:** operations live in `src/graphql/*.graphql`; `just generate` regenerates
  `src/generated/graphql.ts` from the backend schema (git-ignored — never hand-edit).
  Read with Apollo `useQuery`, write with `useMutation`; the client is provided once in
  `src/routes/__root.tsx`.
- Styling is Tailwind v4 (CSS-first, via `@tailwindcss/vite`) plus design tokens in
  `src/styles.css`. Use the tokens (`--lagoon`, `--sea-ink`, …), not raw hex; see
  `design.md`.
- Light/dark/auto theming: inline `THEME_INIT_SCRIPT` in `src/routes/__root.tsx` sets
  `data-theme` on `<html>` before paint; mode persists in `localStorage["theme"]`. Keep
  them consistent if you touch either.
- Devtools (TanStack Router/Devtools) render in the corner in dev; leave them out of any
  production-only code paths you author.
- `frontend/AGENTS.md` contains an `intent-skills` block: before substantial work here,
  run `npm exec --no -- intent list` and load a matching local skill if one exists.

## Conventions

- **Java:** 21, Lombok for boilerplate, constructor injection, records for DTOs where
  practical. Keep the base package `io.github.james5635.chafgram`.
- **API surface:** GraphQL-first — everything goes through `/graphql` (social, identity,
  invites, workspace, messaging, moderation). REST only for the documented keep-list in
  `docs/architecture.md` (session ceremonies, WebAuthn, media upload/bytes, multipart
  message sends, Actuator).
  Resolver classes carry class-level `@PreAuthorize("isFullyAuthenticated()")`; only
  explicitly public operations carry `permitAll()` (workspace branding query).
  Authorization failures return HTTP 200 + `errors[]` with `extensions.classification`,
  not HTTP 403/401 — tests must assert on `$.errors`, not the status code.
- **Frontend:** TypeScript strict; function components; colocate components in
  `src/components/`; routes thin, components dumb; server logic via TanStack Start server
  functions, not ad-hoc fetch wrappers.
- **Commits:** `<type>(<scope>): <subject>` — e.g. `feat(backend): add passkey
  registration`, `fix(db): align media position columns with entity mappings`,
  `docs(docs): split plans into FR/BR`. Types: `feat`, `fix`, `docs`, `refactor`,
  `test`, `chore`, `build`, `ci`. Scope: the area touched (`backend`, `frontend`, `db`,
  `docs`, `justfile`, …). Subject is imperative with no trailing period; describe
  *why*, not just *what*. PR titles follow the same format.
- **Imports:** Never use fully-qualified class names (FQCN) inline. Always import the class and use the simple name.
  - Test utilities: `import org.springframework.mock.web.MockMultipartFile;` → `new MockMultipartFile(...)`
  - Test utilities: `import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;` → `.with(csrf())`
  - All other code: same rule — import, don't FQCN.
  - Avoid `org.springframework.mock.web.MockMultipartFile` or `org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()` inline anywhere.
- **Docs-first for direction:** if a change contradicts `product.md`/`design.md`, update
  the doc in the same change or flag it — don't fork the truth in code.

## Testing (mandatory)

**Every implementation ships with tests in the same change.** There is no "tests later".
This applies to new features, bug fixes, refactors, config/build changes that alter behavior,
and any other code change that can regress. A change without tests is not done.

- **Frontend (`frontend/`):** Vitest + React Testing Library (`jsdom`). Colocate tests as
  `*.test.ts` / `*.test.tsx` next to the source they cover. Mock TanStack Router's `Link`,
  `useLocation`, and `useParams` to render components in isolation (see
  `src/components/PostCard.test.tsx`, `src/routes/-screens.test.tsx`). Cover the happy path,
  visible states/disabled states, and user interactions (typing, clicking chips/tabs/buttons).
  Run `just frontend::test`.
- **Backend (`backend/`):** JUnit + MockMvc integration tests against Testcontainers MySQL
  (hermetic). Cover success, validation/authorization failures, boundary cases, and GraphQL
  `errors[]` extensions where relevant. Add or update a test for every endpoint/resolver or
  service-rule change. Run `just backend::test`.
- **Shared helpers/pure logic:** these are the easiest tests to write — extract a predicate
  or mapper and unit-test it directly (e.g. `src/lib/public-paths.test.ts`). Do not hide
  behavior inside a component just because it is hard to test.
- **What a test must prove:** the new behavior works; the old behavior it replaced does not
  regress; error and empty states are handled; accessibility-relevant labels/roles where the
  UI changed. Assert on observable output, never on implementation details.
- **Bug fixes:** write a failing test that reproduces the bug first, then make it pass. The
  regression test stays in the suite permanently.
- **Verification:** run `just verify` (tests + builds) before claiming done; the pre-push hook
  installed by `just bootstrap` enforces this. Never disable or skip it without saying so.
- **Docs-only changes** (markdown, specs, AGENTS.md updates) are exempt from code tests, but
  still run `just verify` when the docs describe shipped behavior.

## Guardrails

- Never commit secrets (`application.properties` with real credentials, `.env`, keys).
  Use env vars or local, git-ignored profiles.
- Don't commit build output: `backend/build/`, `frontend/.output/`, `dist/`,
  `node_modules/`.
- Don't vendor fonts/CDN assets silently; the app currently loads Google Fonts via
  `styles.css` `@import` — flag it if self-hosting is needed for offline deployments
  (it will be, for on-prem air-gapped installs — see product.md delivery model).
- No telemetry/analytics without explicit, opt-in configuration. Chafgram's promise is
  that company data stays with the company.
- Treat `HELP.md` (Spring Initializr reference) as boilerplate; keep or remove, don't
  extend it.
- Ask before deleting or rewriting anything you didn't create; the repo is young but
  every file is someone's decision.
- `.agents/` is dev-only tooling: tracked, but never a dependency of the product and
  never bundled into a deployable artifact.

## Definition of done for a change

1. Tests written and committed for the change (see "Testing (mandatory)" above), including a
   regression test for any bug fix. `just verify` is green — this runs backend + frontend
   tests and both builds.
2. Backend: `./gradlew test` green; migration (if any) is forward-only and reversible by a
   documented restore; OpenAPI/GraphQL schema updated where applicable.
3. Frontend: `npm run build` clean; new UI verified in light/dark/auto; keyboard focus
   visible; tokens used, not magic values.
4. Docs updated (`README.md`, `product.md`, `design.md`, this file) if behavior, scope, or
   decisions changed.
5. No secrets, no generated files in the diff.

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
backend/    Spring Boot 4 (Java 21) service — Maven wrapper, package io.github.james5635.chafgram
frontend/   TanStack Start (React 19, Vite, Tailwind v4) app — file-based routing
compose.yaml  Local dev MySQL 8.0 (dev-only throwaway credentials)
docs/functional-requirements.md  Functional requirements (FR)
docs/business-rules.md  Business rules (BR)
docs/architecture.md  Architecture (system design)
docs/development-workflow.md  Setup, dev loop, DB, testing, verification
product.md  Product definition (what/who/why, goals, non-goals)
design.md   Design system (tokens, components, motion, a11y)
README.md   Human-facing overview and quick start
.agents/    Local agent tooling/skills — not part of the product, don't ship it
```

## Commands

A `justfile` at the repo root wraps common dev tasks — `just` lists them,
`just dev` runs backend + frontend (run `just mysql` first for the database), `just test` runs the test suites, `just verify` runs just test + just build.
Per-package recipes live in `backend/justfile` and `frontend/justfile` — `just install`
is a root alias for `backend::install` + `frontend::install`. The raw commands below remain the source of truth.

### Backend (`backend/`)

```bash
just mysql                 # start local MySQL 8.0 via compose.yaml, wait for healthy (stop with: docker compose down)
just seed                  # RESET dev DB: wipe, Flyway-migrate, insert full demo fixtures (admin + members, posts, comments, DMs, notifications, reports, audit log)
docker compose up -d       # start MySQL AND Garage (S3-compatible dev object storage, bucket chafgram-media)
docker compose up garage-init   # one-shot: layout assign, dev key import, bucket create, permissions — idempotent
./mvnw spring-boot:run     # run dev server (default port 8080)
./mvnw test                # run tests (wipes the dev chafgram schema)
just migrate               # apply Flyway migrations only, no data wiped
just unseed                # EMPTY dev DB: wipe, Flyway-migrate, no fixtures
./mvnw package             # build jar (target/)
```

`just dev` / `just backend::dev` / `just frontend::dev` do not start the database — run `just mysql` first.

- Requires JDK 21. Local dev MySQL runs via `compose.yaml`; the datasource in
  `application.properties` defaults to the compose dev credentials
  (`chafgram` / `chafgram`, db `chafgram`, overridable via `SPRING_DATASOURCE_*` env
  vars or a git-ignored `.env`/profile). Never commit real credentials.
- Dev object storage: Garage (S3-compatible API on `http://localhost:3900`, region
  `garage`, bucket `chafgram-media`, dev key + secret in `scripts/garage-init.sh`).
  Bucket/layout/key are created by `garage-init` on first run — re-running is safe.
- `./mvnw test` uses the dev MySQL from `compose.yaml` (`just mysql` must be running)
  and **wipes the dev `chafgram` schema on every run**. Afterwards run `just seed` to
  restore the dev fixtures. Override the connection with
  `CHAFGRAM_TEST_DATASOURCE_URL|_USERNAME|_PASSWORD` (it wipes whatever it points at —
  not for shared databases).
- Identity Backend modules implemented: `identity` (users/invites/workspace, REST
  `/api/v1`), `social` (posts/comments/reactions/follows/feed/search + GraphQL at
  `/graphql`), `messaging` (1:1 + groups, REST), `notifications`, `moderation`
  (reports + audit log), `media` (local-disk upload). See `docs/architecture.md` for the
  system design.
- Passkeys are handled by Spring Security WebAuthn; the `user_entities` and
  `user_credentials` tables are framework-managed — do not add JPA entities on top of
  them or alter their DDL. Configure via env `CHAFGRAM_WEBAUTHN_RP_ID` (default
  `localhost`) and `CHAFGRAM_WEBAUTHN_ALLOWED_ORIGINS` (default `http://localhost:3000`,
  comma-separated if several).
- Authentication is session-cookie based; passkey ceremonies are at
  `/webauthn/register/*` (authenticated) and `/login/webauthn` +
  `/webauthn/authenticate/options` (unauthenticated). CSRF: double-submit cookie,
  readable JS header `X-XSRF-TOKEN` (CookieCsrfTokenRepository) — BFF clients must
  send it on mutating requests.
- Public endpoints by design: `GET /api/v1/workspace`, `POST /api/v1/bootstrap`
  (first admin only, no members yet), `POST /api/v1/register` (only when
  `signup_mode=open`), `POST /api/v1/invites/accept`. Admin-only: member role/status
  actions, invite create/list/revoke. Method security (`@PreAuthorize`) + service
  layer checks; suspended members can sign in but cannot write (`MemberAccess`).
- GraphQL client codegen (DGS) runs on `generate` against
  `src/main/resources/graphql-client/` → `target/generated-sources`, package
  `io.github.james5635.chafgram.codegen`. Generated code is not committed source you hand-edit.
- Migrations go in `src/main/resources/db/migration/` as `V<n>__snake_case.sql` (Flyway).
  The migration files are the schema of record. `spring.jpa.hibernate.ddl-auto`
  is `validate` — Hibernate never creates or updates; Flyway owns the schema.
- Both `spring-boot-starter-webmvc` and `-webflux` are present; prefer the MVC/servlet path
  unless a reactive endpoint is explicitly needed.

### Frontend (`frontend/`)

```bash
npm install
npm run dev                # dev server on port 3000
npm run build              # production build (also regenerates the route tree)
npm run preview            # serve the production build
```

- Routes are files in `src/routes/`; `src/routeTree.gen.ts` is **generated — never edit
  it**. Add a route by adding a file; the tree regenerates on dev/build.
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
  `docs/architecture.md` (session ceremonies, WebAuthn, media upload/bytes, Actuator).
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

## Guardrails

- Never commit secrets (`application.properties` with real credentials, `.env`, keys).
  Use env vars or local, git-ignored profiles.
- Don't commit build output: `backend/target/`, `frontend/.output/`, `dist/`,
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

## Definition of done for a change

1. Backend: `./mvnw test` green; migration (if any) is forward-only and reversible by a
   documented restore; OpenAPI/GraphQL schema updated where applicable.
2. Frontend: `npm run build` clean; new UI verified in light/dark/auto; keyboard focus
   visible; tokens used, not magic values.
3. Docs updated (`README.md`, `product.md`, `design.md`, this file) if behavior, scope, or
   decisions changed.
4. No secrets, no generated files, no `.agents/` content in the diff.

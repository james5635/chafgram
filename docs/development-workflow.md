# Chafgram — Development Workflow

> How to work on Chafgram day to day: setup, the dev loop, the database, testing,
> verification, and the rules of the road. Conventions and guardrails live in
> [`AGENTS.md`](../AGENTS.md); what the product is lives in [`product.md`](../product.md).

## 1. Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 21 | `just doctor` |
| Node.js + pnpm | any current LTS; pnpm 10+ | `just doctor` |
| Docker + Docker Compose | — | `docker compose version` |
| just | latest | `just --version` |
| ffmpeg + ffprobe | any recent | `ffmpeg -version` (video pipeline; images work without it) |

`just` is the entry point for everything below; raw commands are in each section.
Package-specific recipes live in `backend/justfile` (`just backend::<recipe>`) and
`frontend/justfile` (`just frontend::<recipe>`); the root justfile delegates.

`backend/` is the Gradle **workspace root**: the wrapper and `settings.gradle.kts` live
there, and the Spring Boot application is the `api` subproject. Run Gradle from
`backend/` with `./gradlew :api:<task>` (the `just backend::*` recipes already do this).

## 2. First-time setup (fresh clone)

```bash
git clone <remote> chafgram && cd chafgram
cp .env.example .env   # optional — compose/just load it; dev defaults work as-is
just install           # backend (gradle dependencies) + frontend (pnpm install --frozen-lockfile)
just bootstrap         # install local git hooks (pre-push → just verify)
just mysql             # start local MySQL 8.0, wait for healthy
just seed              # WIPES dev DB, migrates, inserts full demo fixtures
just dev               # backend :8080 + frontend :3000 (parallel; Ctrl-C stops both)
```

- Seeded users: `admin` (admin), `maya` / `tao` / `lena` (members) — all with the dev
  password `chafgram-dev-password` — plus a reusable demo invite
  (`/join?token=demo-invite-token`). Details: [README](../README.md#quick-start).
- `.env` is gitignored; `.env.example` is the committed template. The JVM does **not**
  read `.env` — the datasource/WebAuthn defaults in `application.properties` match the
  compose dev credentials; override with real environment variables for anything shared.
  `just` loads `.env` into recipes (`set dotenv-load := true`); `docker compose`
  auto-loads it too.
- Optional, for media uploads: `docker compose up -d garage garage-init` — one-shot,
  idempotent S3 setup (bucket `chafgram-media`, endpoint `http://localhost:3900`,
  region `garage`). Re-running is safe.
- Optional, for video: `ffmpeg` + `ffprobe` on `PATH`. The in-app pipeline shells out
  to them; without them video uploads end in `status='failed'` (images are unaffected).
- The frontend talks to the backend through its BFF (`src/lib/backend-proxy.ts`), which
  reads `CHAFGRAM_API_URL` (default `http://localhost:8080`). Point it at another host by
  exporting that variable before `just dev`.

## 2.1 GraphQL codegen (frontend)

```bash
just generate          # = just frontend::generate
```

Reads the backend's committed `backend/api/src/main/resources/graphql/*.graphqls` and
regenerates `frontend/src/generated/graphql.ts` (git-ignored — never hand-edit it).
Operations live in `frontend/src/graphql/*.graphql`. `pnpm run test` and `pnpm run build`
regenerate the types automatically, so a fresh clone needs no extra step.

## 3. The daily loop

1. `just mysql` (if not running), then `just dev`.
2. Edit backend Java → devtools restarts the app (logs in the `just dev` output);
   edit frontend → Vite hot-reloads.
3. Explore the API with GraphiQL at `http://localhost:8080/graphiql` (dev only —
   disable in production profiles). All operations go through `POST /graphql`.
4. Before you call it done: `just verify` (tests + build). Pushing runs the same check
   automatically via the pre-push hook installed by `just bootstrap`.

## 4. Database & migrations

Flyway owns the schema; Hibernate only validates (`ddl-auto=validate`). The migration
files under `backend/api/src/main/resources/db/migration/` are the **schema of record**.

Rules:

- Migrations are **forward-only and additive**. Never edit an applied migration —
  Flyway checksums enforce this and any applied database fails validation.
  Alignments go in a new `V<n>__snake_case.sql`.
- `just migrate` applies migrations without touching data.
- `just seed` WIPES the dev database and re-seeds full fixtures (asks for `yes`). The
  fixtures live in the `backend/seed` Gradle subproject (`DevFixtureSeeder`), which boots
  a headless Spring context and calls the `:api` services with an explicit acting member;
  `just migrate` / `just unseed` use the `:api:flywayMigrate` Gradle task instead.
- `just unseed` WIPES and leaves an empty migrated schema (asks for `yes`).
- Dev credentials (`chafgram` / `chafgram`, DB `chafgram`) are throwaway, local-only;
  override via `SPRING_DATASOURCE_*` for anything else. Never commit real credentials.

## 5. Testing

Backend integration tests are hermetic: they run against a throwaway MySQL 8
container (Testcontainers), started once per test JVM. The dev database is never
touched, so no `just seed` is needed afterwards. Docker must be running:

```bash
cd backend
./gradlew :api:test                # no `just mysql` needed — Testcontainers starts its own DB
```

Test-only configuration lives in `backend/api/src/test/resources/application-test.properties`
and is activated by the `test` profile (`AbstractIntegrationTest`). Do **not** add a
test-scoped `application.properties`: it shadows the main one on the classpath and
silently drops every real configuration value.

Suites: application context boot, workspace/identity flows, GraphQL surface
(`GraphqlApiTest`), social flow (`SocialFlowIntegrationTest`), admin filters and
audit log (`AdminFilterIntegrationTest`), account flows
(`EmailFlowsIntegrationTest`), actuator exposure (`ActuatorSecurityIntegrationTest`),
schema/DTO sync (`SchemaDtoSyncTest`). Conventions to respect:

- Authorization failures from GraphQL come back as HTTP 200 + `errors[]` with
  `extensions.classification` (`UNAUTHORIZED` for anonymous, `FORBIDDEN` for an
  authenticated member without the role, `BAD_REQUEST`/`NOT_FOUND` for validation and
  lookup failures) — tests assert on `$.errors`, never on the HTTP status. Service-layer
  `ApiException`s are mapped by `common/GraphQlApiExceptionHandler`.
- Resolver classes carry class-level `@PreAuthorize("isFullyAuthenticated()")`; only the
  `permitAll()` operations are public.
- Keep `*.graphqls` files in sync with the DTOs — `SchemaDtoSyncTest` fails otherwise.

**Tests are mandatory for every change** (feature, bug fix, refactor, behavior-affecting
config) — see "Testing (mandatory)" in `AGENTS.md`. Write them in the same change; there is
no "tests later". Bug fixes get a failing regression test first.

Frontend tests use Vitest + React Testing Library (`jsdom`), colocated as `*.test.ts(x)`
next to the source. Mock TanStack Router's `Link` / `useLocation` / `useParams` to render
components in isolation (see `src/components/PostCard.test.tsx` and
`src/routes/-screens.test.tsx`). Run `just frontend::test`.

```bash
cd frontend
pnpm run test
```

Root `just test` runs both suites; `just verify` runs tests + builds and is enforced by the
pre-push hook installed with `just bootstrap`. Docs-only changes are exempt from code tests.

## 6. API conventions (GraphQL-first)

- Everything is GraphQL at `/graphql`. REST exists only for what GraphQL cannot
  express — the keep-list (session ceremonies, WebAuthn endpoints, media
  upload/`bytes`, multipart message sends, Actuator) is documented in
  [`architecture.md`](architecture.md).
- Adding an API: new/extended `.graphqls` (one file per DTO) + resolver + DTO, then
  re-sync `build/generated-sources` via `./gradlew :api:generateJava` (DGS codegen).
- CSRF: double-submit cookie (`XSRF-TOKEN`, readable JS) with an `X-XSRF-TOKEN` header
  required on mutating requests. BFF clients (the TanStack Start server) must send it.
  Anonymous POSTs without a valid pair get 403 — by design, not a bug. The cookie value
  is accepted verbatim (plain `CsrfTokenRequestAttributeHandler`); Spring's default
  XOR-masked handler is explicitly replaced because a masked token cannot be obtained
  from the cookie.
- GraphiQL executes queries but needs the same CSRF pair; the masked token is exposed
  as the hidden `_csrf` field on the `/login` page.
- The operation surface lives in `backend/api/src/main/resources/graphql/schema.graphqls`
  (Query/Mutation roots) with one `.graphqls` file per DTO. Notable groups:
  profile (`updateProfile`), passkeys (`myPasskeys`/`renamePasskey`/`revokePasskey`),
  account flows (`requestEmailVerification`, `requestPasswordReset`, `resetPassword`,
  `verifyEmail`), watch feed (`watchFeed`), message media + editing (`mediaIds`,
  `editMessage`, `deleteMessage`), admin (`members(query:)`, `auditLog(...)`,
  `deleteMedia(id:)`).

## 7. Verification & pushing

```bash
just verify   # = just test + just build; run before every push / on a fresh clone
just bootstrap  # one-time: install the pre-push hook from scripts/hooks/pre-push
```

- **Pre-push hook:** `just bootstrap` copies the tracked
  [`scripts/hooks/pre-push`](../scripts/hooks/pre-push) into `.git/hooks/pre-push`. The hook
  runs `just verify` on every push, so a failing test or build blocks the push. It is
  idempotent and will not overwrite an unrelated existing hook. Skip deliberately with
  `git push --no-verify` (discouraged). Re-run `just bootstrap` after updating the hook
  source; `.git/hooks/` itself is local and never committed.
- All green: backend `Tests run: … Failures: 0`, frontend `Test Files … passed` (Vitest),
  and both client and SSR builds clean.
- Docs-first: if your change contradicts `product.md` / `design.md`, update the doc in
  the same change or flag it — don't fork the truth in code.
- Commits: `<type>(<scope>): <subject>` — e.g. `feat(backend): …`, `fix(db): …`,
  `docs(docs): …`; types `feat|fix|docs|refactor|test|chore|build|ci` (full rule in
  [`AGENTS.md`](../AGENTS.md#conventions)).
- Nothing generated gets committed: `backend/build/`, `frontend/.output/`, `dist/`,
  `node_modules/` are all ignored. Exception: `frontend/src/routeTree.gen.ts` is generated
  but **committed** — never hand-edit it; commit the regenerated file so type-safe routes
  work on a fresh clone.

## 8. Common pitfalls

| Symptom | Cause / fix |
|---------|-------------|
| Port 8080 already in use | A stray `spring-boot:run`/devtools instance; kill it (`pgrep -af ChafgramApplication`). |
| Port 3000 already in use | Another Vite instance; stop it or change port. |
| App fails with `SchemaManagementException … position` | Applied DB drifted from mappings; align with a new migration (see §4), never by editing applied files. |
| `./gradlew :api:test` fails with "Could not find a valid Docker environment" | Testcontainers needs a running Docker daemon — start Docker (or Podman) and retry. |
| Tests hit the dev database / the dev DB looks wiped by a test run | They don't: the suite uses its own throwaway container. A test-scoped `application.properties` is the usual culprit for a *misconfigured* run — rename it to `application-test.properties` (see §5). |
| GraphQL returns 403 from curl | Missing CSRF double-submit pair (cookie + `X-XSRF-TOKEN`) — see §6. |
| `.env` changes ignored by the JVM | The JVM doesn't read `.env`; export the variables or rely on `application.properties` defaults. |
| Flyway checksum validation failure | An applied migration was edited; restore it and add a new `V<n>` alignment instead. |

## 9. See also

- [`architecture.md`](architecture.md) — system design, module map, API keep-list
- [`functional-requirements.md`](functional-requirements.md) / [`business-rules.md`](business-rules.md) — FR/BR
- [`AGENTS.md`](../AGENTS.md) — conventions and guardrails
- [`README.md`](../README.md) — human-facing overview and quick start

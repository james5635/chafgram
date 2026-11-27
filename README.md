# Chafgram

**Chafgram is an open-source, self-hosted private social platform.**

Admins deploy Chafgram on their own infrastructure; their people get the full modern
social experience — short video, watch, posts, comments, follows, and direct messages —
inside the company, with no data ever leaving their control. No public sign-up, no
algorithmic feed for strangers, no phone-home.

- **License:** [MIT](LICENSE)
- **Delivery:** self-hosted / on-premise (single-tenant per company)
- **Status:** early foundation — architecture and design system are in place; product
  features are being built

## Why

Companies want the engagement and culture density of public social media without the
exposure. Work chat carries messages, not culture; public networks can't hold internal
conversation. Chafgram is the internal social layer: familiar to use, private by default,
open source under MIT. See **[product.md](product.md)** for the full product definition.

## What it includes

- Short-video feed and watch
- Posts, comments, and reactions
- Follows and a personal, work-relevant feed
- Direct messages
- Admin workspace: member management, moderation, configuration

The backend API surface behind these is complete against the decided requirements —
see [`docs/functional-requirements.md`](docs/functional-requirements.md) and
[`docs/business-rules.md`](docs/business-rules.md) for what is implemented, and
[`docs/architecture.md`](docs/architecture.md) for how it is put together.

(Feature scope and open questions: [product.md](product.md).)

## Tech stack

| Layer    | Technology |
|----------|------------|
| Backend  | Spring Boot 4 (Java 21), Spring Security JWT resource server (bearer access tokens + DB-backed refresh tokens), Spring Data JPA + MySQL, Flyway migrations, Spring GraphQL (GraphQL-first; REST only for the keep-list in `docs/architecture.md`), Actuator |
| Frontend | TanStack Start + TanStack Router (file-based), React 19, TypeScript 6, Vite 8; Apollo Client 4 with GraphQL Code Generator for typed operations through the BFF |
| UI       | Tailwind CSS v4 (CSS-first, `@tailwindcss/vite`), shadcn-style primitives in `src/components/ui/` built on Base UI (`@base-ui/react`) + `class-variance-authority`, `lucide-react` icons, `tw-animate-css`; TanStack Devtools in dev only |
| Testing  | Backend: JUnit + MockMvc + Testcontainers MySQL (hermetic). Frontend: Vitest 5 + React Testing Library (jsdom) |
| Media    | S3-compatible object storage via the AWS SDK — locally [Garage](https://garagehq.deuxfleurs.fr/) via `compose.yaml` (`garage`, `garage-init`), bucket `chafgram-media`, endpoint `http://localhost:3900`, region `garage`. Local filesystem storage remains available with `CHAFGRAM_MEDIA_BACKEND=local` |
| Design   | "Sea/lagoon" system — Fraunces + Manrope (Google Fonts), Geist Variable via `@fontsource-variable/geist` for primitives, token-driven light/dark/auto themes — see [design.md](design.md) |

## Repository layout

```
backend/      Backend Gradle workspace; Spring Boot service in backend/api/
frontend/     TanStack Start web app
compose.yaml  Local dev MySQL 8.0
docs/functional-requirements.md  Functional requirements (FR, MoSCoW-prioritized)
docs/business-rules.md  Business rules (BR, incl. compliance & trust)
docs/architecture.md  Architecture (system design)
docs/development-workflow.md  Setup, dev loop, DB, testing, verification
product.md    Product definition
design.md     Design system
AGENTS.md     Guide for AI agents and contributors
```

## Quick start

A [`justfile`](justfile) wraps the common dev tasks (`just` to list, `just install` to
fetch backend and frontend dependencies, `just dev` to run both apps, `just test` for
the test suite, `just verify` for tests + build).
Per-package recipes live in `backend/justfile` and `frontend/justfile` (e.g. `just install`
runs `backend::install` and `frontend::install`). The raw commands:

### Backend

```bash
just mysql          # start local MySQL 8.0 and wait until healthy, or: docker compose up -d --wait mysql
cd backend
./gradlew :api:bootRun
```

Run `just mysql` first if the backend needs the database (it is not auto-started).
The app no longer migrates on boot — run `just migrate` once before `bootRun`
(or `just db-reset` then `just migrate`, optionally followed by `just seed`).

```bash
./gradlew :api:test   # hermetic — Testcontainers starts a throwaway MySQL 8; the dev DB is untouched
just seed           # insert fixtures into an empty, migrated dev DB
just migrate        # apply Flyway migrations only (data untouched)
just db-reset       # drop all objects in the dev DB schema (Flyway clean)
```

`just seed` populates an empty, migrated dev database with a demo workspace:
users `admin` (admin) and `maya` / `tao` / `lena` (members) — all with the dev password
`chafgram-dev-password` — plus a reusable demo invite (`/join?token=demo-invite-token`),
a follows graph, 19 posts (notes, photos, five videos and nine photo posts from
`backend/seed/src/main/resources/images`, plus one moderator-tombstoned post), comments
(with a reply), reactions, saved posts, a block, 2 direct chats + a group chat with
messages (text + one image), all notification types, one open and one actioned report,
audit-log entries and an email-verification token row. Fixtures are generated by the
`backend/seed` Gradle subproject through the application's services — there is no seed
SQL script. Image assets have real blobs under `demo/...` in the `chafgram-media`
Garage bucket; video posts also have short generated MP4 blobs and play inline in the feed. Dev fixtures only.

The API is GraphQL-first (see `docs/architecture.md`): all operations go through
`POST /graphql` — social data (feed, watch feed, posts, comments, reactions, follows),
identity (members, workspace, profile), invites, account flows (email
verification, password reset), messaging, reports, audit log, and media metadata.
Explore it with GraphiQL at `http://localhost:8080/graphiql` (dev). Requests carry
`Authorization: Bearer <accessToken>`; with **no** `Authorization` header at all, a
GraphQL call reaches the resolver and returns HTTP 200 with a GraphQL `errors[]`
(classification `UNAUTHORIZED` for anonymous, `FORBIDDEN` for an authenticated member
without the role). An invalid, expired or foreign-signature token is rejected at the
security filter with HTTP 401. Every resolver class enforces `isFullyAuthenticated()`
except explicitly-public operations. There are no HTTP sessions and no CSRF tokens.

REST exists only for what GraphQL cannot express — the keep-list lives in
`docs/architecture.md`: auth (`POST /api/v1/auth/login`, `/refresh`, `/logout`),
first-admin bootstrap / signup / invite claim (`POST /api/v1/bootstrap`,
`/api/v1/register`, `/api/v1/invites/accept`, all issuing token pairs), media upload
(`POST /api/v1/media`), signed media URL creation (`GET /api/v1/media/{id}/signed-url`)
and byte serving (`GET /api/v1/media/{id}/bytes`), multipart message sends
(`POST /api/v1/messages`, image attachments inline — videos are uploaded first and
attached by id once the pipeline marks them ready), and Actuator (`/actuator/health`
public; `/actuator/info` and `/actuator/metrics` admin-only). Public branding is a
public GraphQL query: `{ workspace { name slug } }`.

Requires JDK 21. The datasource defaults in `application.properties` match the
`compose.yaml` dev credentials (`chafgram` / `chafgram`, database `chafgram`); override
with `SPRING_DATASOURCE_*` environment variables or a git-ignored profile for anything
beyond local dev — never commit credentials. Migrations are applied by Flyway from
`db/migration/`.
Token auth is configured with `CHAFGRAM_JWT_SECRET` (≥32 bytes, throwaway dev default —
set a unique secret in production), `CHAFGRAM_JWT_ISSUER` / `CHAFGRAM_JWT_AUDIENCE`,
`CHAFGRAM_ACCESS_TTL` (15m), `CHAFGRAM_REFRESH_TTL` (14d), `CHAFGRAM_COOKIE_SECURE` and
`CHAFGRAM_MEDIA_URL_TTL` (1h); see `.env.example`. Login returns a short-lived access
JWT (kept in browser memory by the BFF) and sets the HttpOnly `chafgram_refresh` cookie
(path `/`, `SameSite=Strict`, `Secure` outside dev/test). Refresh rotates the row. Any
presented revoked refresh token is rejected; only one revoked without a replacement
(logout or suspected theft) also revokes all of that member's refresh tokens.

#### Email (optional)

Email verification and password reset are opt-in and never required to use the
instance. With no mail server configured, Chafgram writes the message (including the
one-time link) to the application log — so the flows stay exercisable on a fresh clone
and air-gapped installs make no outbound calls.

To send real mail, set `CHAFGRAM_MAIL_SMTP_ENABLED=true` plus `SPRING_MAIL_HOST`,
`SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD`
(`SPRING_MAIL_SMTP_AUTH` / `SPRING_MAIL_SMTP_STARTTLS` as needed), and point
`CHAFGRAM_APP_URL` at the frontend so the links resolve (`http://localhost:3000` by
default). See `.env.example` for the full list. Both flows use single-use tokens that
expire after 30 minutes; only their SHA-256 hash is stored.

#### Video processing

Video uploads are processed in the backend by a bounded async pipeline
(`ffprobe` → `ffmpeg` transcode → thumbnail → blurhash), so `ffmpeg` and `ffprobe` must
be on `PATH` for videos to finish (`status='ready'`). Without them a video upload ends
in `status='failed'`; images are unaffected. Short videos (≤ 60 s) surface in the watch
feed, longer ones in the regular feed. If the pipeline queue is full, the upload ends
in `status='failed'` as well — it never stays stuck in `status='processing'`. The
generated thumbnail is kept and served through the byte endpoint
(`GET /api/v1/media/{id}/bytes?variant=thumbnail`).

#### Signed media URLs

`<img>`/`<video>` cannot attach a bearer header, so GraphQL `Post.mediaUrls` carries
backend-signed BFF URLs (`/api/media/{id}?exp=…&uid=…&sig=…`) valid for 1 hour. The BFF
relays them to `GET /api/v1/media/{id}/bytes`, which validates the signature, expiry
and viewer id without a bearer. On a media load error the client asks the BFF for one
fresh signed URL (`GET /api/v1/media/{id}/signed-url`, bearer-protected) and swaps `src`
once. Signing never consumes or rotates the refresh token.

### Frontend

```bash
cd frontend
pnpm install
pnpm run dev       # http://localhost:3000
```

```bash
pnpm run build     # production build
pnpm run preview   # serve the production build
```

### Using the app (dev)

The frontend now has Chafgram screens wired to the backend via GraphQL (Apollo + BFF).
Every step below was verified against a fresh `just seed` on a local MySQL.

1. **Sign in.** Make sure the dev fixtures are loaded (`just seed`), start the backend,
   and open the frontend login page at **http://localhost:3000/login**.

   **Dev credentials (all use the same password):**

   | Role | Username | Email | Password |
   |------|----------|-------|----------|
   | Admin | `admin` | `admin@chafgram.test` | `chafgram-dev-password` |
   | Member | `maya` | `maya@chafgram.test` | `chafgram-dev-password` |
   | Member | `tao` | `tao@chafgram.test` | `chafgram-dev-password` |
   | Member | `lena` | `lena@chafgram.test` | `chafgram-dev-password` |

   You can sign in with **either the username or the email**. A successful sign-in
   redirects to `/` (the feed). If sign-in fails, run `just seed` and try again.

   There's also a reusable demo invite token: `demo-invite-token` (use at
   `/join?token=demo-invite-token`).
2. **Query the API after sign-in.** GraphiQL is served at
   http://localhost:8080/graphiql (dev only). Sign in through the app (or call
   `POST /api/v1/auth/login`) and copy the `accessToken` from the JSON response into
   GraphiQL's **Headers** tab:

   ```json
   { "Authorization": "Bearer <accessToken>" }
   ```

   There is no CSRF token to copy. When the 15-minute access JWT expires, call
   `POST /api/v1/auth/refresh` with the `chafgram_refresh` cookie to get a new pair;
   the app's BFF does this automatically and retries the failed request once.

   Examples (verified against the seeded fixtures):

   ```graphql
   query { me { username role } }                      # → admin / admin
   query { unreadNotifications }                       # → 4 for the seeded admin
   query { followingFeed(first: 2) { posts { body } } }  # seeded fixture posts
   ```

   **Note:** The frontend carries the access token through the BFF automatically —
   only direct GraphiQL/backend calls need the `Authorization` header above.

3. **Public, no sign-in:** `{ workspace { name slug } }` returns the workspace
   branding. Everything else is `isFullyAuthenticated()` — a GraphQL call with no
   `Authorization` header gets `HTTP 200` with `errors[]`
   (`classification: UNAUTHENTICATED`/`UNAUTHORIZED`), while an invalid, expired or
   foreign-signature bearer token gets `HTTP 401`.
4. **Fixture content** to explore once signed in: posts/comments/reactions, a follows
   graph, direct + group conversations, notifications, moderation reports and the
   audit log. Seeded media has real blobs in Garage once you run
   `docker compose up -d garage garage-init`; feed media is served through 1-hour
   signed URLs.
5. **Token auth:** the BFF keeps the access JWT in memory, silently refreshes it on
   `401`, and sends the visitor to `/login` if refresh fails. Passkeys are removed.
6. **Invite flow:** accept the seeded reusable demo invite via
   `POST /api/v1/invites/accept` (REST keep-list) — `demo-invite-token`.

## Documentation

- **[product.md](product.md)** — what Chafgram is, who it's for, goals/non-goals, open
  questions
- **[docs/functional-requirements.md](docs/functional-requirements.md)** — what Chafgram
  must do (FR, MoSCoW-prioritized)
- **[docs/business-rules.md](docs/business-rules.md)** — the rules it must obey (BR, incl.
  compliance & trust and open owner decisions)
- **[docs/architecture.md](docs/architecture.md)** — architecture: system topology, module
  map, API surface, media/messaging pipelines, deployment and operations
- **[docs/access-refresh-tokens-design.md](docs/access-refresh-tokens-design.md)** — shipped
  JWT access / refresh token design (implementation status, replay rules, signed media URLs)
- **[docs/development-workflow.md](docs/development-workflow.md)** — how to work on the
  repo: first-time setup, the dev loop, database/migrations, testing, pre-push checks
- **[design.md](design.md)** — design system: tokens, theming, components, motion,
  accessibility
- **[AGENTS.md](AGENTS.md)** — repo conventions, commands, and guardrails for contributors
  and AI agents

## Contributing

1. Read [AGENTS.md](AGENTS.md) and the relevant product/design docs.
2. Keep changes small and consistent with the existing structure and tokens.
3. Write tests for every change (new feature, bug fix, refactor) — see the mandatory
   testing rules in [AGENTS.md](AGENTS.md). There is no "tests later".
4. Run `just verify` (backend + frontend tests and builds) before pushing; the pre-push hook
   installed by `just bootstrap` enforces it. Update docs when behavior or scope changes.

## License

[MIT](LICENSE) © Chafgram contributors.

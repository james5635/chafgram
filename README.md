# Chafgram

**Chafgram is an open-source, self-hosted private social platform for companies.**

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

(Feature scope and open questions: [product.md](product.md).)

## Tech stack

| Layer    | Technology |
|----------|------------|
| Backend  | Spring Boot 4 (Java 21), Spring Security + WebAuthn (passkeys), Spring Data JPA + MySQL, Flyway migrations, Spring GraphQL (GraphQL-first; REST only for the keep-list in `docs/architecture.md`), Actuator |
| Frontend | TanStack Start (React 19), TanStack Router (file-based), Vite, Tailwind CSS v4, TypeScript |
| Media    | S3-compatible object storage — locally [Garage](https://garagehq.deuxfleurs.fr/) via `compose.yaml` (`garage`, `garage-init`), bucket `chafgram-media`, endpoint `http://localhost:3900`, region `garage` |
| Design   | "Sea/lagoon" system — Fraunces + Manrope, token-driven light/dark/auto themes — see [design.md](design.md) |

## Repository layout

```
backend/      Spring Boot service (Maven wrapper)
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
./mvnw spring-boot:run
```

Run `just mysql` first if the backend needs the database (it is not auto-started).

```bash
./mvnw test        # hermetic — Testcontainers starts a throwaway MySQL 8; the dev DB is untouched
just seed           # reset dev DB: wipe, Flyway-migrate, insert full fixtures
just migrate        # apply Flyway migrations only (data untouched)
just unseed         # empty dev DB: wipe, Flyway-migrate, no fixtures
```

`just seed` resets the dev database to a fully populated demo workspace:
users `admin` (admin) and `maya` / `tao` / `lena` (members) — all with the dev password
`chafgram-dev-password` — plus a reusable demo invite (`/join?token=demo-invite-token`),
a follows graph, posts (note/photo/video and one moderator-tombstoned post), comments
(with a reply), reactions, saved posts, a block, 2 direct chats + a group chat with
messages (text + one image), all notification types, one open and one actioned report,
audit-log entries and an email-verification token row. Media rows reference fake
`demo/...` keys — no blobs exist until real uploads happen. Dev fixtures only.

The API is GraphQL-first (see `docs/architecture.md`): all operations go through
`POST /graphql` — social data (feed, posts, comments, reactions, follows), identity
(members, workspace), invites, messaging, reports, audit log. Explore it with
GraphiQL at `http://localhost:8080/graphiql` (dev). Authorization failures return
HTTP 200 with a GraphQL `errors[]` (classification `UNAUTHENTICATED`/`UNAUTHORIZED`),
and every resolver class enforces `isFullyAuthenticated()` except explicitly-public
operations. CSRF is enforced for browser clients.

REST exists only for what GraphQL cannot express — the keep-list lives in
`docs/architecture.md`: session ceremonies (`/api/v1/bootstrap`, `/api/v1/register`,
`/api/v1/invites/accept`), Spring Security's WebAuthn endpoints, media upload
(`POST /api/v1/media`) and byte serving (`GET /api/v1/media/{id}/bytes`), and
Actuator. Public branding is a public GraphQL query: `{ workspace { name slug } }`.

Requires JDK 21. The datasource defaults in `application.properties` match the
`compose.yaml` dev credentials (`chafgram` / `chafgram`, database `chafgram`); override
with `SPRING_DATASOURCE_*` environment variables or a git-ignored profile for anything
beyond local dev — never commit credentials. Migrations are applied by Flyway from
`db/migration/`.
`CHAFGRAM_WEBAUTHN_RP_ID` and `CHAFGRAM_WEBAUTHN_ALLOWED_ORIGINS` configure the WebAuthn
relying party (defaults: `localhost`, `http://localhost:3000`).

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

### Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

```bash
npm run build      # production build
npm run preview    # serve the production build
```

### Using the app (dev)

The frontend is still the TanStack Start template (two placeholder routes) — no
Chafgram screens yet. The backend is fully built, so the app is usable today over
HTTP. Every step below was verified against a fresh `just seed` on a local MySQL.

1. **Sign in.** Make sure the dev fixtures are loaded (`just seed`), start the backend,
   and open the default login page at http://localhost:8080/login. Form sign-in:
   `admin` (admin role) or `maya` / `tao` / `lena` (members), password
   `chafgram-dev-password` — a successful sign-in redirects to `/`. If you land on
   `/login?error`, the dev database was wiped (the test suite wipes it) — run
   `just seed` and try again.
2. **Query the API after sign-in.** GraphiQL is served at
   http://localhost:8080/graphiql (dev only). The page loads, but its requests need
   the CSRF double-submit pair: Spring renders a hidden `_csrf` input on the
   `/login` page — the masked token that decrypts to the `XSRF-TOKEN` cookie. Copy
   that value into GraphiQL's **Headers** tab:

   ```json
   { "X-XSRF-TOKEN": "<hidden _csrf value from the /login page>" }
   ```

   The session cookie is sent automatically. If requests start returning `403` after
   you sign in, the token rotated — reload `/login` and copy a fresh `_csrf`.

   Examples (verified against the seeded fixtures):

   ```graphql
   query { me { username role } }                      # → admin / admin
   query { unreadNotifications }                       # → 4 for the seeded admin
   query { followingFeed(first: 2) { posts { body } } }  # seeded fixture posts
   ```

3. **Public, no sign-in:** `{ workspace { name slug } }` returns the workspace
   branding. Everything else is `isFullyAuthenticated()` — anonymous callers get
   `HTTP 200` with `errors[]` (`classification: UNAUTHENTICATED`/`UNAUTHORIZED`),
   or `403` if the CSRF pair is missing entirely (by design).
4. **Fixture content** to explore once signed in: posts/comments/reactions, a follows
   graph, direct + group conversations, notifications, moderation reports and the
   audit log. Seeded media rows reference fake `demo/…` keys (no blobs) — real
   uploads require Garage (`docker compose up -d garage garage-init`).
5. **Passkeys** are wired at the WebAuthn endpoints (`/webauthn/register/*`,
   `/login/webauthn`) but have no UI yet; the seeded accounts use the password path.
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
- **[docs/development-workflow.md](docs/development-workflow.md)** — how to work on the
  repo: first-time setup, the dev loop, database/migrations, testing, pre-push checks
- **[design.md](design.md)** — design system: tokens, theming, components, motion,
  accessibility
- **[AGENTS.md](AGENTS.md)** — repo conventions, commands, and guardrails for contributors
  and AI agents

## Contributing

1. Read [AGENTS.md](AGENTS.md) and the relevant product/design docs.
2. Keep changes small and consistent with the existing structure and tokens.
3. `./mvnw test` and `npm run build` must pass; update docs when behavior or scope changes.

## License

[MIT](LICENSE) © Chafgram contributors.

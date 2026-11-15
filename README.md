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
| Backend  | Spring Boot 4 (Java 21), Spring Security + WebAuthn (passkeys), Spring Data JPA + MySQL, Flyway migrations, Spring GraphQL (+ REST via springdoc-openapi), Actuator |
| Frontend | TanStack Start (React 19), TanStack Router (file-based), Vite, Tailwind CSS v4, TypeScript |
| Design   | "Sea/lagoon" system — Fraunces + Manrope, token-driven light/dark/auto themes — see [design.md](design.md) |

## Repository layout

```
backend/      Spring Boot service (Maven wrapper)
frontend/     TanStack Start web app
compose.yaml  Local dev MySQL 8.0
docs/plans.md    Functional requirements & business rules
docs/architecture.md  Architecture (system design)
docs/roadmap.md    Build order (phases, milestones)
docs/db-schema.md  Database schema (design rationale + V1 baseline)
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
./mvnw test        # requires `just mysql` — WIPES the dev `chafgram` schema; restore with `just seed`
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

The API base is `/api/v1` (see `docs/architecture.md`). Public endpoints by design:
`GET /api/v1/workspace`, `POST /api/v1/bootstrap` (first member only), and
`POST /api/v1/invites/accept`. Everything else requires an authenticated session
(passkey or password); admin actions require `ROLE_ADMIN`. Example — see the OpenAPI
UI at `http://localhost:8080/swagger-ui.html` after signing in.

Requires JDK 21. The datasource defaults in `application.properties` match the
`compose.yaml` dev credentials (`chafgram` / `chafgram`, database `chafgram`); override
with `SPRING_DATASOURCE_*` environment variables or a git-ignored profile for anything
beyond local dev — never commit credentials. Migrations are applied by Flyway from
`db/migration/`.
`CHAFGRAM_WEBAUTHN_RP_ID` and `CHAFGRAM_WEBAUTHN_ALLOWED_ORIGINS` configure the WebAuthn
relying party (defaults: `localhost`, `http://localhost:3000`).

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

## Documentation

- **[product.md](product.md)** — what Chafgram is, who it's for, goals/non-goals, open
  questions
- **[docs/plans.md](docs/plans.md)** — functional requirements & business rules (FR/BR,
  MoSCoW-prioritized, roles: member/moderator/admin)
- **[docs/architecture.md](docs/architecture.md)** — architecture: system topology, module
  map, API surface, media/messaging pipelines, deployment and operations
- **[docs/roadmap.md](docs/roadmap.md)** — build order: phases, milestones, exit criteria
- **[docs/db-schema.md](docs/db-schema.md)** — database schema: design rationale, tables,
  indexes, migrations
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

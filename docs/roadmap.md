# Chafgram — Build Plan

> Phased roadmap for building Chafgram (open-source, self-hosted private social platform
> for companies). Grounded in [`product.md`](../product.md), [`design.md`](../design.md),
> and [`db-schema.md`](db-schema.md). Items marked **(proposal)** are recommendations —
> confirm or replace them as decisions are made. Nothing here is a commitment beyond
> what `product.md` already states.

## 1. What exists today (baseline)

- **Backend**: Spring Boot 4 skeleton on Java 21 (`ChafgramApplication`, context-load test
  only). Starters resolved for WebMVC + WebFlux, Spring Security + WebAuthn, GraphQL +
  DGS codegen, JPA + Flyway + MySQL 8.0, Actuator, springdoc-openapi. No domain code, no
  entities, no endpoints.
- **Database**: `V1__init.sql` Flyway baseline exists and applies cleanly to MySQL 8.0
  (21 tables). Not yet wired to the app (datasource is configured for local dev).
- **Frontend**: TanStack Start (React 19, Vite, Tailwind v4) starter shell with the
  sea/lagoon design system (`design.md`). Two placeholder routes; no app features.
- **Tooling**: `justfile` (`just dev|mysql|check|verify`), `compose.yaml` (MySQL 8.0),
  `AGENTS.md` (working agreements).

## 2. Ordering rationale

User-visible value, in the order that unblocks everything else. Each phase leaves the
product shippable and the schema stable. **(proposal)** — reorder freely; dependencies
are the real constraint, not the sequence:

- Identity first: nothing works until admins can create members and members can log in.
- Feed before video: video depends on the media pipeline and the posts/feed model.
- Messaging after feed: isolated transport decision, independent of feed.
- Hardening last: moderation, backups, and upgrades once behavior is known.

## 3. Phases

### Phase 0 — Decisions that unblock everything *(proposal)*

- [ ] Identity model: built-in accounts (WebAuthn/passkeys, already in the stack) vs.
      LDAP/OIDC/SAML SSO vs. both. *(product.md open question 1)*
- [ ] Video pipeline: self-hosted ffmpeg workers vs. external transcoder; storage:
      local disk vs. S3-compatible. *(product.md open question 2)*
- [ ] Messaging transport: WebSocket/STOMP vs. WebFlux/SSE; offline tolerance. *(product.md open question 3)*
- [ ] Deployment artifact: Docker image + Compose vs. Helm chart; upgrade path story.
- [ ] JDK: pin development to JDK 21 (local machine currently reports 26 — `just doctor`
      warns).
- [x] Test strategy: integration tests run against the dev MySQL's `chafgram` schema
      (Flyway migrates it like any schema; no Testcontainers) — done. Note: `./mvnw test`
      wipes the schema; `just seed` restores the dev fixtures.

**Exit:** written decisions in `product.md`/`AGENTS.md`; test suite green in CI locally
without a dev database.

### Phase 1 — Workspace & identity

- [x] JPA entities + repositories for `users`, `workspace_settings`, `invites`
      (done). `account_tokens` ships in schema only — email flows deferred until the
      instance has SMTP; invite links carry the accept token instead. Passkeys live in
      Spring Security's `user_entities`/`user_credentials` (framework-managed).
- [x] Sign-up/invite flow — done backend-side: `POST /api/v1/bootstrap` (first admin),
      `POST /api/v1/register` (only when `signup_mode=open`), `POST /api/v1/invites`
      + `/accept` (admin or reusable link, SHA-256-hashed, 7-day TTL, single-use),
      passkey registration/authentication via Spring Security WebAuthn
      (`/webauthn/register/*`, `/login/webauthn`), optional password fallback.
- [x] Admin console (API) — member list/get, role change (member/moderator/admin),
      suspend/deactivate/reactivate, with self-guard and last-active-admin guard;
      workspace name/slug/primary color/signup mode via `GET/PATCH /api/v1/workspace`
      (public read, admin write). *(Frontend admin UI not done.)*
- [ ] Workspace branding applied to frontend shell (workspace name/logo in header) —
      not done (frontend).
- [ ] Docker/deploy story: production image build, `compose.yaml` for all-in-one
      self-hosted run (dev compose exists today) — not done.
- [ ] Frontend: replace starter branding (TanStack) with Chafgram identity —
      not done.
- [x] Test strategy: integration tests run against the dev MySQL's `chafgram` schema
      (no Testcontainers — Flyway migrates it like any schema; the suite wipes tables,
      `just seed` restores the dev fixtures) — done.

**Exit:** backend prerequisites all exist — an admin can bootstrap, invite members, and
members authenticate (form login or passkey) against a real MySQL. End-to-end sign-in
still needs the frontend passkey UI (registration page + login button) and the production
deploy story before this exits.

### Phase 2 — Core social (Facebook/Instagram behavior)

- [ ] Posts (`note`/`photo`/`video`), comments (threaded), reactions
      (post/comment), saves, follows, blocks.
- [ ] Home feed: following-feed query (indexed `posts(author_id, created_at)` +
      `follows(followee_id, follower_id)`), with cursor pagination.
- [ ] Notifications: emit on follow/like/comment/reply/mention; unread badge + list;
      mark-read.
- [ ] Full-text search on `posts.body` (FULLTEXT index already in V1).
- [ ] Feed UI in the sea/lagoon system: composer, post card, comment thread, profile
      pages (header/avatar/bio/follow button), empty states.

**Exit:** two members can post, follow, react, comment, and receive notifications.

### Phase 3 — Media & short video

- [ ] Upload endpoints with chunked/large-file support; `media_assets` rows
      `status=processing`; ffmpeg worker (sidecar or queue-consumer) sets `ready`.
- [ ] Video posts + watch view (dark-first theater per design.md §13); short-video
      vertical feed (≤ 60 s cut-off on `duration_ms`).
- [ ] Avatars/workspace logo via same pipeline (`users.avatar_media_id`).
- [ ] Image posts via same media table; blurhash placeholders.
- [ ] Storage abstraction: local disk and S3-compatible both behind one interface.

**Exit:** a member can upload a short video, another watches it; profile pictures work.

### Phase 4 — Messaging

- [ ] `conversations`/`conversation_members`/`messages` wiring; 1:1 and group
      conversations; cursor pagination by `(conversation_id, id)`.
- [ ] Real-time transport per Phase 0 decision; unread badges via
      `last_read_message_id`; muted conversations; member list management for groups
      (add/remove, admin role).
- [ ] Media in messages via `message_media`.
- [ ] DM UI: conversation list, thread, composer, inline media.

**Exit:** two members exchange messages in real time with unread counts.

### Phase 5 — Moderation & hardening

- [ ] Reports (post/comment/message/user), moderator queue with
      `open → reviewing → actioned|dismissed`, actions write `audit_log` rows.
- [ ] Suspension/deactivation enforcement everywhere (feed, comments, messages, login).
- [ ] Admin audit log view; content removal with tombstones (`deleted_at`).
- [ ] Backup/restore and upgrade docs; Flyway migration discipline for releases;
      admin-facing data export (member GDPR-style export). *(proposal)*
- [ ] Accessibility pass, reduced-motion audit, keyboard-nav audit (design.md §10).
- [ ] Ops: Actuator health/metrics exposed, backup guidance, log hygiene, no secrets in
      logs, security headers.

**Exit:** a moderator can resolve a report end-to-end; a company can back up, restore,
and upgrade Chafgram with documented steps.

## 4. Cross-cutting workstreams

- **Testing** — DB-backed tests run against the dev MySQL's `chafgram` schema
  (Flyway migrates it like any schema; no Testcontainers; the suite wipes tables;
  `just seed` restores fixtures); component tests for the
  frontend (none exist yet); `just verify` runs backend tests + builds, and requires `just mysql` for the backend leg.
- **Schema evolution** — additive migrations only; `db-schema.md` updated in the same
  change; never edit applied migrations.
- **Design system** — components land in the sea/lagoon token system (design.md); no raw
  hex in components; dark mode is a first-class citizen from the first screen.
- **Docs** — keep `product.md` (scope), `design.md` (visuals), `db-schema.md` (data),
  this file (sequence) in sync with reality; stale docs are bugs.
- **Branding** — replace starter identity (TanStack links, "Your name here" footer) with
  Chafgram's mark/wordmark; favicon and app icons.

## 5. Definition of done (every phase)

1. `just verify` green (backend + frontend tests, backend + frontend build).
2. New UI verified in light/dark/auto, keyboard-navigable, token-based.
3. Schema changes shipped as new Flyway migrations with `docs/db-schema.md` updated.
4. Product/design docs updated if scope or decisions shifted.
5. No secrets, no `.agents/` content, no build output in the diff.

## 6. Explicit non-goals (from product.md)

- Public/consumer social networking; open sign-up; cross-company federation.
- Slack/Teams replacement for work coordination (huddles, task threads, CRM/ticketing
  integrations).
- Multi-tenant hosting operated by the Chafgram authors; managed cloud offering.
- Ads, monetization, creator payouts.

## 7. Open questions that gate phases

| Phase | Question |
|-------|----------|
| 0 | SSO/LDAP/OIDC vs. built-in WebAuthn-only auth? |
| 0 | Storage backend for media (local vs S3-compatible), and ffmpeg worker model? |
| 0 | Messaging transport (WebSocket/STOMP vs WebFlux/SSE)? |
| 2 | Feed ranking beyond recency? Chronological-following only for v1? |
| 2 | Mentions: parse `@username` at write time, or store structured mentions? |
| 4 | Group chats: simple groups, or Slack-like channels? Affects `conversations` model. |
| 5 | Retention: configurable policies, legal hold, e-discovery export? |
| 5 | Release cadence, supported upgrade window, security disclosure process? |
| Any | Should purged content be immediately unrecoverable, or is there a 30-day admin trash? |

## 8. Milestones *(proposal)*

- **M1 — "Company-ready preview"**: end of Phase 1. Self-host, invite, sign in.
- **M2 — "Usable internally"**: end of Phase 2. Posting, feed, notifications.
- **M3 — "Full social"**: end of Phases 3–4. Video + messaging.
- **M4 — "Production hardened"**: end of Phase 5. First stable tagged release (1.0.0).

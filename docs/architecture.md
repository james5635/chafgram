# Chafgram — Architecture

> How Chafgram is put together: system topology, module boundaries, API surface, media and
> messaging pipelines, deployment, and operations. This is the architectural map of the
> codebase — not a product doc ([`product.md`](../product.md)), not the visual system
> ([`design.md`](../design.md)).
> Proposals are marked **(proposal)**; decided items are stated as fact.

## 1. Architectural stance

- **Modular monolith, single deployable.** One Spring Boot application, one MySQL
  database, one frontend bundle. Not microservices — the operator is a company admin
  team, not a platform crew. *(proposal — consistent with product.md delivery model)*
- **Boring, proven technology.** Spring Boot + Spring Security + JPA/Hibernate + MySQL,
  TanStack Start + React. Nothing exotic in the request path.
- **Private by default.** Single tenant per deployment (one company, one workspace).
  No phone-home, no mandatory external services; the instance runs fully inside the
  company's control. *(decided — product.md)*
- **API-first, not API-everything.** GraphQL for application data; REST for
  operations and auth endpoints (token issuance/refresh, upload/download, Actuator).
- **Replaceable boundaries.** Storage and messaging transport sit behind interfaces so
  they can move without touching domain code.

## 2. System topology

```
┌────────────────────────── company infrastructure ──────────────────────────┐
│                                                                           │
│   browser / PWA ── HTTPS ──▶ ┌────────────────────────┐                  │
│                              │ frontend (TanStack     │                  │
│                              │  Start, SSR, :3000)    │                  │
│                              └───────────┬────────────┘                  │
│                                          │ server functions / BFF         │
│                              ┌───────────▼────────────┐                  │
│                              │ backend (Spring Boot,  │                  │
│                              │  :8080)                 │                  │
│                              │  GraphQL + REST         │                  │
│                              │  Spring Security +      │                  │
│                              │  JWT bearer auth        │                  │
│                              └──────┬───────────┬─────┘                  │
│                                     │ JDBC      │ media I/O              │
│                          ┌──────────▼───┐   ┌───▼──────────────────┐    │
│                          │ MySQL 8.0    │   │ media store          │    │
│                          │ (:3306)      │   │ local disk or        │    │
│                          └──────────────┘   │ S3-compatible (prop.) │    │
│                                           └───▲──────────────────┘    │
│                                               │ transcode/thumbs        │
│                                       ┌───────┴────────────────┐       │
│                                       │ in-app async pipeline   │       │
│                                       │ (ffmpeg, bounded pool)  │       │
│                                       └────────────────────────┘       │
└───────────────────────────────────────────────────────────────────────┘
```

- The **frontend** is the public face: server-rendered, proxies everything to the
  backend (BFF pattern) so no internal URLs or secrets leak to the browser.
- The **backend** owns all domain logic, auth, and data. The frontend holds no
  authoritative state.
- The **media pipeline runs in-app**: a bounded executor inside the backend
  picks up `media_assets` rows with `status='processing'` after the upload transaction
  commits. A separate worker process/sidecar remains possible later, but it is not
  required for v1.
- **Reverse proxy / TLS** (nginx, Traefik, company LB) terminates HTTPS; not part of
  this repo. Configuration documented here later.

## 3. Frontend architecture

- **TanStack Start + TanStack Router (React 19, TypeScript 6, Vite 8)** — SSR with
  streaming; file-based routing under `src/routes/`; generated `routeTree.gen.ts`
  (never hand-edited). Dev-only TanStack Devtools/Router Devtools render in the corner.
- **Media bytes** — GraphQL `Post.mediaUrls` supplies short-lived signed BFF URLs
  (`/api/media/{id}?exp=…&uid=…&sig=…`, 1-hour TTL). The browser loads them through the
  `/api/media/$id` route, which proxies `GET /api/v1/media/{id}/bytes` and relays the
  signature query without a bearer. A media `onError` asks the BFF for one fresh signed
  URL (`GET /api/v1/media/{id}/signed-url`, bearer-protected) and swaps `src` once.
  The backend URL is never exposed to the browser.
- **Server functions** (`createServerFn`) act as the BFF for all backend calls:
  typed, server-only credential handling, single egress point to the Spring Boot API.
  `src/lib/backend-proxy.ts` is the only module that talks to the backend; it forwards
  the browser's bearer token, performs one refresh-and-retry on 401 using the browser's
  HttpOnly refresh cookie, and relays the backend's rotated `Set-Cookie`. No session or
  CSRF cookies are involved.
- **Apollo Client 4** — the browser-side GraphQL client. Its `bffLink`
  (`src/lib/apollo.ts`) sends every operation through the `graphqlRequest` server
  function, so the browser never learns the backend URL. One `ApolloClient` with
  an `InMemoryCache` is provided by `ApolloProvider` in `src/routes/__root.tsx`.
  Reads use `useQuery`, writes use `useMutation`; cache updates target normalized
  entities so all cached queries refresh together.
- **GraphQL codegen** — `just generate` (root) runs `frontend::generate`, which
  reads the backend's committed `*.graphqls` files and regenerates
  `frontend/src/generated/graphql.ts` (git-ignored, never hand-edited).
  Operations live in `frontend/src/graphql/*.graphql`; `pnpm run pretest`/`prebuild`
  regenerate the types so a fresh clone needs no extra step.
- **Rendering strategy** — SSR for shell and public surface; client interactivity for
  feed/compose/watch/messages; route loaders for data.
- **Styling** — Tailwind v4 (CSS-first via `@tailwindcss/vite`) + token system in
  `styles.css` (`--sea-ink`, `--lagoon`, `--avatar-tint-*`, …); theming
  (light/dark/auto) resolved pre-paint by the inline `THEME_INIT_SCRIPT`; see
  [`design.md`](../design.md). `tw-animate-css` supplies Tailwind animations.
- **UI primitives** — shadcn-style components in `src/components/ui/` built on
  Base UI (`@base-ui/react`), with `class-variance-authority` for variants,
  `cn` for class merging, and `lucide-react` for stroke icons. Geist Variable is
  installed via `@fontsource-variable/geist` for the primitives; the UI font
  remains Manrope per `design.md`.
- **Structure** — `src/components/` (shell: Header, LeftRail, RightRail,
  ThemeToggle; `ui/` primitives), `src/routes/` (file routes), feature modules
  land as sibling folders later (`feed/`, `watch/`, `messages/`, `admin/`) rather
  than route-local code.
- **Testing** — frontend: Vitest 5 + React Testing Library (`jsdom`), colocated
  `*.test.ts(x)`; backend: JUnit + MockMvc against Testcontainers MySQL (hermetic).
  `just verify` runs both suites and both builds.
- **Data fetching** — Apollo `useQuery`/`useMutation` for reads and writes; TanStack
  Router loaders remain an option for route-level data.
- **Auth flow** — `loginRequest` calls `POST /api/v1/auth/login`; the access JWT is
  returned to browser memory (never `localStorage`) and the HttpOnly
  `chafgram_refresh` cookie is relayed by the BFF. `createAuthFetch` attaches the
  bearer to every server-function call. On a backend 401, `callBackend` calls
  `/api/v1/auth/refresh` once with the browser's refresh cookie, relays the rotated
  cookie, swaps the in-memory token, and retries the original request once; a second
  401 clears the token and signals re-login. Reloads trigger a silent refresh during
  bootstrap; SSR renders only the public shell, and authenticated data loads after
  hydration. `useSessionGuard` (`src/hooks/`) redirects to `/login` on that signal.
- **No CSRF** — the main chain is stateless and CSRF-disabled; no double-submit token
  exists. Refresh is authenticated by the HttpOnly cookie alone; logout also requires
  a bearer token. If OIDC SSO lands, the same BFF shape holds.


## 4. Backend architecture

Package root `io.github.james5635.chafgram`. Each module is physically layered:

```
<module>/controller    GraphQL resolvers and REST controllers (the API layer)
<module>/service       transactional services
<module>/repository    Spring Data repositories
<module>/utils         helpers, converters, stateless utilities
<module>/dto           wire types (where present)
<module>/              JPA entities, enums and value types
```

Dependencies point inward: `controller` → `service` → `repository`; `utils` is
called by all layers but depends on none of them. `config` keeps Spring
configuration. *(`identity`, `common`, all implemented)*

The Gradle workspace has a second subproject, `backend/seed`, for dev fixtures.
It boots a headless Spring context (controllers and web security excluded),
depends on `:api`, and creates fixtures by calling the same services as the app
with an explicit acting member. `just seed` runs it against an empty, migrated dev DB;
`just migrate` uses `:api:flywayMigrate`, and `just db-reset` uses
`:api:flywayClean` to drop all schema objects.
No seed SQL script is shipped.

```
io.github.james5635.chafgram
├── config           Security, CORS, GraphQL scalars, JPA auditing, Actuator
├── identity         users, invites, auth token pairs (access JWT + opaque refresh
│                    tokens), workspace_settings, roles & member lifecycle  ← implemented
├── social           posts, comments, reactions, follows, saved_posts, feed,
│                    user_blocks, search (FULLTEXT)  ← implemented
├── media            media_assets lifecycle, upload endpoints, transcode orchestration,
│                    storage abstraction (local disk | S3-compatible)  ← implemented
├── messaging        conversations, conversation_members, messages, realtime transport  ← implemented
├── notifications    emit/consume, unread badges, (no push in v1)  ← implemented
├── moderation       reports workflow, audit_log, content tombstones  ← implemented
├── mail             account mail only: logging fallback (default) or SMTP  ← implemented
└── common           errors (RFC-7807), paging cursors, mappers, constants  ← implemented
```

### 4.1 API surface

| Surface | Used for | Notes |
|---------|----------|-------|
| **GraphQL** (Spring GraphQL) | **Every API operation** — social data, identity, invites, workspace, messaging, moderation, media metadata | Single endpoint `POST /graphql`. Class-level `@PreAuthorize("isFullyAuthenticated()")` on every resolver class; `permitAll()` only on the public `workspace` branding query and the account-flow mutations (`requestPasswordReset`, `resetPassword`, `verifyEmail`). Authorization failures return HTTP 200 with `errors[]` and `extensions.classification` (`UNAUTHORIZED` for anonymous, `FORBIDDEN` for an authenticated member without the role, `BAD_REQUEST`/`NOT_FOUND` for validation and lookup failures); assert on `$.errors`, never the HTTP status. |
| **REST** | Auth endpoints, media upload/bytes/signed URLs, Actuator | The keep-list below — mandated by the fact that a GraphQL operation cannot set the refresh cookie a login needs, cannot receive multipart bodies, and cannot stream bytes. |
| **Actuator** | `/actuator/health` for probes; `/actuator/info` + `/actuator/metrics` for ops | Own security chain (`SecurityConfiguration.actuatorFilterChain`): health is public, info and metrics are admin-only. HTTP Basic is the only entry point, so anonymous requests get `401` + `WWW-Authenticate` instead of the member-facing login redirect. |
| **WebSocket/SSE** *(proposal)* | Messaging delivery (see §6) | Transport decision pending |

**REST keep-list (all other APIs are GraphQL):**

- `POST /api/v1/auth/login` — username/email + password → access JWT + refresh cookie
- `POST /api/v1/auth/refresh` — refresh cookie → rotated refresh cookie + new access JWT
- `POST /api/v1/auth/logout` — bearer + refresh cookie → revoke row + clear cookie
- `POST /api/v1/bootstrap` — first-run setup; creates the first admin and issues a token pair
- `POST /api/v1/register` — self-serve signup (only when `signup_mode=open`); issues a token pair
- `POST /api/v1/invites/accept` — invite claim; creates account + token pair
- `POST /api/v1/media` — multipart media upload (image/video)
- `GET /api/v1/media/{id}/signed-url` — bearer-protected; issues a 1-hour signed bytes URL
- `POST /api/v1/messages` — multipart message send (image attachments inline)
- `GET /api/v1/media/{id}/bytes` — authorized byte streaming; also accepts signed `exp`/`uid`/`sig` query params without a bearer
- `GET /actuator/health` — public liveness/readiness probe (status only, no details)
- `GET /actuator/info`, `/actuator/metrics` — admin-only ops endpoints (HTTP Basic)
- `/error` — servlet error dispatch

Public workspace branding is a **GraphQL** operation, not REST: `query { workspace { name slug } }`
(anonymous, `@PreAuthorize("permitAll()")`). `/graphql` and `/graphiql` are public at the
HTTP boundary: resolver authorization failures stay HTTP 200 + `errors[]`, while
missing/invalid/expired access tokens are rejected by the resource-server filter with 401.

### 4.2 Cross-cutting

- **Security** — Spring Security, roles `member|moderator|admin`; password login issuing
  a 15-minute HS256 access JWT plus a 14-day opaque refresh token; stateless bearer
  authentication (no sessions, no CSRF); per-route authorization (admin console vs.
  member surface). Any presented revoked refresh row is rejected with 401; only rows
  revoked without `replaced_by` revoke the whole user's token family.
- **Persistence** — JPA/Hibernate, `@Enumerated(STRING)`, UTC `DATETIME(6)`, Flyway
  owns all schema changes; soft-delete via `deleted_at` + query-level filtering;
  audit fields via JPA auditing.
- **Error model** — RFC-7807 problem responses from REST; GraphQL error extensions
  mirror the same codes (`code`, `detail`, `status`).
- **Transactions** — service layer owns `@Transactional`; no lazy loading across
  GraphQL boundary (fetch joins / projections).
- **Social graph filtering** — blocking is mutual-effect (BR-2.2): every feed
  (discover, following, watch) drops an author when *either* side blocked the other.
  A one-directional filter is not enough — the blocker must disappear from the
  blockee's feeds too.
- **Watch-feed pagination** — the watch feed fetches past the rows the
  mutual-block filter hides: a blocked video must not leave the page short, and
  the next cursor must not stop on a hidden row — the older allowed videos
  behind it would become unreachable.
- **Presence** — last-seen (FR-2.5) is refreshed on every authenticated read
  (`CurrentMember.require()`), not only on login. The write is throttled to one
  update per member per five minutes and never breaks the request that triggered
  it — the caller guards the whole transactional call, so a write that fails at
  commit is swallowed too. `Member.lastSeenAt` exposes the timestamp to clients.
- **Logging** — structured logs to stdout; never log credential material; correlation
  ids propagated frontend→backend. The dev mail fallback is no exception: the logged
  body keeps everything except the account-flow token (`?token=…` is redacted), so a
  reset/verification link in the log cannot be redeemed.

## 5. Media pipeline

```
upload (REST multipart)            in-app async pipeline
  │  media_assets row: status=processing, kind=image|video
  ▼
 local disk / S3-compatible  ◀── ffmpeg/ffprobe: transcode, thumbnail, blurhash
  │                                        │
  └── feed/watch query: WHERE status='ready'
```

- The pipeline is an in-app task inside the backend (`AsyncConfig`: 2 threads, queue
  64, `media-pipeline-*` threads), started after the upload transaction commits. It runs
  `ffprobe` (duration, dimensions) → `ffmpeg` (H.264/AAC transcode, metadata stripped)
  → thumbnail → blurhash, then flips the row to `status='ready'`. Any error or a
  10-minute timeout lands it in `status='failed'`.
- The work is handed to the bounded executor explicitly (not via an `@Async` proxy), so
  a full queue is visible: a rejected asset is marked `status='failed'` instead of
  staying stuck in `status='processing'`.
- The external tools run with their output drained while the process runs. ffmpeg writes
  progress to stdout/stderr; a pipe nobody reads fills up and blocks the tool, which
  would look like a transcode timeout. The process is killed when it overruns its
  timeout, and the captured tail is kept for the error message.
- The transcode always produces MP4, so the pipeline moves the asset to a `.mp4`
  storage key and updates `mime_type` to `video/mp4` — the stored bytes and the
  served content type always agree. The pre-transcode source blob is dropped.
- `markReady`/`markFailed` only transition a still-`processing` asset
  (`WHERE status = 'processing'`): an admin moderation delete while a transcode
  runs must survive — the byte authorizer would otherwise serve the video again.
  When the guard matches nothing the pipeline discards the result.
- Short vs. long video: same `media_assets` table; short feed =
  `kind='video' AND duration_ms <= 60000 AND status='ready'` — a query, not a table.
- Storage behind `MediaStorage` interface (`store`, `storeProcessed`, `delete`, `read`);
  S3-compatible backend is the default (Garage in dev, any S3 provider in prod),
  with local filesystem storage available via `chafgram.media.backend=local`.
  `resolvePath` stages S3 objects into a temp file for image probing/ffmpeg.
- Bytes are never served anonymously: `GET /api/v1/media/{id}/bytes` runs the
  authorization tree in `MediaAccessService` (owner, message participants, visible
  posts, avatars/logos; staff see everything) and answers `404` for every denial.
  Suspended members keep read access (FR-1.4) — only writes are blocked for them.
  Message attachments require **active** participation: a member who left or was
  removed from a conversation keeps the membership row for history, but `left_at` is
  set and the row no longer grants access to the conversation's attachments.
- Signed media URLs are HMAC-SHA256 over asset id, expiry and viewer id using the JWT
  secret, with a 1-hour TTL (`CHAFGRAM_MEDIA_URL_TTL`) and `Cache-Control: private,
  max-age=300` on bytes. A URL signed for one viewer fails for another; tampered and
  expired URLs are rejected. Re-signing never rotates the refresh token.
- Image uploads are ready immediately and carry the required metadata: the
  upload reads the stored file back with ImageIO and records `width`/`height`
  and the blurhash (FR-5.2). Formats ImageIO cannot read (e.g. WebP) leave the
  fields null. Both image paths share one helper — `POST /api/v1/media` and the
  inline `POST /api/v1/messages` upload expose the same metadata.
- Video thumbnails are kept, not discarded: the pipeline stores the generated
  JPG next to the transcoded video (`media_assets.thumbnail_key`) and the
  bytes endpoint serves it with `GET /api/v1/media/{id}/bytes?variant=thumbnail`
  (`image/jpeg`). The same authorization decision as the asset itself applies;
  `MediaAsset.hasThumbnail` tells clients that one exists.
- Message attachments: `POST /api/v1/messages` (multipart) takes **image** files
  inline. Videos cannot be ready in the same request (FR-5.2), so the flow is
  upload → poll `mediaAsset(id)` until `status='ready'` → send the message with
  `mediaIds`. The multipart endpoint rejects inline video files with a 400 that
  points at that flow.
- A purged sender leaves `messages.sender_id` NULL; the attachment authorizer
  compares it null-safely, so the message history (and its attachments) stay
  readable instead of failing with a 500.
- Moderation delete (admin, BR-5.3): `deleteMedia(id)` tombstones the asset
  (`status='deleted'`), writes an `audit_log` row, and the bytes stop being served
  everywhere. The file itself stays on disk until a retention sweep exists.
- Failures land in `status='failed'` with last error in `blurhash`-adjacent column or
  a proper `error_message` if added — **(proposal)** if retried 3×, require manual
  re-upload rather than infinite retry.

## 6. Messaging & realtime

- Transport **(proposal, Phase 0 decision)**: WebSocket + STOMP on the servlet stack,
  or WebFlux/SSE. MVC-first default keeps one web stack; reactive is only reached for
  if backpressure demands it.
- Model: `conversations` (direct|group) → `conversation_members` (incl.
  `last_read_message_id` for unread badges) → `messages` + `message_media`.
- Ordering: messages are totally ordered by `id` per conversation; cursor pagination
  by `(conversation_id, id)`; unread count = messages with `id >` member's
  `last_read_message_id`. The per-conversation badge is exposed as
  `query { unreadMessageCount(conversationId: ID!): Int! }`.
- Membership is soft: leaving a conversation keeps the row (history), but `left_at`
  is set — the former member loses access to the conversation, its messages and its
  attachments.
- No server-side read receipts (per-message "seen") in v1 — privacy and simplicity.

## 7. Notifications

- In-app only for v1 (no Web Push, no email digests).
- Polymorphic read model (`notifications.entity_type/entity_id`) — written by domain
  services (feed interactions, follows, mentions, messages), never joined for integrity;
  recipients query index `(recipient_id, read_at, created_at)`.
- Fan-out is write-time (row per recipient) — correct at company scale; no queue needed
  until proven otherwise.
- Visibility is resolved in bulk: a page of notifications is checked with one query per
  entity type (posts, comments, messages), not one query per row — the unread set is
  unbounded, so a per-row check would be an N+1.
- The notification list has no cursor, so a page must be **filled**: when the newest rows
  point at tombstoned content, the service keeps scanning backwards until the requested
  number of visible notifications is collected. Otherwise the older visible ones would be
  unreachable.

## 8. Moderation & audit

- Content removal writes tombstones (`deleted_at`) on posts/comments/messages; all
  read paths filter them.
- `reports` workflow: `open → reviewing → actioned|dismissed`; actions enqueue
  `audit_log` entries (append-only, actor nullable for purged admins).
- Enforcement points: feed assembly, comment/profile/message rendering, login —
  all check `users.status` and content tombstones.

## 9. Deployment & operations

| Concern | Approach |
|---------|----------|
| Artifact | OCI image for backend + static assets via frontend build; `compose.yaml` for the full self-hosted stack (app + MySQL + volume) — dev compose exists today and doubles as the reference |
| Migrations | Flyway via `:api:flywayMigrate` (`just migrate`), not at app boot; additive only; `V1__init.sql` baseline, `V2` media column alignment, `V3` `users.email_verified_at`, `V4` `media_assets.thumbnail_key`, `V5` `refresh_tokens`, `V6` drop WebAuthn tables (migrations in `backend/api/src/main/resources/db/migration/`) |
| Backups | `mysqldump`/volume snapshot, admin-facing docs; restore tested per release |
| TLS | Terminated at company reverse proxy; app serves plain HTTP internally |
| Observability | Actuator health/metrics; logs to stdout; **no** mandatory external telemetry |
| Upgrade | Pull tag → restart; migrations run automatically; single node, brief downtime acceptable |
| Scale target | Hundreds–thousands of members, one MySQL instance, single node; read replicas/HA explicitly out of scope for v1 |

## 10. Security model

- Threat model: internal-network primary; internet exposure only behind company proxy.
- AuthN: password login only; short-lived bearer JWTs are validated on every request,
  opaque refresh tokens are stored as SHA-256 hashes and rotated on use. Refresh cookies
  are HttpOnly, `SameSite=Strict`, path `/`, `Secure` outside dev/test. Production must
  override the throwaway `CHAFGRAM_JWT_SECRET` (≥32 bytes).
- AuthZ: role-based, least privilege; admin routes enforce `ROLE_ADMIN`; moderator
  routes for `ROLE_MODERATOR|ROLE_ADMIN`.
- Secrets: environment variables / git-ignored profiles; never in repo, never in logs.
- Data residency: every byte (MySQL + media store) lives in the company; deletion is
  hard-delete at the owner's request for member PII; member content purge cascades.

## 11. What this rules out (v1)

Multi-tenant; microservices; Kafka/queue-first architecture; mandatory Kubernetes;
`node_modules` on servers (build in CI); external SaaS dependencies in the request
path; read replicas and sharding; mobile native apps (responsive web/PWA only).

## 12. Open decisions (block specific modules)

| Decision | Blocks | Source |
|----------|--------|--------|
| Built-in password accounts vs. LDAP/OIDC/SAML SSO vs. both | identity module | product.md §Open |
| WebSocket/STOMP vs. WebFlux/SSE for messaging | messaging module | product.md §Open |
| ~~Local disk vs. S3-compatible media storage~~ — resolved: S3-compatible default, local fallback | media module | docs/architecture.md §4 |
| Group chat semantics (simple groups vs. channels) | conversation model | business-rules.md §12 |
| FULLTEXT scope: `posts.body` only vs. also comments | search | functional-requirements.md FR-3.6 / FR-8.1 |
| Retention/legal-hold policies | moderation | product.md §Open |

## 13. Relationship to other docs

- [`product.md`](../product.md) — what/who/why; [`design.md`](../design.md) — how it
  looks; [`functional-requirements.md`](functional-requirements.md) — what must be
  built (FR); [`business-rules.md`](business-rules.md) — the rules it must obey (BR).

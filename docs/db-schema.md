# Chafgram — Database Schema

> Relational schema for the Chafgram platform: a self-hosted, single-tenant private
> social platform for companies (inspired by Facebook/Instagram). Engine: **MySQL 8.0**,
> applied via **Flyway** (`backend/src/main/resources/db/migration/`). ORM: Spring Data
> JPA. Migration baseline: `V1__init.sql`.
>
> Scope: core identity & auth, social graph, media (posts/short videos), messaging,
> notifications, moderation/audit, workspace settings — the full product, not an MVP.

## 1. Design decisions

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | **Single-tenant** — one deployment = one company; no `tenant_id` columns | Matches product.md delivery model (on-prem, self-hosted). Keeps every query and index simple. |
| D2 | **`BIGINT AUTO_INCREMENT` primary keys** | Dense, JPA/`GenerationType.IDENTITY` friendly, compact FKs and indexes. (Alternative considered: UUID/ULID — rejected: unnecessary opacity inside a company instance; wider keys, random insert order.) |
| D3 | **`utf8mb4` / `utf8mb4_0900_ai_ci`** charset+collation, InnoDB everywhere | Emoji in posts/comments/names; case/accent-insensitive matching for emails/usernames without app-side normalization. |
| D4 | **Physical per-type reaction tables** (`post_reactions`, `comment_reactions`) | Real FKs and unique constraints; no polymorphic `target_id` where integrity is cheap to enforce. |
| D5 | **Pragmatic polymorphism for notifications, reports, audit log** (`entity_id`/`target_id` + `type`, no FK) | A notification must survive/attach to rows of several types, and reports/audit must outlive content deletion. Accept and document, never join blindly. |
| D6 | **Soft delete for user content** (`deleted_at`) on posts/comments/messages; hard delete cascades from `users` | Moderation needs tombstones ("removed by admin"); GDPR purge of a member cascades cleanly via FK `ON DELETE CASCADE`. |
| D7 | **No denormalized counters** (`likes_count`, etc.) | Avoid drift; compute counts with indexed `COUNT(*)` until profiling proves otherwise. Revisit with a counter cache if needed. |
| D8 | **Media pipeline via `media_assets`** with `status` (`processing → ready/failed`) | One pipeline for images, videos, short videos; `post_media` attaches ready media to posts. Transcoding (ffmpeg workers) reads the table for `status='processing'` rows. |
| D9 | **Single-row instance config** — one row in `workspace_settings` | Self-hosted instance needs name/branding/signup policy without a tenant system; `CHECK (id = 1)` enforces singleton. |
| D10 | **Timestamps are UTC** (`DATETIME(6)`), app connects with `serverTimezone=UTC` | Avoid TIMESTAMP 2038 ceiling and TZ ambiguity; microsecond precision. |

## 2. Conventions

- Tables: `snake_case` plural nouns. Constraints: `pk_…`, `fk_…`, `uk_…` (unique), `idx_…` (index).
- Every table has `created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`;
  mutable tables add `updated_at … ON UPDATE CURRENT_TIMESTAMP(6)`.
- Join-table PKs are composite natural keys (`follows`, `saved_posts`, …).
- All `id`s are `BIGINT AUTO_INCREMENT`.
- FK delete rules:
  - `ON DELETE CASCADE` for ownership trees (post → media join rows, message → media,
    memberships, reactions, saves) — deleting a user purges their contribution footprint.
  - `ON DELETE RESTRICT` / `SET NULL` where history must survive: `audit_log.actor_id`
    and `reports.resolved_by` are `SET NULL` (actors may be purged); `notifications.actor_id`
    is `SET NULL`.
  - `reports`/`audit_log` never FK to their targets (see D5).

## 3. Entity map (high level)

```
users ─┬─ follows (self M:N)
       ├─ blocks  (self M:N, directional)
       ├─ posts ──< post_media >── media_assets
       │    └─< comments (self-parent) ──< comment_reactions
       │    └─< post_reactions
       │    └─< saved_posts
       ├─ conversations ──< conversation_members >── users
       │    └─< messages ──< message_media >── media_assets
       ├─ notifications (polymorphic: post|comment|message|user)
       ├─ notification_preferences (per-type mutes, FR-7.3)
       ├─ user_entities / user_credentials (Spring Security WebAuthn-managed; `user_entities.name` = users.username)
       ├─ account_tokens / invites
       │
reports ───────────── audit_log     (polymorphic: post|comment|message|user)
workspace_settings (singleton) ── logo_media → media_assets
```

## 4. Tables

### 4.1 Core identity & auth

**users**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AI | |
| username | VARCHAR(30) NOT NULL, UNIQUE | lowercase handle; case-insensitive collation |
| display_name | VARCHAR(80) NOT NULL | |
| email | VARCHAR(254) NOT NULL, UNIQUE | stored lowercased |
| password_hash | VARCHAR(255) NULL | bcrypt/argon2; NULL = passkey-only account |
| bio | VARCHAR(160) NULL | Instagram-style |
| avatar_media_id | BIGINT NULL → `media_assets` | FK added by `ALTER` after media table exists (see §6) |
| role | ENUM('member','moderator','admin') NOT NULL DEFAULT 'member' | single workspace role |
| status | ENUM('active','suspended','deactivated') NOT NULL DEFAULT 'active' | soft deactivation |
| last_seen_at | DATETIME(6) NULL | updated by app (throttled) |
| created_at / updated_at | DATETIME(6) | |

**user_blocks** — `blocker_id`, `blocked_id` (PK pair), `created_at`; FKs → users CASCADE. Blocks hide content/messages both ways at the app layer.

**user_entities / user_credentials** (passkeys; managed by the Spring Security WebAuthn
framework via its own JDBC repositories — exact framework schema, not our design)

Spring Security expects:

```sql
CREATE TABLE user_entities (
  id           VARCHAR(512) PRIMARY KEY,   -- base64url of the 32-byte user handle
  name         VARCHAR(100)  NOT NULL,      -- = users.username
  display_name VARCHAR(200)
);
CREATE TABLE user_credentials (
  credential_id                VARCHAR(512) PRIMARY KEY,
  user_entity_user_id          VARCHAR(512) NOT NULL REFERENCES user_entities (id) ON DELETE CASCADE,
  public_key                   BLOB          NOT NULL,
  signature_count              BIGINT,
  uv_initialized               BOOLEAN,
  backup_eligible              BOOLEAN       NOT NULL,
  authenticator_transports     VARCHAR(512),
  public_key_credential_type   VARCHAR(100),
  backup_state                 BOOLEAN       NOT NULL,
  attestation_object           BLOB,
  attestation_client_data_json BLOB,
  created                      TIMESTAMP,
  last_used                    TIMESTAMP,
  label                        VARCHAR(512) NOT NULL
);
```

These tables are **not** JPA-mapped — the framework drives them through its own
`Jdbc*Repository` beans (`config/SecurityConfiguration`). No user handle ↔ member id
join is stored; the app joins on `user_entities.name = users.username` when needed.
`user_entities.name` is UNIQUE per the DDL.

*Column width note:* the framework's shipped DDL uses `VARCHAR(1000)`; we use
`VARCHAR(512)` (identical semantics for `base64url(32B)` handles) because InnoDB's
3072-byte key limit can't index `utf8mb4 VARCHAR(1000)` (4000 bytes). WebAuthn
`rawId` values are ≤ 255 bytes (≤ 340 base64url chars), so 512 is generous.

**account_tokens** — email verification & password reset (schema present; flows deferred
until the instance has SMTP — invite links carry the accept token, so onboarding works
without email): `id`, `user_id` FK CASCADE, `type ENUM('verify_email','reset_password')`,
`token_hash CHAR(64)` (SHA-256 of token, never store raw), `expires_at`, `used_at NULL`,
`created_at`. Index `(user_id, type)`.

**invites** — workspace onboarding: `id`, `email` NULL (NULL = reusable link), `token_hash CHAR(64) UNIQUE`, `role ENUM('member','moderator','admin') DEFAULT 'member'`, `created_by` → users, `expires_at`, `accepted_at NULL`, `created_at`.

**workspace_settings** — singleton: `id TINYINT PRIMARY KEY CHECK (id = 1)`, `name`, `slug`, `logo_media_id NULL → media_assets`, `primary_color CHAR(7) NULL`, `signup_mode ENUM('invite_only','open') DEFAULT 'invite_only'`, `created_at`, `updated_at`. Default row seeded by migration.

### 4.2 Media

**media_assets**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AI | |
| owner_user_id | BIGINT NOT NULL → `users` CASCADE | uploader |
| kind | ENUM('image','video') NOT NULL | |
| status | ENUM('processing','ready','failed','deleted') NOT NULL DEFAULT 'processing' | worker sets `ready`/`failed` |
| storage_key | VARCHAR(512) NOT NULL | S3-compatible or local path |
| mime_type | VARCHAR(127) NOT NULL | |
| width / height | INT UNSIGNED NULL | |
| duration_ms | INT UNSIGNED NULL | video only |
| size_bytes | BIGINT UNSIGNED NOT NULL | |
| blurhash | VARCHAR(64) NULL | placeholder |
| created_at | DATETIME(6) | |

Short video vs long video: not a schema concern — both are `media_assets.kind='video'`;
the **watch feed** is `WHERE kind='video' AND status='ready' ORDER BY created_at` and
short vs long is a `duration_ms` cut-off (≤ 60 s) applied by the query layer.

**post_media** — `post_id` PK-part, `media_id` PK-part, `position SMALLINT`; PK `(post_id, media_id)`; FKs CASCADE.

**message_media** — same shape with `position SMALLINT`; PK `(message_id, media_id)`.

### 4.3 Social graph & feed

**follows** — `follower_id`, `followee_id`; PK `(follower_id, followee_id)`; FKs → users CASCADE; index `(followee_id)` for follower counts / fan-out.

**posts**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AI | |
| author_id | BIGINT NOT NULL → `users` CASCADE | |
| type | ENUM('note','photo','video') NOT NULL DEFAULT 'note' | derived convenience; media presence authoritative |
| body | TEXT NULL | caption/text; NULL allowed for media-only |
| deleted_at | DATETIME(6) NULL | moderator tombstone |
| created_at / updated_at | DATETIME(6) | `updated_at` set on edit |

Indexes: `(author_id, created_at)`, `(created_at)`, `FULLTEXT(body)` (feed search).

**comments** — `id`, `post_id` FK CASCADE, `author_id` → users CASCADE, `parent_id` NULL → `comments(id)` CASCADE (thread depth 1 enforced at app layer), `body TEXT NOT NULL`, `edited_at NULL`, `deleted_at NULL`, `created_at`. Index `(post_id, created_at)`, `(parent_id)`.

**post_reactions / comment_reactions** — `(post_id|comment_id, user_id)` PK pairs + `type ENUM('like','love','haha','wow','sad','angry') DEFAULT 'like'`, `created_at`. Index `(post_id)` (count likes) resp. `(comment_id)`.

**saved_posts** — `user_id`, `post_id` PK pair, `created_at`; FKs CASCADE. (Instagram "save".)

### 4.4 Messaging

**conversations** — `id`, `type ENUM('direct','group')`, `title VARCHAR(80) NULL` (groups), `created_by → users` (SET NULL), `created_at`, `updated_at` (bumped per new message — conversation list sort). Enforce at app layer: one `direct` conversation per user pair (unique index on a normalized pair is awkward in SQL — handled in service code; document).

**conversation_members** — PK `(conversation_id, user_id)`; `role ENUM('member','admin') DEFAULT 'member'` (group admin), `joined_at`, `left_at NULL`, `last_read_message_id BIGINT NULL` (unread badge without a read receipts table), `muted_until NULL`, FKs CASCADE.

**messages** — `id`, `conversation_id` FK CASCADE, `sender_id` → users (SET NULL on purge), `type ENUM('text','image','video','file','system')`, `body TEXT NULL` (NULL for media-only), `edited_at NULL`, `deleted_at NULL`, `created_at`. Index `(conversation_id, id)` (cursor pagination). Per-message "seen" receipts are intentionally excluded (D7 spirit + privacy) — `last_read_message_id` covers unread counts.

### 4.5 Notifications

**notifications**
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AI | |
| recipient_id | BIGINT NOT NULL → `users` CASCADE | |
| actor_id | BIGINT NULL → `users` SET NULL | NULL = system notification |
| type | ENUM('follow','like','comment','reply','mention','message','system') NOT NULL | |
| entity_type | ENUM('post','comment','message','user') NULL | polymorphic handle (D5) |
| entity_id | BIGINT NULL | no FK (D5) |
| read_at | DATETIME(6) NULL | |
| created_at | DATETIME(6) | |

Index `(recipient_id, read_at, created_at)` (unread badge + list).

**notification_preferences** — `user_id` PK-part, `type` PK-part (same ENUM as `notifications`), `created_at`. Presence of a row = that type is muted for the member (FR-7.3); absence = enabled. No extra index — the PK covers user lookups.

### 4.6 Moderation, reports & audit

**reports** — `id`, `reporter_id` → users (SET NULL), `target_type ENUM('post','comment','message','user')`, `target_id BIGINT` (no FK), `reason ENUM('spam','harassment','nsfw','violence','impersonation','other')`, `details TEXT NULL`, `status ENUM('open','reviewing','actioned','dismissed') DEFAULT 'open'`, `resolved_by` NULL → users SET NULL, `resolved_at NULL`, `created_at`. Indexes `(status, created_at)`, `(target_type, target_id)`.

**audit_log** — append-only admin/moderator trail: `id`, `actor_id` NULL → users SET NULL, `action VARCHAR(100)` (e.g. `post.delete`, `user.suspend`, `report.resolve`, `workspace.update`), `target_type VARCHAR(64) NULL`, `target_id BIGINT NULL` (no FK — survives purges), `metadata JSON NULL`, `created_at`. Indexes `(actor_id, created_at)`, `(target_type, target_id)`, `(created_at)` — the latter for date-range filtering in the audit viewer (FR-9.5/FR-10.2).

## 5. Relationship cardinalities (summary)

- users 1—N posts, comments, reactions, messages, notifications(recipient), user_entities (via `name`)
- users N—N follows (self), users N—N blocks (self)
- posts 1—N comments; comment 1—N comment_reactions; posts/comments N—N users (reactions, saves)
- posts N—N media_assets (via post_media); messages N—N media_assets (via message_media)
- conversations 1—N conversation_members; conversations 1—N messages; message_media join
- reports/audit_log: polymorphic, no FK (D5)

## 6. Migration & JPA notes

- `V1__init.sql` creates tables in FK-safe order. `users.avatar_media_id` FK is added by
  `ALTER TABLE` **after** `media_assets` is created (circular dependency). `workspace_settings`
  is seeded with one default row. Pre-release, V1 was extended once (added
  `notification_preferences` and `audit_log`'s `idx_audit_created`) — safe because no
  environment has applied it yet.
- Future changes: additive `V2__…`, `V3__…` — never edit applied migrations (Flyway checksums).
- JPA: map `users`→`User`, … with `@Enumerated(EnumType.STRING)`; `@CreationTimestamp`/UTC
  via entity listeners or `DATETIME(6)` defaults; `deleted_at IS NULL` baked into queries
  (or Hibernate `@Where`) for posts/comments/messages.
- Connection pool: Hikari defaults fine at company scale; UTC set in JDBC URL already.
- Index growth: revisit after real workloads — the named indexes cover feed, comments,
  conversation lists, unread badges, and follower counts; nothing here is speculative.

## 7. Deliberate exclusions

- **Stories/reels extras**, hashtags, mentions table (mentions are stored as notification rows only; text search covers discovery), push device tokens (added per-platform later), presence/last-seen privacy settings, read receipts table, multi-workspace, soft deletes for users (status covers it), denormalized counters, soft-deleted reactions (delete = delete).

## 8. Traceability to `plans.md` (FR/BR → where enforced)

Legend: **V1** = enforced/supported by the V1 schema; **APP** = application layer
(service/UI), documented there. Proposed FRs are marked *(proposal)*.

| plans.md | Enforced by | |
|----------|-------------|--|
| FR-1.1 workspace config | `workspace_settings` (singleton, `CHECK (id = 1)`, seeded) | V1 |
| FR-1.2 invites | `invites` (`token_hash` UNIQUE, `expires_at`, `accepted_at`, role) | V1 |
| FR-1.3 passkey auth | `user_entities`/`user_credentials` (Spring Security WebAuthn JDBC) + `users.password_hash` (optional fallback) | V1 |
<!-- FR-1.7 row updated below: account_tokens exists in schema but email flows are not implemented until SMTP is configured -->
| FR-1.4 / FR-1.5 suspend, deactivate, reactivate | `users.status` ENUM; login & write-path checks | V1 + APP |
| FR-1.6 passkey labels & revoke | `user_credentials.label` (framework-managed; revoke = row DELETE, no user FK) | V1 |
| FR-1.7 email verify / reset | `account_tokens` in schema; flows NOT wired — no SMTP on the instance (invite links carry the token instead) | V1 + APP (deferred) |
| FR-2.1 profile fields | `users(username, display_name, bio, avatar_media_id)` | V1 |
| FR-2.2 followers/following | `follows` (PK pair) + `idx_follows_followee` | V1 |
| FR-2.3 block | `user_blocks` (PK pair) | V1 |
| FR-2.4 avatar via media pipeline | `users.avatar_media_id → media_assets` | V1 |
| FR-2.5 last seen | `users.last_seen_at` (app-throttled writes) | V1 + APP |
| FR-3.1 create posts (note/photo/video) | `posts.type` + `post_media(position)`; mixed-type rule APP | V1 + APP |
| FR-3.2 edit + marker | `posts.updated_at` | V1 |
| FR-3.3 delete/tombstone | `deleted_at` on `posts`; read-path filter APP | V1 + APP |
| FR-3.4 following feed | `idx_posts_author_created`, `idx_posts_created`, `follows` | V1 |
| FR-3.5 discover feed *(proposal)* | same indexes; ranking APP | V1 + APP |
| FR-3.6 search | `FULLTEXT ft_posts_body` | V1 |
| FR-3.7 save posts | `saved_posts` (PK pair) | V1 |
| FR-3.8 reactions | `post_reactions`, `comment_reactions` (ENUM types, PK per user) | V1 |
| FR-3.9 multi-image reorder | `post_media.position` | V1 |
| FR-4.1 replies (1 level) | `comments.parent_id → comments(id)` | V1 |
| FR-4.2 edit/delete comments | `comments.edited_at`, `deleted_at` | V1 |
| FR-4.3 comment list | `idx_comments_post_created` | V1 |
| FR-4.4 @mentions | `notifications(type='mention')`; write-time parse APP | V1 + APP |
| FR-4.5 comment reactions | `comment_reactions` | V1 |
| FR-5.1 upload images/video | `media_assets(kind, storage_key, mime_type, size_bytes)` | V1 |
| FR-5.2 async processing | `media_assets.status` (`processing→ready|failed`); `idx_media_status_created` | V1 |
| FR-5.3 short vs. long video | `duration_ms ≤ 60000` predicate in queries | V1 |
| FR-5.4 attach to posts/messages | `post_media`, `message_media` | V1 |
| FR-5.5 avatar/logo via pipeline | `media_assets` + `workspace_settings.logo_media_id` | V1 |
| FR-5.6 retry & 24 h sweeper | `status='failed'` rows + `idx_media_status_created` | V1 + APP |
| FR-5.7 opaque keys / served authz-checked | `storage_key` (opaque, 512) | V1 + APP |
| FR-6.1 direct messages | `conversations` + `conversation_members`; 1:1 uniqueness APP | V1 + APP |
| FR-6.2 groups (admin role) | `conversations.type='group'`, `conversation_members.role='admin'` | V1 |
| FR-6.3 history pagination | `idx_messages_conversation_id (conversation_id, id)` | V1 |
| FR-6.4 unread badge | `conversation_members.last_read_message_id` | V1 |
| FR-6.5 mute | `conversation_members.muted_until` | V1 |
| FR-6.6 media in messages | `message_media` | V1 |
| FR-6.7 real-time delivery *(proposal)* | transport decision; no schema object | APP |
| FR-7.1 notification types | `notifications.type` ENUM matches FR-7.1 list | V1 |
| FR-7.2 unread badge + list | `idx_notifications_recipient_unread (recipient_id, read_at, created_at)` | V1 |
| FR-7.3 per-type mute | `notification_preferences` (presence = muted) | V1 |
| FR-8.1 search | `posts` FULLTEXT; filter APP | V1 + APP |
| FR-8.2 member search | `uk_users_username` (prefix match) | V1 |
| FR-9.1 report flow | `reports` (`target_type`, `reason`, `status` ENUMs) | V1 |
| FR-9.2 queue transitions | `reports.status` + `idx_reports_status_created` | V1 |
| FR-9.3 / FR-9.4 actions | `audit_log` (append-only) + tombstones on content | V1 |
| FR-9.5 audit viewer | `idx_audit_created`, `idx_audit_actor_created`, `idx_audit_target` | V1 |
| FR-10.2 audit filter | same `audit_log` indexes | V1 |
| FR-10.3 backup/export | app-level (mysqldump/export job) | APP |
| FR-10.4 actuator/health | Actuator (no schema) | APP |
| FR-10.5 retention sweeper | `idx_media_status_created`; invites/account_tokens expire — small tables, app scan | V1 + APP |

Business rules:

| plans.md | Enforcement | Where |
|----------|-------------|-------|
| BR-1.1 single workspace | no `tenant_id` anywhere by design (D1) | V1 |
| BR-1.2 uniqueness (ci) | `uk_users_username`, `uk_users_email` (+ `utf8mb4_0900_ai_ci`); lowercased at app | V1 + APP |
| BR-1.3 first user = admin | bootstrap check in invite-accept flow | APP |
| BR-1.4 admin-only role grants | `role` checks in service layer | APP |
| BR-1.5 invite-only default | `workspace_settings.signup_mode DEFAULT 'invite_only'` + registration gate APP | V1 + APP |
| BR-1.6 slug immutability | enforced in workspace-settings service | APP |
| BR-1.7 purge w/ export first | export job, then `users` delete → FK cascades purge content | APP + V1 |
| BR-2.1 no self-follow/block | service-layer checks | APP |
| BR-2.2 mutual block effect | feed/profile/message filters on `user_blocks` | APP |
| BR-2.3 follower counts derived | no counter columns (D7); `COUNT(*)` over `follows` | V1 |
| BR-2.4 deactivated rendering | `users.status`; rendering APP | V1 + APP |
| BR-3.1 tombstone hidden | `deleted_at` + read-path filters | V1 + APP |
| BR-3.2 type purity (no mixed media) | service validation; `posts.type` vs `post_media` (image vs video) | V1 + APP |
| BR-3.3 5000-char post cap *(proposal)* | service-level validation | APP |
| BR-3.4 one reaction per target | reaction PK `(target_id, user_id)` | V1 |
| BR-3.5 edit, no window, marked | `posts.updated_at` presence = edited | V1 |
| BR-3.6 pull fan-out | no feed materialization tables | V1 |
| BR-4.1 one reply level | app rejects reply-to-reply; `parent_id` self-FK | V1 + APP |
| BR-4.2 thread survives parent tombstone | tombstone (not DELETE) keeps replies; FK self-CASCADE applies only to hard purge | V1 + APP |
| BR-4.3 2000-char comment cap *(proposal)* | service validation | APP |
| BR-4.4 tombstoned post read-only | flag check in comment service | APP |
| BR-5.1 shorts = `duration_ms` predicate | query-level | V1 |
| BR-5.2 media authz-checked | media download endpoint checks membership/ownership | APP |
| BR-5.3 attach own media only | `media_assets.owner_user_id` check at attach time | V1 + APP |
| BR-5.4 orphan media GC | `media_assets.status='deleted'` + sweeper deletes blobs | V1 + APP |
| BR-5.5 EXIF/GPS strip *(proposal)* | upload pipeline strips metadata | APP |
| BR-6.1 one direct conversation per pair | service-level lookup/unique constraint on normalized pair key *(V2 candidate)* | APP |
| BR-6.2 block blocks messaging | blocked-pair check at send | APP |
| BR-6.3 leave/remove behavior | `conversation_members.left_at`; read gate APP | V1 + APP |
| BR-6.4 editable w/ `edited_at` | `messages.edited_at` | V1 |
| BR-6.5 purged sender survives | `messages.sender_id → users SET NULL` | V1 |
| BR-6.6 no read receipts v1 | deliberate exclusion (§7) | V1 |
| BR-7.1 in-app only, nothing external | no external-send code paths; assets bundled | APP |
| BR-7.2 purged actor renders NULL | `notifications.actor_id → users SET NULL` | V1 |
| BR-7.3 write fan-out | service writes notification rows | APP |
| BR-8.1 search scope | filters APP; index serves it | V1 + APP |
| BR-8.2 workspace-scoped only | no cross-instance anything (D1) | V1 |
| BR-9.1 moderator-only queue | role checks on report-resolution endpoints | APP |
| BR-9.2 audit never edited | no app code paths updating/deleting `audit_log`; DB-level revoke for app user if desired | APP + V1 |
| BR-9.3 report rate limit *(proposal)* | service rate limit (1 per member per target / 24h) | APP |
| BR-9.4 repeat violations → suspension | admin step | APP |
| BR-9.5 purge all-or-nothing | `ON DELETE CASCADE` member content tree | V1 |
| BR-10.x ops/secrets/Flyway discipline | operational (no schema) | APP |
| BR-11.x data residency & deletion honesty | architecture + retention jobs | APP |

*(FRs/BRs marked proposal await owner sign-off; enforcement location is as designed today.)*

## 9. Open questions

1. Group chats: is `conversations.type='group'` enough, or do groups need channels/lists (Slack-like)? Affects messaging.
2. Mentions: parse `@username` at write time (needs usernames) — ok? Or store structured mentions?
3. Should purged user content be truly unrecoverable immediately, or 30-day trash for admins? Drives `deleted_at` retention job.
4. Feed ranking beyond recency (e.g., following-only vs. workspace-wide "discover") — affects indexes only, not tables.
5. Full-text: MySQL `FULLTEXT` on `posts.body` only, or also `comments.body`?
6. At-rest encryption for media (company disk encryption assumed) or app-level encryption for messages?

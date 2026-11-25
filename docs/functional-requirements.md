# Chafgram — Functional Requirements

> What Chafgram must do (FR). Companion doc: [`business-rules.md`](business-rules.md), the
> rules it must obey (BR) — same section numbering, so `FR-3.4` and `BR-3.2` describe the
> same feature area. Scope: the full product — Facebook/Instagram-inspired private social
> platform, self-hosted per company.
> Data-model enforcement lives in the Flyway migrations
> (`backend/api/src/main/resources/db/migration/`); behavior without a schema anchor is
> stated as a rule in `business-rules.md`.
> Items marked **(proposal)** need owner sign-off.
> Priority: **M**ust / S**h**ould / **C**ould (MoSCoW).
>
> Roles: `member`, `moderator`, `admin` — one workspace per deployment.

## 1. Identity, workspace & invites

**FR-1.1 (M)** An admin creates/configures the workspace: name, slug, logo, primary
color, signup mode (`invite_only` default | `open`).
**FR-1.2 (M)** Admins invite members by email or by reusable link; invites carry a role,
expire (`expires_at`), and are accepted at most once (`accepted_at` is written).
**FR-1.3 (M)** Members authenticate primarily with passkeys (WebAuthn;
`user_entities`/`user_credentials`, managed by Spring Security's WebAuthn
framework), with an optional password fallback per member.
**FR-1.4 (M)** Admins can change member roles, suspend, and deactivate; deactivated
members cannot authenticate; suspended members can read but not write.
**FR-1.5 (S)** Admins can reactivate suspended/deactivated members.
**FR-1.6 (S)** A member may register several passkeys and name each
(`label`), revoke them individually.
**FR-1.7 (S)** Optional email verification and password-reset flows via single-use,
time-boxed tokens (`account_tokens`, only SHA-256 hashes stored).

## 2. Members, profiles & social graph

**FR-2.1 (M)** Members have profile: username, display name, bio (≤ 160), avatar.
**FR-2.2 (M)** Follow/unfollow any member; view followers/following lists.
**FR-2.3 (M)** Block/unblock a member.
**FR-2.4 (S)** Avatar upload via the media pipeline (`avatar_media_id`).
**FR-2.5 (S)** Last-seen presence (throttled client updates).

## 3. Posts & feed

**FR-3.1 (M)** Members create posts: text notes, photo posts (1..N images), video posts
(1 video). Body optional for media-only posts.
**FR-3.2 (M)** Posts can be edited; `updated_at` records the edit; `edited` marker shown.
**FR-3.3 (M)** Authors delete own posts; `moderator`/`admin` delete any post; deletion is
a tombstone (`deleted_at`), not a row drop.
**FR-3.4 (M)** Home feed = reverse-chronological following-feed (people/pages you follow)
with cursor pagination.
**FR-3.5 (M)** Workspace-wide feed ("discover") for all non-tombstoned content —
recency-ordered. *(proposal — decide per product.md open question 4)*
**FR-3.6 (S)** Full-text search over posts (`FULLTEXT(body)`), scoped to the workspace,
excluding tombstones and blocked authors.
**FR-3.7 (S)** Save/unsave posts; saved list per member.
**FR-3.8 (S)** Reactions on posts and comments: `like|love|haha|wow|sad|angry`
(`post_reactions`, `comment_reactions`).
**FR-3.9 (C)** Drag-and-drop reordering of multi-image posts *(proposal)*.

## 4. Comments

**FR-4.1 (M)** Members comment on posts; replies to comments allowed at 1 level of depth.
**FR-4.2 (M)** Comment authors edit own comments (`edited_at` shown); authors and
`moderator`/`admin` can delete (tombstone).
**FR-4.3 (M)** Comments list per post with cursor pagination, oldest-first.
**FR-4.4 (S)** @mentions in comments/post bodies notify mentioned members. *(proposal:
parse at write time; membership of mention scope = same workspace)*
**FR-4.5 (C)** Reactions on comments (see FR-3.8).

## 5. Media & video

**FR-5.1 (M)** Members upload images (JPEG/PNG/WebP, ≤ 20 MB) and videos
(MP4/WebM, ≤ 4 GB). *(proposal limits)*
**FR-5.2 (M)** Server processes uploads asynchronously: transcode (video), thumbnail,
dimensions, duration, blurhash → `media_assets.status: processing → ready|failed`.
**FR-5.3 (M)** Short videos: ≤ 60 s, surface in the vertical short-video feed ("watch").
Longer videos surface in the regular feed and a Watch area.
**FR-5.4 (M)** Media is referenced from posts (`post_media`) and messages (`message_media`).
**FR-5.5 (S)** Avatar/logo uploads reuse `media_assets` (owner = uploader; logo kind
image).
**FR-5.6 (S)** Failed uploads are retryable by the owner; stuck `processing` rows are
marked `failed` by a sweeper after 24 h. *(proposal)*
**FR-5.7 (C)** Direct media URLs are unguessable (opaque `storage_key`, time-limited
download if storage is S3-compatible).

## 6. Messaging

**FR-6.1 (M)** 1:1 direct messages with any member (unless blocked).
**FR-6.2 (S)** Group conversations: create, title, add/remove members, group admin role
(`conversation_members.role`).
**FR-6.3 (M)** Message history with cursor pagination `(conversation_id, id)`.
**FR-6.4 (M)** Unread badge per conversation via `last_read_message_id`; "mark read" on
open.
**FR-6.5 (S)** Mute a conversation until a given time (`muted_until`); muted = no badge.
**FR-6.6 (M)** Media messages (image/video) in both 1:1 and groups via `message_media`.
**FR-6.7 (S)** Real-time delivery. *(proposal — WebSocket/SSE; transport decision pending;
pre-MVP behavior = poll/refresh)*

## 7. Notifications

**FR-7.1 (M)** Notify members on: new follower, like/reaction, comment on your post,
reply to your comment, @mention, direct message.
**FR-7.2 (M)** Unread count + list, newest first; mark one/all read.
**FR-7.3 (S)** Per-type mute preferences — `notification_preferences` (presence = muted;
V1).

## 8. Search & discovery

**FR-8.1 (S)** Search posts by text (FULLTEXT), filter: notes/photos/videos.
**FR-8.2 (C)** Member search (username/display name prefix match). *(proposal)*
**FR-8.3 (C)** Trending/popular within workspace. *(proposal — feed ranking open)*

## 9. Moderation & reports

**FR-9.1 (M)** Members report a post, comment, message, or user
(`reason: spam|harassment|nsfw|violence|impersonation|other`).
**FR-9.2 (M)** Moderator queue: list reports, take ownership (`reviewing`),
action (`actioned`) or dismiss (`dismissed`).
**FR-9.3 (M)** Actions taken by moderators/admins write `audit_log` entries (append-only).
**FR-9.4 (M)** moderators/admins can delete any post/comment/message; admins can suspend
or deactivate any member and change roles.
**FR-9.5 (S)** View the audit log (who did what, when) — moderator-visible.
**FR-9.6 (S)** Bulk actions (e.g., purge reported media). *(proposal)*

## 10. Workspace administration & ops

**FR-10.1 (M)** Admin console: members (list/search/filter/role/suspend/deactivate),
workspace settings (name/slug/logo/color/signup mode).
**FR-10.2 (S)** Audit log viewer with filter (actor, action, target type, date range).
**FR-10.3 (S)** Backup/export: admin-triggered full export of workspace data.
*(proposal — format TBD; GDPR-style member export per member)*
**FR-10.4 (S)** Instance health: actuator endpoints for uptime/metrics (admin-only).
**FR-10.5 (C)** Scheduled retention jobs (e.g., purge `processing` media, expired invites).
*(proposal)*

See also: [`product.md`](../product.md) (intent), [`business-rules.md`](business-rules.md)
(rules), [`architecture.md`](architecture.md) (how).

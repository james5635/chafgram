# Chafgram — Functional Requirements & Business Rules

> What Chafgram must do (FR) and the rules it must obey (BR). Scope: the full product —
> Facebook/Instagram-inspired private social platform, self-hosted per company.
> Data-model enforcement is noted as `db-schema.md §` where it exists; behavior without a
> schema anchor is stated as a rule. Items marked **(proposal)** need owner sign-off.
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

**BR-1.1** A deployment serves exactly one workspace; there is no cross-workspace data,
identity, or discovery.
**BR-1.2** `username` (≤ 30 chars) and `email` are unique per workspace; comparison is
case-insensitive (collation); emails/usernames are stored lowercased.
**BR-1.3** Bootstrap: the first account created in a fresh deployment becomes `admin`.
**(proposal)**
**BR-1.4** Only an `admin` may grant/revoke `admin`; `moderator` may not change roles.
**BR-1.5** `invite_only` is the default signup mode; `open` remains bounded by whoever can
reach the instance (private network/proxy) — it never means internet-public signup.
**BR-1.6** Workspace slug and name changes never rewrite member content.
**BR-1.7** A purged (hard-deleted) member's content is removed (FK cascades) and their
`email`/`username` become reusable; an export must be offered before purge. *(proposal)*

## 2. Members, profiles & social graph

**FR-2.1 (M)** Members have profile: username, display name, bio (≤ 160), avatar.
**FR-2.2 (M)** Follow/unfollow any member; view followers/following lists.
**FR-2.3 (M)** Block/unblock a member.
**FR-2.4 (S)** Avatar upload via the media pipeline (`avatar_media_id`).
**FR-2.5 (S)** Last-seen presence (throttled client updates). *(proposal)*

**BR-2.1** A member cannot follow or block themselves.
**BR-2.2** Blocking is mutual-effect: neither party sees the other's posts, comments,
messages, or profile actions, and no new follows/messages in either direction. Existing
content stays hidden, not deleted.
**BR-2.3** Follower counts are derived (COUNT over `follows`), never stored — no drift.
**BR-2.4** Deactivated profiles render as "Deactivated member"; suspended profiles stay
visible, badged suspended.

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

**BR-3.1** Tombstoned posts are invisible everywhere — feed, search, profile, media links,
comments context.
**BR-3.2** A post is either a note (text), photo (≥1 image, no video) or video (exactly 1
video); mixed photo+video posts are rejected.
**BR-3.3** Post body length cap: 5,000 characters. *(proposal)*
**BR-3.4** A member may react at most once per target; switching type updates the row
(PK `(post_id|comment_id, user_id)`); removing the reaction deletes the row.
**BR-3.5** Only the author edits; edits have no time window but are always marked.
**BR-3.6** Feed fan-out is pull-based (indexed author/followee lookups) — no precomputed
feed materialization.

## 4. Comments

**FR-4.1 (M)** Members comment on posts; replies to comments allowed at 1 level of depth.
**FR-4.2 (M)** Comment authors edit own comments (`edited_at` shown); authors and
`moderator`/`admin` can delete (tombstone).
**FR-4.3 (M)** Comments list per post with cursor pagination, oldest-first.
**FR-4.4 (S)** @mentions in comments/post bodies notify mentioned members. *(proposal:
parse at write time; membership of mention scope = same workspace)*
**FR-4.5 (C)** Reactions on comments (see FR-3.8).

**BR-4.1** Replies attach to top-level comments only; no nested replies (`parent_id` →
`comments`).
**BR-4.2** Deleting a parent hides it as "[removed]" but keeps its replies visible;
tombstoned parents' children stay attached.
**BR-4.3** Comment body length cap: 2,000 characters. *(proposal)*
**BR-4.4** A tombstoned post accepts no new comments; existing comments remain readable.

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

**BR-5.1** Short vs. long video is a `duration_ms ≤ 60000` predicate — no separate storage
or table for shorts.
**BR-5.2** Media is always served through Chafgram (authz-checked), never as anonymous
static files. *(proposal)*
**BR-5.3** Members can attach only media they uploaded; admins can reassign/delete any
media for moderation.
**BR-5.4** When a post is tombstoned and no other reference exists, its media is purged
from storage (scheduled job). *(proposal)*
**BR-5.5** Image EXIF/GPS metadata is stripped on upload. *(proposal)*

## 6. Messaging

**FR-6.1 (M)** 1:1 direct messages with any member (unless blocked).
**FR-6.2 (S)** Group conversations: create, title, add/remove members, group admin role
(`conversation_members.role`).
**FR-6.3 (M)** Message history with cursor pagination `(conversation_id, id)`.
**FR-6.4 (M)** Unread badge per conversation via `last_read_message_id`; "mark read" on
open.
**FR-6.5 (S)** Mute a conversation until a given time (`muted_until`); muted = no badge.
**FR-6.6 (M)** Media messages (image/video) in both 1:1 and groups via `message_media`.
**FR-6.7 (S)** Real-time delivery. *(proposal — WebSocket/SSE; decided in roadmap Phase 0;
pre-MVP behavior = poll/refresh)*

**BR-6.1** At most one direct (`type='direct'`) conversation per unordered user pair;
enforced by application service (not a unique index — pairs are unordered).
**BR-6.2** A blocked member cannot send messages to the blocker; existing histories remain
visible to both.
**BR-6.3** Leaving or being removed from a group removes the member from the conversation;
they can no longer read new messages; past messages remain on server per retention
policy. *(proposal)*
**BR-6.4** Messages may be edited by their sender; `edited_at` is stored; moderators can
tombstone messages. *(edit time-window: open decision)*
**BR-6.5** `sender_id` is retained as nullable: if the sender is purged, their messages
survive as "Deactivated member" (schema `ON DELETE SET NULL`).
**BR-6.6** There are no per-message read receipts in v1 — only conversation-level unread
state.

## 7. Notifications

**FR-7.1 (M)** Notify members on: new follower, like/reaction, comment on your post,
reply to your comment, @mention, direct message.
**FR-7.2 (M)** Unread count + list, newest first; mark one/all read.
**FR-7.3 (S)** Per-type mute preferences — `notification_preferences` (presence = muted;
V1).

**BR-7.1** Notifications are in-app only; nothing is ever sent outside the company
instance — no email, no Web Push, no analytics ping.
**BR-7.2** If the actor is purged, the notification renders as "Deactivated member";
`actor_id` is set NULL (schema `ON DELETE SET NULL`).
**BR-7.3** Notifications are write-fanned-out (row per recipient); deleting the underlying
content hides the notification but does not error the feed.

## 8. Search & discovery

**FR-8.1 (S)** Search posts by text (FULLTEXT), filter: notes/photos/videos.
**FR-8.2 (C)** Member search (username/display name prefix match). *(proposal)*
**FR-8.3 (C)** Trending/popular within workspace. *(proposal — feed ranking open)*

**BR-8.1** Search results never include tombstoned content, blocked authors, or
purged members' identities.
**BR-8.2** Search is workspace-scoped only — no cross-instance discovery exists.

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

**BR-9.1** Only `moderator`/`admin` resolve reports; `member` has no visibility into the
queue or audit log.
**BR-9.2** Every moderation state transition emits an audit entry; audit rows are never
edited or deleted through the application.
**BR-9.3** A member may not report the same target repeatedly in a spam loop — rate limit
reports per member per target (e.g., 1 / 24 h). *(proposal)*
**BR-9.4** Repeated substantiated violations → admin suspension (manual step, not automatic).
**BR-9.5** Deactivated members' content survives purges only if the admin keeps it; purge
is all-or-nothing for the member.

## 10. Workspace administration & ops

**FR-10.1 (M)** Admin console: members (list/search/filter/role/suspend/deactivate),
workspace settings (name/slug/logo/color/signup mode).
**FR-10.2 (S)** Audit log viewer with filter (actor, action, target type, date range).
**FR-10.3 (S)** Backup/export: admin-triggered full export of workspace data.
*(proposal — format TBD; GDPR-style member export per member)*
**FR-10.4 (S)** Instance health: actuator endpoints for uptime/metrics (admin-only).
**FR-10.5 (C)** Scheduled retention jobs (e.g., purge `processing` media, expired invites).
*(proposal)*

**BR-10.1** Chafgram never initiates outbound network calls without explicit admin action
(e.g., font fetch, update check) — air-gapped installs must work. Consequence: assets are
bundled, not CDN-linked, in release builds.
**BR-10.2** Secrets live only in environment/git-ignored config; the app refuses to boot
with default credentials outside the dev profile. *(proposal for dev-only exception)*
**BR-10.3** All schema changes ship as Flyway migrations; the app must not use
`ddl-auto=create/update` in any environment.
**BR-10.4** Instance upgrades are one-node and forward-only; no down-migrations are
promised — rollback = restore from backup.

## 11. Business rules — compliance & trust

**BR-11.1** Data residency: all member data (MySQL, media, logs) stays inside the
company-controlled environment; Chafgram sends nothing externally by default.
**BR-11.2** Deletion is honest: hard-deleted member data (incl. media bytes) is not
retained beyond a pending 30-day admin-trash window, if enabled. *(proposal)*
**BR-11.3** Audit retention: `audit_log` is retained indefinitely unless an external
retention policy is configured; it is never user-deletable.
**BR-11.4** Chafgram's source is MIT; any fork/derivative remains usable under MIT.

## 12. Open business questions (owner decisions needed)

1. Message edit window after send (unlimited vs. 15 min vs. never).
2. Discover feed on/off by default for new workspaces.
3. Is `open` signup mode ever used, or always `invite_only` in practice?
4. Group chat semantics: simple titled groups vs. channels.
5. Retention/legal-hold policy and e-discovery export format.
6. Whether profile deactivation for members is self-service or admin-mediated.

See also: [`product.md`](../product.md) (intent), [`architecture.md`](architecture.md)
(how), [`db-schema.md`](db-schema.md) (data), [`roadmap.md`](roadmap.md) (order).

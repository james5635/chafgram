# Chafgram — Business Rules

> The rules Chafgram must obey (BR). Companion doc: [`functional-requirements.md`](functional-requirements.md),
> what it must do (FR) — same section numbering, so `BR-3.2` and `FR-3.4` describe the same
> feature area. Scope: the full product — Facebook/Instagram-inspired private social
> platform, self-hosted per company.
> Data-model enforcement lives in the Flyway migrations
> (`backend/src/main/resources/db/migration/`); behavior without a schema anchor is
> stated here as a rule.
> Items marked **(proposal)** need owner sign-off.
>
> Roles: `member`, `moderator`, `admin` — one workspace per deployment.

## 1. Identity, workspace & invites

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

**BR-2.1** A member cannot follow or block themselves.
**BR-2.2** Blocking is mutual-effect: neither party sees the other's posts, comments,
messages, or profile actions, and no new follows/messages in either direction. Existing
content stays hidden, not deleted.
**BR-2.3** Follower counts are derived (COUNT over `follows`), never stored — no drift.
**BR-2.4** Deactivated profiles render as "Deactivated member"; suspended profiles stay
visible, badged suspended.

## 3. Posts & feed

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

**BR-4.1** Replies attach to top-level comments only; no nested replies (`parent_id` →
`comments`).
**BR-4.2** Deleting a parent hides it as "[removed]" but keeps its replies visible;
tombstoned parents' children stay attached.
**BR-4.3** Comment body length cap: 2,000 characters. *(proposal)*
**BR-4.4** A tombstoned post accepts no new comments; existing comments remain readable.

## 5. Media & video

**BR-5.1** Short vs. long video is a `duration_ms ≤ 60000` predicate — no separate storage
or table for shorts.
**BR-5.2** Media is always served through Chafgram (authz-checked), never as anonymous
static files. *(proposal)*
**BR-5.3** Members can attach only media they uploaded; admins can delete any media for
moderation (tombstone + audit-log entry; the bytes stay on disk until a retention sweep).
Reassigning media to another member is not part of v1.
**BR-5.4** When a post is tombstoned and no other reference exists, its media is purged
from storage (scheduled job). *(proposal)*
**BR-5.5** Image EXIF/GPS metadata is stripped on upload. *(proposal)*

## 6. Messaging

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

**BR-7.1** Notifications are in-app only; nothing is ever sent outside the company
instance — no email, no Web Push, no analytics ping.
**BR-7.2** If the actor is purged, the notification renders as "Deactivated member";
`actor_id` is set NULL (schema `ON DELETE SET NULL`).
**BR-7.3** Notifications are write-fanned-out (row per recipient); deleting the underlying
content hides the notification but does not error the feed.

## 8. Search & discovery

**BR-8.1** Search results never include tombstoned content, blocked authors, or
purged members' identities.
**BR-8.2** Search is workspace-scoped only — no cross-instance discovery exists.

## 9. Moderation & reports

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

**BR-10.1** Chafgram never initiates outbound network calls without explicit admin action
(e.g., font fetch, update check) — air-gapped installs must work. Consequence: assets are
bundled, not CDN-linked, in release builds.
**BR-10.2** Secrets live only in environment/git-ignored config; the app refuses to boot
with default credentials outside the dev profile. *(proposal for dev-only exception)*
**BR-10.3** All schema changes ship as Flyway migrations; the app must not use
`ddl-auto=create/update` in any environment.
**BR-10.4** Instance upgrades are one-node and forward-only; no down-migrations are
promised — rollback = restore from backup.

## 11. Compliance & trust

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

See also: [`product.md`](../product.md) (intent), [`functional-requirements.md`](functional-requirements.md)
(requirements), [`architecture.md`](architecture.md) (how).

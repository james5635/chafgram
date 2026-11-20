# Chafgram — Web Wireframe

> Complete wireframe specification for the Chafgram member-facing web app (desktop +
> tablet). Grounded in [`design.md`](../design.md) (tokens, components, motion, a11y),
> [`product.md`](../product.md) (intent), [`docs/functional-requirements.md`](functional-requirements.md)
> (FR), [`docs/business-rules.md`](business-rules.md) (BR), [`docs/architecture.md`](architecture.md)
> (API surface), and the reference UI example (`.tmp/web-ui.webp`).
>
> The reference shows a Facebook/Instagram-inspired social shell (left rail nav, composer,
> feed cards, right-rail "Who to follow"/"Trending"); this doc re-specifies that structure —
> plus every other screen in the product — in Chafgram's lagoon/palm design language.
>
> Status: design proposal. Confirm IA (rail vs tabs), right-rail visibility, and admin
> surface scope with the owner before implementing (product.md open questions 1, 8; design.md
> §13/#14; BR-12).

---

## 1. Design system alignment

Every screen below uses the `design.md` token set — CSS custom properties, never raw hex.
Key tokens:

| Role | Light token | Dark token | Use |
|------|-------------|------------|-----|
| Primary text | `--sea-ink` `#173a40` | `--sea-ink` `#d7ece8` | Body, headings, labels |
| Secondary text | `--sea-ink-soft` `#416166` | `--sea-ink-soft` `#afcdc8` | Captions, metadata, inactive nav |
| Accent | `--lagoon` `#4fb8b2` | `--lagoon` `#60d7cf` | Links, focus, primary CTAs |
| Accent strong | `--lagoon-deep` `#328f97` | `--lagoon-deep` `#8de5db` | Active link text, button text |
| Brand kicker | `--palm` `#2f6a4a` | `--palm` `#6ec89a` | Eyebrow text, brand gradient end |
| Base bg | `--bg-base` `#e7f3ec` | `--bg-base` `#0a1418` | Page background |
| Card fill | `--surface` `rgba(255,255,255,0.74)` | `--surface` `rgba(16,30,34,0.8)` | `.island-shell` cards |
| Card fill strong | `--surface-strong` `rgba(255,255,255,0.9)` | `--surface-strong` `rgba(15,27,31,0.92)` | Elevated cards |
| Hairline | `--line` `rgba(23,58,64,0.14)` | `--line` `rgba(141,229,219,0.18)` | Dividers, input borders |
| Header bg | `--header-bg` `rgba(251,255,248,0.84)` | `--header-bg` `rgba(10,20,24,0.8)` | Sticky header (backdrop-blur) |
| Chip | `--chip-bg` / `--chip-line` | `--chip-bg` / `--chip-line` | Pills, brand chip |

Component primitives (from `design.md` §7–§9):

- **`.island-shell`** — 1px `--line`, `165deg` gradient `--surface-strong`→`--surface`,
  inner `--inset-glint` top highlight, two-layer soft shadow, `blur(4px)`, radius `1rem–2rem`.
  Cards `0.9–1rem`. Interactive shells get hover lift `translateY(-2px)` + lagoon border-mix.
- **Header** — `--header-bg` backdrop-blur, hairline `--line` bottom border, brand chip
  (rounded-full, `--chip-bg`/`--chip-line`, pulsing two-tone lagoon dot).
- **Nav link** — inactive `--sea-ink-soft`; active `--sea-ink` + animated 2px lagoon-gradient
  underline (`scaleX 0→1`, 170ms).
- **Button primary** — rounded-full tonal lagoon `rgba(79,184,178,0.14→0.24)` hover,
  `--lagoon-deep` text.
- **Button secondary/ghost** — transparent, `--sea-ink`, hover `--link-bg-hover`.
- **Input** — `.demo-input`: 0.75rem radius, `--line` border, focus `--lagoon-deep` border +
  3px `--lagoon` 24% ring.
- **Pill / tag** — `.demo-pill`: rounded-full, chip tokens, 0.75rem 700.
- **Alert** — `.demo-alert`: tinted (lagoon info, palm/success, amber warning, red danger),
  1rem radius, never a bare code — always a path forward.
- **Empty state** — kicker + one line + one action; never a dead end.
- **Skeleton** — `sand`/`foam` shimmer (no spinners for content regions; `design.md` §13).
- **Motion** — `.rise-in` 700ms `cubic-bezier(0.16,1,0.3,1)`, `translateY(12px)` + fade,
  staggered ~90ms per sibling; hover/press 180ms ease; respects `prefers-reduced-motion`.

Typography: **Fraunces** (display/h1 only, `line-height ≈ 1.02–1.05`, never below ~1.25rem),
**Manrope** (body 400/500, emphasis 600/700, buttons 700, kickers 700 uppercase `0.16em`
tracking `0.69rem`). Never pure `#000`/`#fff` text.

---

## 2. Screen inventory

| Path | Auth | Screen | Primary action |
|------|------|--------|----------------|
| `/welcome` | No | Welcome / landing | Sign in or contact admin |
| `/login` | No | Login | Passkey-first sign in; password fallback |
| `/` | Yes | Home feed | Compose post; scroll following-feed |
| `/explore` | Yes | Explore / Watch | Browse For You / Following / Trending / Nearby |
| `/notifications` | Yes | Notifications | Read + mark activity read |
| `/profile/:username` | Yes | Profile | View posts / replies / media / likes; edit own |
| `/messages` | Yes | Direct messages (list) | Open a conversation |
| `/messages/:conversationId` | Yes | Direct messages (thread) | Read/send messages |
| `/compose` | Yes | Create post | Photo/video/text post; one primary "Post" |
| `/admin/members` | Admin | Admin: members | List/search/filter/role/suspend/deactivate |
| `/admin/settings` | Admin | Admin: workspace settings | Name/slug/logo/color/signup mode |
| `/admin/audit` | Admin/Mod | Admin: audit log | Filter by actor/action/target/date |
| `/admin/reports` | Admin/Mod | Admin: reports queue | Review/action/dismiss reports |
| `/settings` | Yes | Member settings | Theme, passkeys, notification prefs |

Roles: `member`, `moderator`, `admin`. One workspace per deployment (BR-1.1).

---

## 3. Unauthenticated screens

### 3.1 Welcome (`/welcome`)

Full-bleed ambient background (hero washes, `design.md` §5). Centered content column, max ~600px.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    welcome card  (island-shell, max ~600px)                 │
│                                                                             │
│   [Brand chip · Chafgram]                                          [Theme ⓘ]│
│                                                                             │
│   kicker: "Chafgram"                                                       │
│   h1: "Your company's social space"         Fraunces                       │
│   lead: "Connect. Share. Belong. — inside your workspace, on your own      │
│          servers."          Manrope                                         │
│                                                                             │
│   ambient illustration: tonal lagoon/sand gradient + speech-bubble glyph   │
│   (never stock photography; design.md §11)                                  │
│                                                                             │
│   [ Sign in ]                                                    lagoon pill│
│   [ Contact your admin ]                                        ghost       │
│                                                                             │
│   "New to Chafgram? Your admin creates your account via invite."           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Behavior:**
- [Sign in] → `/login`.
- [Contact your admin] → info alert: signup is `invite_only` by default (BR-1.5); members are invited by email/link and accept at `POST /api/v1/invites/accept`.
- No public "Create account" unless `signup_mode=open` (then link to `POST /api/v1/register`).
- Fresh install with no members: bootstrap (`POST /api/v1/bootstrap`, first admin only, BR-1.3) is a separate setup flow — not on welcome by default.

Traceability: FR-1.1, FR-1.3, BR-1.3, BR-1.5, architecture.md §4.1.

### 3.2 Login (`/login`)

Full-bleed ambient background, centered card max ~480px. No rails/nav (unauthenticated).

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  [Brand chip · Chafgram]                                          [Theme ⓘ] │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                     login card  (island-shell, max ~480px)                  │
│                                                                             │
│   kicker: "Sign in to your workspace"                                       │
│   h1: Welcome back                                                      Fraunces│
│                                                                             │
│   [ Sign in with passkey 🔑 ]                                   primary lagoon│
│   → WebAuthn ceremony: /webauthn/authenticate/options → /login/webauthn     │
│                                                                             │
│   ──── or ────                                                              │
│                                                                             │
│   [ Sign in with password ]                                   secondary ghost │
│   ▼ progressive disclose                                              │
│   email/username input  (.demo-input, --lagoon focus ring)                 │
│   password input        (.demo-input)                                      │
│   [ Continue ]                                                      lagoon   │
│                                                                             │
│   Don't have an account?  Contact your admin                                │
│   [ Need help? ]                                                            │
│                                                                             │
│   .demo-alert (tinted lagoon info / red danger) — errors w/ path forward   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Flow:**
1. Passkey (primary) → `navigator.credentials.get()` → `/login/webauthn` → session cookie → redirect `/`. Passkey-first per FR-1.3.
2. Password (secondary) → email/username + password inputs → [Continue] → session cookie → `/`. Optional password fallback per member (FR-1.3).
3. Errors: `.demo-alert`, never a bare code — always a path forward (`design.md` §3).
4. A11y: `aria-label` on all buttons; `lang="en"`; visible focus ring; respects `prefers-reduced-motion`.

Traceability: FR-1.3, BR-1.3, BR-1.5, architecture.md §4.1 (`/login/webauthn`, `/webauthn/authenticate/options`, `/api/v1/register`, `/api/v1/bootstrap`, `/api/v1/invites/accept`).

---

## 4. Authenticated global shell (all screens except welcome/login)

Layout: centered content column `.page-wrap` — `min(1080px, 100% - 2rem)`; wide canvases (tables, video grids) extend to 1280px as `.demo-page-wide`.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ [Brand chip]  Search Chafgram...        [Notif 🔔 5] [Theme ⓘ] [Avatar ▾]  │  ← sticky header
├──────────┬──────────────────────────────────────────────┬──────────────────┤
│ Rail     │  Content column  .page-wrap                  │ Right rail       │
│ Home     │                                              │ Who to follow    │
│ Explore  │  ┌─────────────────────────────────────────┐ │ [See all →]      │
│ Messages │  │ Composer: "What's on your mind?"        │ │ ┌─────────┐      │
│ Notif.   │  │ [Photo] [Video] [Feeling] [Poll] [Post] │ │ avatar · Mey Sokka│
│ Profile  │  │                                         │ │ 12.5K followers│[Follow]│
│ More     │  ┌─────────────────────────────────────────┐ │ ┌─────────┐      │
│          │  │ Post card (island-shell)                │ │ avatar · Dara Chan│
│          │  │  avatar · name · 2h ago · ···           │ │ 8.7K followers │[Follow]│
│          │  │  body text                              │ │ ┌─────────┐      │
│          │  │  [media image]                          │ │ avatar · Vizea│
│          │  │  👍❤️ 248   💬 32   ➤ 12   [bookmark]   │ │ 6.3K followers │[Follow]│
│          │  │  [Like] [Comment] [Share] [Save]        │ │           Trending│
│          │  └─────────────────────────────────────────┘ │ 1 Tech & AI …   │
│          │  ┌─────────────────────────────────────────┐ │ 2 Life in Cambodia│
│          │  │ Post card … (rise-in, staggered)        │ │ 3 Developer Tips │
│          │  └─────────────────────────────────────────┘ │ 4 Study and Career│
│          └─────────────────────────────────────────────┘ │ 5 Random Thoughts│
│  [Create Post] lagoon pill (bottom-left rail)            │ [Join Chafgram]  │
│  "Connect. Share. Belong." kicker                        │   card           │
└──────────┴──────────────────────────────────────────────┴──────────────────┘
```

**Header** (sticky, `design.md` §7):
- Left: brand chip + wordmark "Chafgram" (Manrope 700).
- Center: search input (`.demo-input`, placeholder "Search Chafgram…", focus ring `--lagoon` 24% / 3px).
- Right: notification bell with unread badge (FR-7.2), theme toggle (cycles light→dark→auto, `aria-label` announces mode), member avatar (circular 40px, `--line` ring, initials fallback), chevron dropdown (profile/settings/sign-out).

**Left rail** (nav links, `design.md` §7/#8):
- Home, Explore, Messages (unread badge FR-6.4), Notifications (unread badge FR-7.2), Profile, More.
- Active link: `--sea-ink` + lagoon underline `scaleX`; inactive: `--sea-ink-soft`.
- Touch targets ≥ 44px; at ≤ 640px nav wraps to a second row (existing pattern).
- Bottom of rail: "Create Post" lagoon pill + kicker "Connect. Share. Belong."

**Right rail** (desktop only; collapses/hides at tablet):
- "Who to follow" — kicker + member list (avatar 32px, name, follower count, "Follow" ghost button), "See all →".
- "Trending" — numbered workspace topics with post counts.
- "Join Chafgram today" card (empty-state pattern: kicker + one line + one "Sign Up" lagoon pill). **Note:** private/invite-only default (BR-1.5) — copy confirmed with owner.

---

## 5. Authenticated screens

### 5.1 Home feed (`/`) — default landing

Composer at top of content column, then reverse-chronological following-feed (FR-3.4), cursor pagination.

**Composer card** (`.island-shell`):
- Avatar + "What's on your mind?" (Manrope lead).
- Action chips: [Photo] [Video] [Feeling] [Poll].
- Primary [Post] button (rounded-full lagoon).
- One idea per screen; one primary action (`design.md` §2 #3).

**Post card** (`.island-shell`, hover lift `translateY(-2px)`):
1. Header row: avatar 32px · name (Manrope 600) · `2h ago` (caption, `--sea-ink-soft`) · privacy/globe · more `···`.
2. Body text (optional for media-only posts, FR-3.1; 5,000 char cap, BR-3.3).
3. Media: image or exactly 1 video (BR-3.2 rejects mixed photo+video); placeholder tonal lagoon/sand gradient + centered glyph (`design.md` §11). Short videos (`duration_ms ≤ 60000`) surface in watch (FR-5.3 / BR-5.1).
4. Interaction bar: reactions `👍❤️ 248` · `💬 32 comments` · `➤ 12 shares` · bookmark outline.
5. Action row: [Like] [Comment] [Share] [Save] (Manrope 500, `--sea-ink-soft`, lagoon hover).
6. Edited posts show `edited` marker (FR-3.2). Tombstoned posts invisible everywhere (BR-3.1).

**States:** loading → `sand`/`foam` shimmer skeletons; empty → empty-state pattern; error → `.demo-alert`.

Traceability: FR-3.1, FR-3.2, FR-3.4, FR-3.7, FR-3.8, FR-5.1, FR-5.3, BR-2.4, BR-3.1–BR-3.5.

### 5.2 Explore / Watch (`/explore`)

Tabs (pill segment, chip tokens): `For You` | `Following` | `Trending` | `Nearby`.

Content below:
- Short videos (`duration_ms ≤ 60000`, FR-5.3 / BR-5.1) in vertical watch layout.
- Longer videos + photo posts in regular feed/Watch area.
- Grid: 2×2 on `sm`, 4-up on `lg`; each cell `.island-shell.feature-card` with hover lift + lagoon border-mix (`design.md` §7).
- Each card: media, title overlay, heart count.

Traceability: FR-3.5 (discover feed — proposal), FR-5.3, BR-5.1, BR-12 #2 (discover on/off default).

### 5.3 Notifications (`/notifications`)

List, newest first (FR-7.2), unread count + mark one/all read.

Each item: actor avatar · action text ("liked your post", "commented", "replied", "mentioned you", "sent you a message", "started following you") · target preview · timestamp · read indicator (bold/unread dot).

Types per FR-7.1: new follower, like/reaction, comment on your post, reply to your comment, @mention, direct message. In-app only (BR-7.1 — no email/push). Per-type mute preferences (FR-7.3).

Traceability: FR-7.1, FR-7.2, FR-7.3, BR-7.1–BR-7.3.

### 5.4 Profile (`/profile/:username`)

Header: avatar 56px · name · `@username` · bio (≤ 160, FR-2.1) · stats row (Posts / Followers / Following) · [Edit Profile] (owner only) · share icon.

Tabs: `Posts` | `Replies` | `Media` | `Likes` (active underlined lagoon).

Body: grid/list of own posts per tab.

- Deactivated profiles render "Deactivated member"; suspended stay visible + suspended badge (BR-2.4).
- Blocked members: mutual-effect content hiding (BR-2.2); neither can follow/message (BR-2.1).
- Follow/unfollow any member; view followers/following lists (FR-2.2); block/unblock (FR-2.3); avatar upload via media pipeline (FR-2.4).

Traceability: FR-2.1–FR-2.5, BR-2.1–BR-2.4.

### 5.5 Direct messages — list (`/messages`)

Two-pane on desktop: conversation list (left) + active conversation (right). On mobile, list view that pushes to thread.

Conversation list:
- Avatar/name · last-message preview · unread badge (`last_read_message_id`, FR-6.4) · muted indicator (`muted_until`, FR-6.5).
- At most one direct conversation per unordered user pair (BR-6.1).
- Blocked members cannot message each other (BR-6.2).

Traceability: FR-6.1, FR-6.4, FR-6.5, BR-6.1, BR-6.2.

### 5.6 Direct messages — thread (`/messages/:conversationId`)

Message history, cursor pagination `(conversation_id, id)` (FR-6.3).
- Sender avatar + name + `edited_at` marker (BR-6.4).
- Media messages via `message_media` (FR-6.6).
- Bottom composer: text input + attach image/video.
- No per-message read receipts in v1 — conversation-level unread only (BR-6.6).
- Group conversations: create/title/add/remove, group admin role (FR-6.2); leave/removal (BR-6.3).

Traceability: FR-6.2, FR-6.3, FR-6.4, FR-6.6, BR-6.3–BR-6.6.

### 5.7 Create post (`/compose`) — modal or dedicated route

One idea per screen; single primary action [Post].

- Text body (optional for media-only, FR-3.1; 5,000 char cap, BR-3.3).
- Media picker: [1..N images | 1 video] — mixed photo+video rejected (BR-3.2).
- [Photo] [Video] [Feeling] [Poll] toggles.
- Video upload progress inline (FR-5.2).
- [Cancel] ghost / [Post] lagoon pill (disabled until valid).
- Edited posts: `updated_at` + `edited` marker (FR-3.2).

Traceability: FR-3.1, FR-3.2, FR-5.1, FR-5.2, BR-3.2, BR-3.3.

### 5.8 Admin console

Access: `admin` routes enforce `ROLE_ADMIN`; moderator routes `ROLE_MODERATOR|ROLE_ADMIN` (architecture.md §10). Class-level `@PreAuthorize` on resolvers; auth failures return HTTP 200 + `errors[]` with `extensions.classification` — assert on `$.errors`, not status.

#### 5.8.1 Admin: members (`/admin/members`) — FR-10.1
Table (`.demo-table-shell`, overflow-x; header `chip-bg`; row hover `lagoon` 8%):
- Member list/search/filter · avatar · username · display name · role · status · last activity.
- Role actions: change member/moderator/admin (only `admin` may grant/revoke `admin`, BR-1.4; `moderator` may not change roles).
- Suspend / deactivate / reactivate (FR-1.4, FR-1.5). Deactivated cannot authenticate; suspended can read but not write (FR-1.4).
- Deactivated profiles render "Deactivated member" (BR-2.4).

#### 5.8.2 Admin: workspace settings (`/admin/settings`) — FR-10.1
- Workspace name, slug, logo, primary color, signup mode (`invite_only` default | `open`, FR-1.1).
- Slug/name changes never rewrite member content (BR-1.6).
- `open` never means internet-public signup (BR-1.5).

#### 5.8.3 Admin: audit log (`/admin/audit`) — FR-10.2 / FR-9.3 / FR-9.5
- Filter: actor, action, target type, date range.
- Append-only entries; actor nullable for purged admins; never edited/deleted via app (BR-9.2, BR-11.3).
- Moderator-visible only (BR-9.1).

#### 5.8.4 Admin: reports queue (`/admin/reports`) — FR-9.1, FR-9.2
- List reports (reason: spam|harassment|nsfw|violence|impersonation|other, FR-9.1).
- Take ownership (`reviewing`), action (`actioned`) or dismiss (`dismissed`).
- Rate limit: 1 report per member per target per 24h (BR-9.3, proposal).
- Every state transition emits an audit entry (BR-9.2).

Traceability: FR-1.4, FR-1.5, FR-9.1–FR-9.6, FR-10.1–FR-10.5, BR-1.4–BR-1.6, BR-2.4, BR-9.1–BR-9.5, BR-10.1–BR-10.5, BR-11.3.

### 5.9 Member settings (`/settings`)
- Theme toggle (light/dark/auto, persisted `localStorage["theme"]`, `design.md` §6).
- Passkey management: register several, name each (`label`), revoke individually (FR-1.6).
- Notification preferences (per-type mute, FR-7.3).
- Optional email verification / password reset via single-use tokens (FR-1.7, proposal).

Traceability: FR-1.6, FR-1.7, FR-7.3, design.md §6.

---

## 6. Component & token spec (web)

| Component | Tokens / spec |
|-----------|---------------|
| Shell / card | `.island-shell`: 1px `--line`, `165deg` `--surface-strong`→`--surface`, `--inset-glint`, two-layer shadow, `blur(4px)`, radius `1rem–2rem` |
| Header | `--header-bg` backdrop-blur, hairline `--line` bottom |
| Brand chip | rounded-full, `--chip-bg`/`--chip-line`, dot `linear-gradient(90deg,#56c6be,#7ed3bf)` |
| Nav link | `--sea-ink` / `--sea-ink-soft`; underline 2px lagoon→`#7ed3bf`, `scaleX` 170ms |
| Button primary | rounded-full tonal lagoon `rgba(79,184,178,0.14→0.24)` hover, `--lagoon-deep` text |
| Button secondary/ghost | transparent, `--sea-ink`, hover `--link-bg-hover` |
| Input | `.demo-input`: 0.75rem radius, `--line` border, focus `--lagoon-deep` + 3px `--lagoon` 24% ring |
| Pill / tag | `.demo-pill`: rounded-full, chip tokens, 0.75rem 700 |
| Alert | `.demo-alert`: tinted lagoon/palm/amber/red, 1rem radius |
| Table | `.demo-table-shell` overflow-x; header `chip-bg`; row hover `lagoon` 8% |
| Empty state | kicker + one line + one action |
| Motion | `.rise-in` 700ms, staggered ~90ms; hover 180ms ease; respects `prefers-reduced-motion` |

Typography: Fraunces display only; Manrope body/UI (400/500, buttons 700, kickers 700 uppercase `0.69rem`). Never Fraunces below ~1.25rem; never `#000`/`#fff` text.

---

## 7. Responsive behavior

| Breakpoint | Layout |
|------------|--------|
| Desktop ≥ 1024px | Three-column shell: left rail / content `.page-wrap` / right rail |
| Tablet 641–1023px | Left rail collapses to icons or hides behind menu; right rail hidden; content full width |
| Mobile ≤ 640px | See `mobile-wireframe.md`; nav wraps to second row; header search may tuck into menu |

---

## 8. States (all screens)

| State | Treatment |
|-------|-----------|
| Loading | `sand`/`foam` shimmer skeletons (no spinners; design.md §13) |
| Empty | Empty-state pattern: kicker + one line + one action |
| Error | `.demo-alert` tinted, 1rem radius, path forward (never bare code) |
| Success | Lagoon/palm tinted confirmation; auto-dismiss toast (proposal, design.md §8) |
| Skeleton | `sand`/`foam` shimmer |

---

## 9. Accessibility (web)

- Contrast ≥ 4.5:1 text; large display ≥ 3:1 (`design.md` §10).
- Visible focus: 3px `--lagoon` 24% ring on inputs; `:focus-visible` outline on all interactives — never `outline: none` without replacement.
- Meaningful icons `aria-label`; decorative `aria-hidden`.
- `lang="en"` on `<html>`; theme toggle announces current mode.
- All interactives keyboard-navigable; respects `prefers-reduced-motion` (disable `rise-in`, instant state changes only).
- Touch targets ≥ 44px; color never the only signal (status pills pair tint + text).

---

## 10. Interaction flows (web)

1. **Sign in:** welcome → `/login` → passkey (primary) or password (secondary) → session cookie → `/`.
2. **Create post:** composer → media picker (1..N images \| 1 video) → [Post] → feed insert with `rise-in`.
3. **Follow:** profile → [Follow] ghost → lagoon active; follower count derived (BR-2.3, never stored).
4. **Message:** `/messages` → open conversation → send → history cursor-paginated; unread badge via `last_read_message_id`.
5. **Moderate:** `/admin/reports` → review → action/dismiss → audit entry (append-only).
6. **Admin member action:** `/admin/members` → role/suspend/deactivate → audit entry; deactivated can't auth (FR-1.4).

---

## 11. Open questions (confirm with owner)

1. Final IA: left rail vs top tabs for Feed · Watch · Messages · Notifications (product.md Q1; design.md §13 #1, §14 #1).
2. Right rail ("Who to follow"/"Trending") ships in v1 or behind a flag — discover feed on/off default (BR-12 #2).
3. "Join Chafgram today" card copy: private/invite-only default (BR-1.5) conflicts with public signup framing — confirm copy.
4. Brand mark for Chafgram (current dot is placeholder; design.md §14 #3).
5. Watch: vertical shorts vs 16:9 inline player vs both (design.md §14 #2).
6. Dark-mode watch view: pure `--bg-base` theater or lifted surface (design.md §14 #6).
7. Admin surface scope and which routes are `moderator`-visible vs `admin`-only.

---

## 12. Traceability matrix (web)

| Screen | FR | BR |
|--------|----|----|
| Welcome/Login | FR-1.1, FR-1.3 | BR-1.3, BR-1.5 |
| Home feed | FR-3.1, FR-3.2, FR-3.4, FR-3.7, FR-3.8, FR-5.1, FR-5.3 | BR-2.4, BR-3.1–BR-3.5 |
| Explore/Watch | FR-3.5, FR-5.3 | BR-5.1, BR-12 #2 |
| Notifications | FR-7.1, FR-7.2, FR-7.3 | BR-7.1–BR-7.3 |
| Profile | FR-2.1–FR-2.5 | BR-2.1–BR-2.4 |
| Messages | FR-6.1–FR-6.6 | BR-6.1–BR-6.6 |
| Compose | FR-3.1, FR-3.2, FR-5.1, FR-5.2 | BR-3.2, BR-3.3 |
| Admin | FR-1.4, FR-1.5, FR-9.x, FR-10.x | BR-1.4–BR-1.6, BR-9.x, BR-10.x, BR-11.3 |
| Settings | FR-1.6, FR-1.7, FR-7.3 | design.md §6 |

---

*Related:* [`mobile-wireframe.md`](mobile-wireframe.md) · [`design.md`](design.md) · [`product.md`](../product.md) · [`docs/functional-requirements.md`](functional-requirements.md) · [`docs/business-rules.md`](business-rules.md) · [`docs/architecture.md`](architecture.md)

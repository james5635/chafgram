# Chafgram — Mobile Wireframe

> Complete wireframe specification for the Chafgram member-facing mobile web app (PWA /
> responsive, single-column). Grounded in [`design.md`](../design.md),
> [`product.md`](../product.md), [`docs/functional-requirements.md`](functional-requirements.md)
> (FR), [`docs/business-rules.md`](business-rules.md) (BR), [`docs/architecture.md`](architecture.md)
> (API surface), and the reference UI example (`.tmp/mobile-ui.webp`).
>
> The reference shows four phone states (welcome, feed, explore, profile) with a bottom tab
> bar (Home / Explore / + / Messages / Profile) and a story row; this doc re-specifies that
> structure — plus every other screen — in Chafgram's lagoon/palm design language.
>
> Mobile is responsive web / PWA only — no native companions (architecture.md §11).
> Status: design proposal. Confirm IA, bottom-tab vs top-tabs on larger mobile, and admin
> surface scope before implementing (product.md open questions 1, 8; design.md §13/#14; BR-12).

---

## 1. Design system alignment

Every screen uses the `design.md` token set — CSS custom properties, never raw hex. Key
tokens:

| Role | Light token | Dark token | Use |
|------|-------------|------------|-----|
| Primary text | `--sea-ink` `#173a40` | `--sea-ink` `#d7ece8` | Body, headings, labels |
| Secondary text | `--sea-ink-soft` `#416166` | `--sea-ink-soft` `#afcdc8` | Captions, metadata, inactive tabs |
| Accent | `--lagoon` `#4fb8b2` | `--lagoon` `#60d7cf` | Links, focus, primary CTAs, active tab |
| Accent strong | `--lagoon-deep` `#328f97` | `--lagoon-deep` `#8de5db` | Active tab text, button text |
| Brand kicker | `--palm` `#2f6a4a` | `--palm` `#6ec89a` | Eyebrow text, brand gradient |
| Base bg | `--bg-base` `#e7f3ec` | `--bg-base` `#0a1418` | Page background |
| Card fill | `--surface` `rgba(255,255,255,0.74)` | `--surface` `rgba(16,30,34,0.8)` | `.island-shell` cards |
| Card fill strong | `--surface-strong` `rgba(255,255,255,0.9)` | `--surface-strong` `rgba(15,27,31,0.92)` | Elevated cards |
| Hairline | `--line` `rgba(23,58,64,0.14)` | `--line` `rgba(141,229,219,0.18)` | Dividers, input borders |
| Header bg | `--header-bg` `rgba(251,255,248,0.84)` | `--header-bg` `rgba(10,20,24,0.8)` | Sticky header (backdrop-blur) |
| Chip | `--chip-bg` / `--chip-line` | `--chip-bg` / `--chip-line` | Pills, tab segment |

Component primitives (from `design.md` §7–§9):

- **`.island-shell`** — 1px `--line`, `165deg` gradient `--surface-strong`→`--surface`,
  inner `--inset-glint` top highlight, two-layer soft shadow, `blur(4px)`, radius `0.9–1rem`.
- **Header** — `--header-bg` backdrop-blur, hairline `--line` bottom border.
- **Bottom tab bar** — fixed, five destinations, 44px+ touch targets (`design.md` §10),
  safe-area inset (notch / home indicator). Active tab `--lagoon` icon + label; inactive
  `--sea-ink-soft`.
- **Center FAB `+`** — rounded-full lagoon fill, `--lagoon-deep` text, 44px+ target,
  opens create-post sheet.
- **Tab pills** — chip tokens for For You / Following / Trending / Nearby.
- **Button primary** — rounded-full tonal lagoon `rgba(79,184,178,0.14→0.24)` hover,
  `--lagoon-deep` text.
- **Button secondary/ghost** — transparent, `--sea-ink`.
- **Input** — `.demo-input`: 0.75rem radius, `--line` border, focus `--lagoon-deep` border +
  3px `--lagoon` 24% ring.
- **Pill / tag** — `.demo-pill`: rounded-full, chip tokens, 0.75rem 700.
- **Alert** — `.demo-alert`: tinted (lagoon info, palm/success, amber warning, red danger),
  1rem radius, never a bare code — always a path forward.
- **Empty state** — kicker + one line + one action; never a dead end.
- **Skeleton** — `sand`/`foam` shimmer (no spinners; `design.md` §13).
- **Motion** — `.rise-in` 700ms `cubic-bezier(0.16,1,0.3,1)`, `translateY(12px)` + fade,
  staggered ~90ms per sibling; hover/press 180ms ease; respects `prefers-reduced-motion`.

Typography: **Fraunces** (display/h1 only, `line-height ≈ 1.02–1.05`, never below ~1.25rem),
**Manrope** (body 400/500, buttons 700, kickers 700 uppercase `0.16em` tracking `0.69rem`).
Never pure `#000`/`#fff` text.

---

## 2. Screen inventory

| Path | Auth | Screen | Primary action |
|------|------|--------|----------------|
| `/welcome` | No | Welcome / landing | Sign in or contact admin |
| `/login` | No | Login | Passkey-first sign in; password fallback |
| `/` | Yes | Home feed | Compose post via FAB; scroll following-feed |
| `/explore` | Yes | Explore / Watch | Browse For You / Following / Trending / Nearby |
| `/notifications` | Yes | Notifications | Read + mark activity read |
| `/profile` | Yes | Profile | View own posts / replies / media / likes; edit |
| `/messages` | Yes | Direct messages (list) | Open a conversation |
| `/messages/:conversationId` | Yes | Direct messages (thread) | Read/send messages |
| `/compose` | Yes | Create post (bottom sheet) | Photo/video/text post; one primary "Post" |
| `/admin/*` | Admin/Mod | Admin surfaces | Members, settings, audit, reports (mobile-dense) |
| `/settings` | Yes | Member settings | Theme, passkeys, notification prefs |

Roles: `member`, `moderator`, `admin`. One workspace per deployment (BR-1.1).

---

## 3. Global shell (mobile)

```
┌───────────────────────────────────────────────┐
│ [Back ←]   Title / Search      [Notif 🔔] [ⓘ] │  ← header (--header-bg, --line bottom)
├───────────────────────────────────────────────┤
│                                               │
│             <screen content>                  │
│             single column                     │
│                                               │
│                                               │
├───────────────────────────────────────────────┤
│  Home  Explore   [ + ]   Messages   Profile   │  ← bottom tab bar (fixed, 44px+)
└───────────────────────────────────────────────┘
```

**Header:** left back/chevron (or brand chip + wordmark on home), center title or search,
right notification bell with unread badge (FR-7.2) + theme/overflow. Background
`--header-bg` with backdrop-blur, hairline `--line` bottom.

**Bottom tab bar** (fixed, five destinations):
- `Home` (house), `Explore` (compass), `+` create (center, elevated lagoon FAB),
  `Messages` (speech bubble, unread badge FR-6.4), `Profile` (person).
- Active tab: `--lagoon` icon + label; inactive: `--sea-ink-soft`.
- Center `+` FAB: rounded-full lagoon, opens `/compose` bottom sheet (full-screen or
  bottom sheet).
- Bottom-safe-area: respects mobile safe areas (notch / home indicator) as in the
  reference frames.

**Tablet (641–1023px):** same single-column pattern but wider content; may expose the web
left rail or keep bottom tabs — confirm IA with owner (product.md Q1; design.md §14 #1).

---

## 4. Unauthenticated screens

### 4.1 Welcome (`/welcome`)

Single column, full viewport height, safe-area insets. Centered. Matches reference frame 1.

```
┌───────────────────────────────────────────────┐
│  welcome card (centered, island-shell)        │
│                                               │
│   [Brand chip · Chafgram]                     │
│                                               │
│   kicker: "Chafgram"                          │
│   h1: "Your company's social space"  Fraunces │
│   lead: "Connect. Share. Belong. — inside    │
│          your workspace, on your own servers."│
│                                               │
│   ambient illustration: tonal lagoon/sand     │
│   gradient + speech-bubble glyph (never stock │
│   photography; design.md §11)                 │
│                                               │
│   [ Sign in ]                          lagoon │
│   [ Contact your admin ]                   ghost│
│                                               │
│   "New to Chafgram? Your admin creates your    │
│    account via invite."                       │
│                                               │
└───────────────────────────────────────────────┘
```

**Behavior:**
- [Sign in] → `/login`.
- [Contact your admin] → info alert: signup is `invite_only` by default (BR-1.5); invites accepted at `POST /api/v1/invites/accept`.
- No public "Create account" unless `signup_mode=open`.
- Fresh install: bootstrap (`POST /api/v1/bootstrap`, first admin only, BR-1.3) is a separate flow.

Traceability: FR-1.1, FR-1.3, BR-1.3, BR-1.5.

### 4.2 Login (`/login`)

Single column, full viewport height, safe-area insets. No bottom tab bar (unauthenticated).

```
┌───────────────────────────────────────────────┐
│ [Back ←]                       [Theme ⓘ]      │  ← minimal header
├───────────────────────────────────────────────┤
│                                               │
│        login card (island-shell, centered)    │
│                                               │
│   kicker: "Sign in"                           │
│   h1: Welcome back                       Fraunces│
│                                               │
│   [ Sign in with passkey 🔑 ]        primary   │
│   → WebAuthn: /webauthn/authenticate/options   │
│       → /login/webauthn → session → /          │
│                                               │
│   ──── or ────                                │
│                                               │
│   [ Sign in with password ]        secondary   │
│   ▼ progressive disclose                      │
│   email/username input  (.demo-input)         │
│   password input        (.demo-input)         │
│   [ Continue ]                         lagoon  │
│                                               │
│   Don't have an account?  Contact your admin  │
│   [ Need help? ]                              │
│                                               │
│   .demo-alert (tinted) — errors w/ path forward│
│                                               │
├───────────────────────────────────────────────┤
│                                 [Need help?]  │  ← footer
└───────────────────────────────────────────────┘
```

**Flow:**
1. Passkey (primary, lagoon tonal pill, 🔑 icon, 44px+ touch target) → `navigator.credentials.get()`
   → `/login/webauthn` → session cookie → redirect `/`. Passkey-first per FR-1.3.
2. Password (secondary ghost) → email/username + password inputs (`.demo-input`, focus ring
   `--lagoon` 24% / 3px) → [Continue] lagoon button → session cookie → `/`. Optional password
   fallback per member (FR-1.3).
3. Errors: `.demo-alert`, tinted lagoon info / red danger, 1rem radius, always a path forward
   (`design.md` §3).
4. A11y: `aria-label` on all buttons; `lang="en"`; visible focus ring; respects
   `prefers-reduced-motion`; connects to welcome screen's "Sign In" ghost button (§4.1).

Traceability: FR-1.3, BR-1.3, BR-1.5, architecture.md §4.1.

---

## 5. Authenticated screens

### 5.1 Home feed (`/`) — default landing

Single column, `rise-in` entrance staggered ~90ms per post (`design.md` §9).

**Story row** (horizontal scroll, matches reference frame 2):
- [+] Create Story chip (outlined, `--line` border).
- Story avatars: "You", then followed members, circular 56px with `--line` ring, status ring
  when unwatched.

**Composer** (`.island-shell`):
- Avatar + "What's on your mind?" (Manrope lead).
- Action chips: [Photo] [Video] [Feeling] [Poll].
- Primary [Post] button at right (rounded-full lagoon).
- One idea per screen; one primary action (`design.md` §2 #3).

**Post cards** (full-width `.island-shell`, `--line` hairline separators):
1. Avatar 32px · name · `2h ago` · more `···`.
2. Body text (optional for media-only, FR-3.1; 5,000 char cap, BR-3.3).
3. Media: image or exactly 1 video (BR-3.2 rejects mixed photo+video); placeholder tonal
   lagoon/sand gradient + centered glyph (`design.md` §11). Short videos
   (`duration_ms ≤ 60000`) surface in watch (FR-5.3 / BR-5.1).
4. Reaction row: `👍❤️ 248` · `💬 72` · share · bookmark.
5. Secondary actions: [Like] [Comment] [Share] [Save].

- Edited posts show `edited` marker (FR-3.2). Tombstoned posts invisible everywhere (BR-3.1).
- Deactivated profiles render "Deactivated member"; suspended badged (BR-2.4).

**States:** loading → `sand`/`foam` shimmer skeletons; empty → empty-state pattern; error →
`.demo-alert`.

Traceability: FR-3.1, FR-3.2, FR-3.4, FR-3.7, FR-3.8, FR-5.1, FR-5.3, BR-2.4, BR-3.1–BR-3.5.

### 5.2 Explore / Watch (`/explore`)

Matches reference frame 3:

1. Header "Explore" + search icon.
2. Tab pills (chip tokens): `For You` (active, lagoon fill) | `Following` | `Trending` | `Nearby`.
3. Content grid: 2 columns on mobile. Short-video cards (`duration_ms ≤ 60000`, FR-5.3 /
   BR-5.1) plus photo posts.
4. Each card: `.island-shell`, media, title overlay ("Better Together", "Good Morning"),
   heart count (12.5K / 24.1K / 11.3K / 15.6K), hover lift.

Traceability: FR-3.5 (discover feed — proposal), FR-5.3, BR-5.1, BR-12 #2.

### 5.3 Notifications (`/notifications`)

List, newest first (FR-7.2), unread count + mark one/all read.

Each item: actor avatar · action text ("liked your post", "commented on your post", "replied
to your comment", "mentioned you", "sent you a message", "started following you") · target
preview · timestamp · read indicator (bold / unread dot).

Types per FR-7.1. In-app only (BR-7.1 — no email/push). Per-type mute preferences (FR-7.3).

Traceability: FR-7.1, FR-7.2, FR-7.3, BR-7.1–BR-7.3.

### 5.4 Profile (`/profile`)

Matches reference frame 4:

1. Header avatar 56px · name "Rojame" · `@rojame`.
2. Stats row: `128 Posts` · `2.4K Followers` · `320 Following`.
3. [Edit Profile] button (owner only) + share icon.
4. Bio (≤ 160, FR-2.1): "Software Engineer & Tech Enthusiast 🚀 · Always exploring new things."
5. Tabs: `Posts` | `Replies` | `Media` | `Likes` (active underlined lagoon).
6. Body: grid of own posts per tab.

- Deactivated → "Deactivated member"; suspended → visible + suspended badge (BR-2.4).
- Blocked: mutual-effect content hiding (BR-2.2); neither can follow/message (BR-2.1).
- Follow/unfollow (FR-2.2); block/unblock (FR-2.3); avatar upload via media pipeline (FR-2.4).

Traceability: FR-2.1–FR-2.5, BR-2.1–BR-2.4.

### 5.5 Direct messages — list (`/messages`)

Conversation list (full screen on mobile):
- Avatar/name · last-message preview · unread badge (`last_read_message_id`, FR-6.4) ·
  muted indicator (`muted_until`, FR-6.5).
- At most one direct conversation per unordered user pair (BR-6.1).
- Blocked members cannot message each other (BR-6.2).
- Tapping pushes to conversation thread.

Traceability: FR-6.1, FR-6.4, FR-6.5, BR-6.1, BR-6.2.

### 5.6 Direct messages — thread (`/messages/:conversationId`)

Message history, cursor pagination `(conversation_id, id)` (FR-6.3).
- Sender avatar + name + `edited_at` marker (BR-6.4).
- Media messages via `message_media` (FR-6.6).
- Bottom composer: text input + attach image/video.
- No per-message read receipts in v1 — conversation-level unread only (BR-6.6).
- Group conversations: create/title/add/remove, group admin role (FR-6.2); leave/removal
  (BR-6.3).

Traceability: FR-6.2, FR-6.3, FR-6.4, FR-6.6, BR-6.3–BR-6.6.

### 5.7 Create post (`/compose`) — bottom sheet or full screen

One idea per screen; single primary action [Post].

- Text body (optional for media-only, FR-3.1; 5,000 char cap, BR-3.3).
- Media picker: [1..N images | 1 video] — mixed photo+video rejected (BR-3.2).
- [Photo] [Video] [Feeling] [Poll] toggles.
- Video upload progress inline (FR-5.2).
- [Cancel] ghost / [Post] lagoon pill (disabled until valid).
- Edited posts: `updated_at` + `edited` marker (FR-3.2).

Traceability: FR-3.1, FR-3.2, FR-5.1, FR-5.2, BR-3.2, BR-3.3.

### 5.8 Admin surfaces (mobile-dense)

Access: `admin` routes enforce `ROLE_ADMIN`; moderator routes `ROLE_MODERATOR|ROLE_ADMIN`
(architecture.md §10). Auth failures return HTTP 200 + `errors[]` with
`extensions.classification` — assert on `$.errors`, not status.

On mobile, admin surfaces use the same single-column shell with denser lists (table patterns
from `design.md` §8, `.demo-table-shell`).

- **Members (`/admin/members`) — FR-10.1:** list/search/filter · avatar · username · role ·
  status · last activity. Role change (only `admin` grants/revokes `admin`, BR-1.4;
  `moderator` may not change roles). Suspend / deactivate / reactivate (FR-1.4, FR-1.5).
- **Workspace settings (`/admin/settings`) — FR-10.1:** name, slug, logo, primary color,
  signup mode (`invite_only` default \| `open`, FR-1.1). Slug/name changes never rewrite
  member content (BR-1.6). `open` never means internet-public (BR-1.5).
- **Audit log (`/admin/audit`) — FR-10.2 / FR-9.3:** filter actor/action/target/date;
  append-only; moderator-visible only (BR-9.1, BR-9.2, BR-11.3).
- **Reports queue (`/admin/reports`) — FR-9.1, FR-9.2:** list reports (reason:
  spam|harassment|nsfw|violence|impersonation|other); take ownership (`reviewing`), action
  (`actioned`) or dismiss (`dismissed`); rate limit 1 per member per target per 24h
  (BR-9.3, proposal).

Traceability: FR-1.4, FR-1.5, FR-9.1–FR-9.6, FR-10.1–FR-10.5, BR-1.4–BR-1.6, BR-2.4,
BR-9.1–BR-9.5, BR-10.1–BR-10.5, BR-11.3.

### 5.9 Member settings (`/settings`)

- Theme toggle (light/dark/auto, persisted `localStorage["theme"]`, `design.md` §6).
- Passkey management: register several, name each (`label`), revoke individually (FR-1.6).
- Notification preferences (per-type mute, FR-7.3).
- Optional email verification / password reset via single-use tokens (FR-1.7, proposal).

Traceability: FR-1.6, FR-1.7, FR-7.3, design.md §6.

---

## 6. Component & token spec (mobile)

| Component | Tokens / spec |
|-----------|---------------|
| Card | `.island-shell`: 1px `--line`, `165deg` `--surface-strong`→`--surface`, `--inset-glint`, two-layer shadow, `blur(4px)`, radius `0.9–1rem` |
| Header | `--header-bg` backdrop-blur, hairline `--line` bottom |
| Tab bar active/inactive | `--lagoon` / `--sea-ink-soft`; fixed bottom, safe-area inset |
| FAB / primary button | rounded-full lagoon fill, `--lagoon-deep` text, 44px+ target |
| Secondary/ghost | transparent, `--sea-ink` |
| Input | `.demo-input`: 0.75rem radius, `--line` border, focus `--lagoon-deep` + 3px `--lagoon` 24% ring |
| Pill / tag | `.demo-pill`: rounded-full, chip tokens, 0.75rem 700 |
| Alert | `.demo-alert`: tinted lagoon/palm/amber/red, 1rem radius |
| Empty state | kicker + one line + one action |
| Motion | `.rise-in` 700ms, staggered ~90ms; hover/press 180ms ease; respects `prefers-reduced-motion` |

Typography: Fraunces display only; Manrope body/UI (400/500, buttons 700, kickers 700
uppercase `0.69rem`). Never Fraunces below ~1.25rem; never `#000`/`#fff` text.

---

## 7. Responsive / adaptive behavior

| Breakpoint | Layout |
|------------|--------|
| Mobile ≤ 640px | Single column, bottom tab bar (Home/Explore/+/Messages/Profile), header search may tuck into menu, story row horizontally scrollable |
| Tablet 641–1023px | Same single-column pattern but wider content; may expose web left rail or keep bottom tabs — confirm IA with owner (product.md Q1; design.md §14 #1) |
| Dark mode | Re-lit scene (design.md §6); tested in light/dark/auto; watch view may use pure `--bg-base` theater or lifted surface (design.md §14 #6) |

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

## 9. Accessibility (mobile)

- Contrast ≥ 4.5:1; touch targets ≥ 44px (`design.md` §10); bottom tab bar items sized
  accordingly.
- Visible focus: 3px `--lagoon` 24% ring; `:focus-visible` on all interactives.
- Meaningful icons `aria-label`; decorative `aria-hidden`; `lang="en"`.
- Keyboard-navigable everything; theme toggle announces mode; reduced-motion honored.

---

## 10. Interaction flows (mobile)

1. **Sign in:** welcome → `/login` → passkey (primary) or password (secondary) → session cookie → `/`.
2. **Create post:** FAB `+` → `/compose` sheet → media picker (1..N images \| 1 video) → [Post] → feed insert with `rise-in`.
3. **Follow:** profile → [Follow] ghost → lagoon active; follower count derived (BR-2.3, never stored).
4. **Message:** `/messages` → open conversation → send → history cursor-paginated; unread badge via `last_read_message_id`.
5. **Moderate:** `/admin/reports` → review → action/dismiss → audit entry (append-only).
6. **Admin member action:** `/admin/members` → role/suspend/deactivate → audit entry; deactivated can't auth (FR-1.4).

---

## 11. Open questions (confirm with owner)

1. IA: bottom tab bar (Home/Explore/+/Messages/Profile) vs top-tab or rail pattern on larger mobile (product.md Q1; design.md §13 #1, §14 #1).
2. Watch: vertical shorts vs 16:9 inline player vs both (design.md §14 #2).
3. Discover/"Trending" on or off by default for new workspaces (BR-12 #2).
4. Brand mark + favicon/app icons (design.md §14 #3).
5. Welcome screen: direct "open" signup or strictly invite acceptance (BR-1.5 — `open` never means internet-public).
6. Admin surface scope on mobile and which routes are `moderator`-visible vs `admin`-only.

---

## 12. Traceability matrix (mobile)

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

*Related:* [`web-wireframe.md`](web-wireframe.md) · [`design.md`](design.md) · [`product.md`](../product.md) · [`docs/functional-requirements.md`](functional-requirements.md) · [`docs/business-rules.md`](business-rules.md) · [`docs/architecture.md`](architecture.md)

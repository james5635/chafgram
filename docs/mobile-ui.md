# Chafgram — Mobile UI Specification

> Complete UI specification for the Chafgram member-facing mobile web app (PWA /
> responsive, single-column). This doc is the single source of truth for **what** each UI
> component is, **where** it appears, and **how** it is built (tokens, markup structure,
> variants, states).
>
> It comprises two parts:
> 1. **Wireframe** — screen layouts and assembly (§2–§10).
> 2. **Component spec** — what/where/how for every component type (§11–§14).
>
> Grounded in [`design.md`](design.md) (tokens, components, motion, a11y), the reference UI
> example (`.tmp/mobile-ui.webp`), [`docs/functional-requirements.md`](functional-requirements.md)
> (FR), [`docs/business-rules.md`](business-rules.md) (BR), and [`docs/architecture.md`](architecture.md)
> (API surface).
>
> Implemented in `frontend/src/` (TanStack Start + Tailwind v4). UI only — no backend
> connection yet (mock data). Brand is blue (`#2563eb`) per the reference.
>
> Mobile is responsive web / PWA only — no native companions (architecture.md §11).
> Status: design proposal. Confirm IA, bottom-tab vs top-tabs on larger mobile, and admin
> scope with the owner before finalising (§15).

---

## 1. Design tokens (mobile)

Same token set as `web-ui.md` §1 (lagoon/palm/sea-ink/sand/foam plus brand blue
`#2563eb`). Use tokens, never raw hex. Touch targets ≥ 44px (`design.md` §10).

---

## 2. Screen inventory (mobile)

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

Single column, full viewport height, safe-area insets (notch / home indicator).

```
┌───────────────────────────────────────────────┐
│  Header: [Back ←]  Title / Search  [Notif 🔔] [ⓘ] │  ← header (--header-bg, --line bottom)
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

**Bottom tab bar** (fixed, five destinations, 44px+ touch targets):
- `Home` (house), `Explore` (compass), `+` create (center, elevated brand FAB),
  `Messages` (speech bubble, unread badge FR-6.4), `Profile` (person).
- Active tab: `--brand` icon + label; inactive: `--sea-ink-soft`.
- Center `+` FAB: rounded-full brand fill, `--brand-dark` text, opens `/compose` bottom
  sheet (full-screen or bottom sheet).
- Bottom-safe-area: respects mobile safe areas (notch / home indicator) as in the
  reference frames.

**Tablet (641–1023px):** same single-column pattern but wider content; may expose the web
left rail or keep bottom tabs — confirm IA with owner (product.md Q1; design.md §13 #1).

---

## 4. Unauthenticated screens

### 4.1 Welcome (`/welcome`)

Single column, full viewport height, safe-area insets. Centered. Matches reference frame 1.

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Brand chip + wordmark | Top center | inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow; dot `h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]`; wordmark `font-bold tracking-tight` |
| Kicker "Private social for your company" | Below brand | `island-kicker` |
| h1 "Connect. Share. Belong." | Below kicker | `display-title` (Fraunces), `text-4xl font-bold text-[var(--sea-ink)] leading-[1.02] sm:text-6xl` |
| Lead paragraph | Below h1 | `max-w-lg text-base leading-8 text-[var(--sea-ink-soft)] sm:text-lg`, `m-0` |
| Illustration | Below lead | `relative h-64 sm:h-80 rounded-xl overflow-hidden border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)80%,white_20%)]` with radial-gradient overlay + centered speech-bubble glyph |
| [Sign in] | Primary CTA | `flex-1 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] transition hover:bg-[color-mix(in_oklab,var(--brand)30%,var(--surface-strong))] sm:flex-none sm:w-48` → `/login` |
| [Contact your admin] | Secondary | `flex-1 rounded-full border border-[var(--line)] bg-transparent px-6 py-3 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)] sm:flex-none sm:w-48` → `/welcome` |
| Footer note | Bottom | `text-[0.75rem] text-[var(--sea-ink-soft)]`: "New to Chafgram? Your admin creates your account via invite. Signup is invite-only by default." |

**Flow:** [Sign in] → `/login`. [Contact your admin] → info alert: signup is `invite_only` by default (BR-1.5); accepts at `POST /api/v1/invites/accept`. No public "Create account" unless `signup_mode=open`.

Traceability: FR-1.1, FR-1.3, BR-1.3, BR-1.5.

### 4.2 Login (`/login`)

Single column, full viewport height, safe-area insets. No bottom tab bar (unauthenticated).

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Header | Top | `[Back ←]` left, `[Theme ⓘ]` right; minimal |
| Kicker "Sign in" | Card top | `island-kicker` |
| h1 "Welcome back" | Below kicker | `display-title` (Fraunces) |
| [Sign in with passkey 🔑] | Primary | `w-full rounded-full border px-6 py-3 text-sm font-semibold transition`; active `border-[var(--chip-line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] text-[var(--sea-ink)]`; inactive `border-transparent bg-transparent text-[var(--sea-ink-soft)] hover:bg-[var(--link-bg-hover)]` |
| ──── or ──── | Divider | `mx-2 mt-1 mb-1 h-[1px] bg-[var(--line)]` |
| [Sign in with password] | Secondary | Same pill styling → progressive disclose inputs |
| Email/username input | Password mode | `.demo-input`: `w-full border border-[var(--line)] rounded-full bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[color-mix(in_oklab,var(--brand)58%,var(--line))] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--brand)24%,transparent)]` |
| Password input | Below email | Same `.demo-input`, `type="password"` |
| [Continue] | Below inputs | `w-full rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] transition disabled:opacity-55 disabled:transform-none hover:!bg-[color-mix(in_oklab,var(--brand)30%,var(--surface-strong))]` |
| "Don't have an account? Contact your admin" | Below mode toggle | `mb-3 text-sm text-[var(--sea-ink-soft)]` + `font-semibold` "Contact your admin" |
| [← Back to welcome] | Link | `text-sm font-semibold text-[var(--brand-dark)] no-underline transition hover:text-[var(--brand)]` → `/welcome` |
| `.demo-alert` | Error state | tinted red, never a bare code — always a path forward |
| [Need help?] | Footer area | link |

**Flow:** Passkey (primary) → `/webauthn/authenticate/options` → `navigator.credentials.get()` → `/login/webauthn` → session cookie → redirect `/`. Password (secondary) → email/password inputs → [Continue] → session cookie → `/`.

Traceability: FR-1.3, BR-1.3, BR-1.5, architecture.md §4.1.

---

## 5. Authenticated screens

### 5.1 Home feed (`/`) — default landing

Single column, `rise-in` entrance staggered ~90ms per post (`design.md` §9).

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Story row | Top, horizontal scroll | `flex gap-2 overflow-x-auto pb-2 mb-6 no-scrollbar`: [+] Create Story chip (outlined, `--line` border) + story avatars circular 56px with `--line` ring, status ring when unwatched |
| Composer | Below story row | `.island-shell`: avatar + "What's on your mind?" (Manrope lead) + chips [Photo][Video][Feeling][Poll] + primary [Post] button at right (rounded-full brand) |
| Post cards | Full-width, `.island-shell`, `--line` hairline separators | avatar 32px · name · `2h ago` · more `···`; body text (optional for media-only, FR-3.1; 5,000 char cap, BR-3.3); media (image or exactly 1 video, BR-3.2); placeholder tonal blue/sand gradient + glyph; reaction row `👍❤️ 248` · `💬 72` · share · bookmark; secondary actions [Like] [Comment] [Share] [Save] |
| Edited marker | Post body | `edited` shown (FR-3.2) |
| Tombstone | Post | Invisible everywhere (BR-3.1) |
| Deactivated/suspended | Profile | "Deactivated member" / suspended badge (BR-2.4) |

**States:** loading → `sand`/`foam` shimmer skeletons; empty → empty-state pattern; error → `.demo-alert`.

Traceability: FR-3.1, FR-3.2, FR-3.4, FR-3.7, FR-3.8, FR-5.1, FR-5.3, BR-2.4, BR-3.1–BR-3.5.

### 5.2 Explore / Watch (`/explore`)

Matches reference frame 3:

| Component | Where | How |
|-----------|-------|-----|
| Header "Explore" + search | Top | h1 + search icon |
| Tab pills | Below header | chip tokens: `For You` (active, brand fill) | `Following` | `Trending` | `Nearby` |
| Content grid | Below tabs | 2 columns on mobile; short-video cards (`duration_ms ≤ 60000`, FR-5.3 / BR-5.1) plus photo posts |
| Card | Each item | `.island-shell`, media, title overlay ("Better Together", "Good Morning"), heart count (12.5K / 24.1K / 11.3K / 15.6K), hover lift |

Traceability: FR-3.5 (discover feed — proposal), FR-5.3, BR-5.1, BR-12 #2.

### 5.3 Notifications (`/notifications`)

List, newest first (FR-7.2), unread count + mark one/all read.

Each item: actor avatar · action text ("liked your post", "commented on your post", "replied to your comment", "mentioned you", "sent you a message", "started following you") · target preview · timestamp · read indicator (bold / unread dot).

Types per FR-7.1. In-app only (BR-7.1 — no email/push). Per-type mute preferences (FR-7.3).

Traceability: FR-7.1, FR-7.2, FR-7.3, BR-7.1–BR-7.3.

### 5.4 Profile (`/profile`)

Matches reference frame 4:

| Component | Where | How |
|-----------|-------|-----|
| Header avatar | Top | 56px · name "Rojame" · `@rojame` |
| Stats row | Below avatar | `128 Posts` · `2.4K Followers` · `320 Following` |
| [Edit Profile] | Stats row | Button (owner only) + share icon |
| Bio | Below stats | ≤ 160 (FR-2.1): "Software Engineer & Tech Enthusiast 🚀 · Always exploring new things." |
| Tabs | Below bio | `Posts` | `Replies` | `Media` | `Likes` (active underlined brand) |
| Body | Below tabs | Grid of own posts per tab |

Deactivated → "Deactivated member"; suspended → visible + suspended badge (BR-2.4). Blocked: mutual-effect hiding (BR-2.2); neither can follow/message (BR-2.1). Follow/unfollow (FR-2.2); block/unblock (FR-2.3); avatar upload via media pipeline (FR-2.4).

Traceability: FR-2.1–FR-2.5, BR-2.1–BR-2.4.

### 5.5 Direct messages — list (`/messages`)

Conversation list (full screen on mobile):

| Component | Where | How |
|-----------|-------|-----|
| Header | Top | "Messages" + search/new-conversation icon |
| List items | Full screen | Avatar/name · last-message preview · unread badge (`last_read_message_id`, FR-6.4) · muted indicator (`muted_until`, FR-6.5) |
| Tapping | Pushes | To conversation thread |

At most one direct conversation per unordered user pair (BR-6.1). Blocked members cannot message each other (BR-6.2).

Traceability: FR-6.1, FR-6.4, FR-6.5, BR-6.1, BR-6.2.

### 5.6 Direct messages — thread (`/messages/:conversationId`)

| Component | Where | How |
|-----------|-------|-----|
| Header | Top | Conversation name + back |
| Message history | Main | Cursor pagination `(conversation_id, id)` (FR-6.3); sender name + `edited_at` (BR-6.4); media messages via `message_media` (FR-6.6) |
| Bottom composer | Fixed | Text input + attach image/video |

No per-message read receipts in v1 — conversation-level unread only (BR-6.6). Group conversations: create/title/add/remove, group admin role (FR-6.2); leave/removal (BR-6.3).

Traceability: FR-6.2, FR-6.3, FR-6.4, FR-6.6, BR-6.3–BR-6.6.

### 5.7 Create post (`/compose`) — bottom sheet or full screen

One idea per screen; single primary action [Post].

| Component | Where | How |
|-----------|-------|-----|
| Text body | Top | Optional for media-only (FR-3.1); 5,000 char cap (BR-3.3) |
| Media picker | Below body | [1..N images \| 1 video] — mixed photo+video rejected (BR-3.2) |
| Toggles | Below picker | [Photo] [Video] [Feeling] [Poll] |
| Upload progress | Inline | Video upload progress (FR-5.2) |
| Actions | Bottom | [Cancel] ghost / [Post] brand pill (disabled until valid) |

Traceability: FR-3.1, FR-3.2, FR-5.1, FR-5.2, BR-3.2, BR-3.3.

### 5.8 Admin surfaces (mobile-dense)

Access: `admin` routes enforce `ROLE_ADMIN`; moderator routes `ROLE_MODERATOR|ROLE_ADMIN`
(architecture.md §10). Auth failures return HTTP 200 + `errors[]` with
`extensions.classification` — assert on `$.errors`, not status.

On mobile, admin surfaces use the same single-column shell with denser lists (table patterns
from `design.md` §8, `.demo-table-shell`).

| Sub-screen | Components |
|------------|------------|
| Members | `/admin/members`: list/search/filter · avatar · username · role · status · last activity. Role change (only `admin` grants/revokes `admin`, BR-1.4; `moderator` may not change roles). Suspend / deactivate / reactivate (FR-1.4, FR-1.5). |
| Workspace settings | `/admin/settings`: name, slug, logo, primary color, signup mode (`invite_only` default \| `open`, FR-1.1). Slug/name changes never rewrite member content (BR-1.6). `open` never means internet-public (BR-1.5). |
| Audit log | `/admin/audit`: filter actor/action/target/date; append-only; moderator-visible only (BR-9.1, BR-9.2, BR-11.3). |
| Reports queue | `/admin/reports`: list reports (reason: spam\|harassment\|nsfw\|violence\|impersonation\|other); take ownership (`reviewing`), action (`actioned`) or dismiss (`dismissed`); rate limit 1 per member per target per 24h (BR-9.3, proposal). |

Traceability: FR-1.4, FR-1.5, FR-9.1–FR-9.6, FR-10.1–FR-10.5, BR-1.4–BR-1.6, BR-2.4, BR-9.1–BR-9.5, BR-10.1–BR-10.5, BR-11.3.

### 5.9 Member settings (`/settings`)

| Component | Where | How |
|-----------|-------|-----|
| Theme toggle | Top | light/dark/auto, persisted `localStorage["theme"]`, `design.md` §6 |
| Passkeys | Below theme | Register several, name each (`label`), revoke individually (FR-1.6) |
| Notification preferences | Below passkeys | Per-type mute (FR-7.3) |
| Email verification / password reset | Below notifs | Via single-use tokens (FR-1.7, proposal) |

Traceability: FR-1.6, FR-1.7, FR-7.3, design.md §6.

---

## 6. Component library index (what/where/how)

| Component | What | Where | How (key classes/tokens) |
|-----------|------|-------|--------------------------|
| Brand chip | Circular brand mark + wordmark | Header, Welcome, Login | `inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)]`; dot `h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]` |
| Bottom tab bar | Fixed nav | Every authenticated screen | Fixed bottom, safe-area inset; active `--brand` icon + label; inactive `--sea-ink-soft`; 44px+ targets |
| Center FAB `+` | Create post | Tab bar center | Rounded-full brand fill, `--brand-dark` text, 44px+ target, opens `/compose` |
| Header | Top bar | Every screen | `--header-bg` backdrop-blur, hairline `--line` bottom |
| Tab pills | Segmented control | Explore | Chip tokens; active brand fill |
| Avatar | Circular profile image | Everywhere | `relative inline-flex shrink-0 items-center justify-center overflow-hidden border border-[var(--line)] rounded-full bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)]`; sizes 24/32/40/56 |
| Island shell | Card container | Every card | `.island-shell`: 1px `--line`, `165deg` `--surface-strong`→`--surface`, `--inset-glint`, two-layer shadow, `blur(4px)`, radius `0.9–1rem` |
| Composer | Post creation | Home, Compose | `.island-shell`; textarea + chips + brand Post button |
| Post card | Feed item | Home, Explore, Profile | `.island-shell`; header, body, media, interaction bar, action row |
| Pill / tag | Small label | Chips, tabs | `.demo-pill`: `inline-flex items-center gap-0.35 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-0.35 py-0.65 text-[0.75rem] font-weight-700 text-[var(--sea-ink-soft)]` |
| Button primary | Main action | Login, Composer | `rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)]` |
| Button secondary | Ghost action | Login | `rounded-full border border-[var(--line)] bg-transparent px-6 py-3 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]` |
| Input | Text entry | Login | `.demo-input` per §4.2 |
| Alert | Error/info message | Login | `.demo-alert`: `rounded-lg border ... bg-[rgba(...)] p-3 text-sm` tinted |
| Empty state | No content | Feed, Explore | kicker + one line + one action; never dead end |
| Badge | Unread/muted indicator | Tab bar, Messages, Notifications | `inline-flex h-5 w-5 items-center justify-center rounded-full ... text-[0.625rem] font-bold text-white shadow-sm` |

---

## 7. States (all screens)

| State | Treatment |
|-------|-----------|
| Loading | `sand`/`foam` shimmer skeletons (no spinners; design.md §13 #6) |
| Empty | Empty-state pattern: kicker + one line + one action |
| Error | `.demo-alert` tinted, 1rem radius, path forward (never bare code) |
| Success | Brand/lagoon tinted confirmation; auto-dismiss toast (proposal, design.md §8) |
| Skeleton | `sand`/`foam` shimmer |

---

## 8. Responsive / adaptive behavior

| Breakpoint | Layout |
|------------|--------|
| Mobile ≤ 640px | Single column, bottom tab bar (Home/Explore/+/Messages/Profile), header search may tuck into menu, story row horizontally scrollable |
| Tablet 641–1023px | Same single-column pattern but wider content; may expose web left rail or keep bottom tabs — confirm IA with owner (product.md Q1; design.md §13 #1) |
| Dark mode | Re-lit scene (design.md §6); tested in light/dark/auto; watch view may use pure `--bg-base` theater or lifted surface (design.md §14 #5) |

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
3. **Follow:** profile → [Follow] ghost → brand active; follower count derived (BR-2.3, never stored).
4. **Message:** `/messages` → open conversation → send → history cursor-paginated; unread badge via `last_read_message_id`.
5. **Moderate:** `/admin/reports` → review → action/dismiss → audit entry (append-only).
6. **Admin member action:** `/admin/members` → role/suspend/deactivate → audit entry; deactivated can't auth (FR-1.4).

---

## 11. Traceability matrix (mobile)

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

## 13. Implementation mapping (mobile → frontend/src)

| Spec section | Screen/component | Frontend file |
|--------------|------------------|---------------|
| §3 Global shell | Header | `src/components/Header.tsx` |
| §3 Global shell | Bottom tab bar + FAB | `src/components/Footer.tsx` |
| §4.1 Welcome | Welcome page | `src/routes/welcome.tsx` |
| §4.2 Login | Login page | `src/routes/login.tsx` |
| §5.1 Home feed | Home page | `src/routes/index.tsx` |
| §5.1 Post card | Post card | `src/components/PostCard.tsx` |
| §5.1 Composer | Composer | `src/components/Composer.tsx` |
| §5.2 Explore / Watch | Explore page | `src/routes/explore.tsx` |
| §5.3 Notifications | Notifications page | `src/routes/notifications.tsx` |
| §5.4 Profile | Profile page | `src/routes/profile.\$username.tsx` |
| §5.5 Messages list | Messages list | `src/routes/messages.tsx` |
| §5.6 Messages thread | Message thread | `src/routes/messages.\$conversationId.tsx` |
| §5.7 Create post | Compose page | `src/routes/compose.tsx` |
| §5.8 Admin | Admin pages | *(not yet implemented — placeholder routes)* |
| §5.9 Member settings | Settings page | `src/routes/settings.tsx` |
| §6 Component library | Avatar | `src/components/Avatar.tsx` |
| §6 Component library | Theme toggle | `src/components/ThemeToggle.tsx` |
| §6 Component library | Footer | `src/components/Footer.tsx` |
| §6 Component library | Utilities | `src/lib/utils.ts` |
| §1 Design tokens | Token definitions | `src/styles.css` |
| — | Route tree (generated) | `src/routeTree.gen.ts` |
| — | Root shell | `src/routes/__root.tsx` |
| — | Router | `src/router.tsx` |

Tests: `src/components/Avatar.test.tsx`, `src/components/Composer.test.tsx`, `src/components/PostCard.test.tsx`, `src/test/setupTests.ts`, `vitest.config.ts`

---

## 14. Open questions (confirm with owner)

1. IA: bottom tab bar (Home/Explore/+/Messages/Profile) vs top-tab or rail pattern on larger mobile (product.md Q1; design.md §13 #1).
2. Watch: vertical shorts vs 16:9 inline player vs both (design.md §14 #1).
3. Discover/"Trending" on or off by default for new workspaces (BR-12 #2).
4. Brand mark + favicon/app icons (design.md §14 #2).
5. Welcome screen: direct "open" signup or strictly invite acceptance (BR-1.5 — `open` never means internet-public).
6. Admin surface scope on mobile and which routes are `moderator`-visible vs `admin`-only.

---

*Related:* [`web-ui.md`](web-ui.md) · [`design.md`](design.md) · [`product.md`](../product.md) · [`docs/functional-requirements.md`](functional-requirements.md) · [`docs/business-rules.md`](business-rules.md) · [`docs/architecture.md`](architecture.md)

# Chafgram — Web UI Specification

> Complete UI specification for the Chafgram member-facing web app (desktop + tablet).
> This doc is the single source of truth for **what** each UI component is, **where** it
> appears, and **how** it is built (tokens, markup structure, variants, states).
>
> It comprises two parts:
> 1. **Wireframe** — screen layouts and assembly (§2–§10).
> 2. **Component spec** — what/where/how for every component type (§11–§14).
>
> Grounded in [`design.md`](design.md) (tokens, components, motion, a11y), the reference UI
> example (`.tmp/web-ui.webp`), [`docs/functional-requirements.md`](functional-requirements.md)
> (FR), [`docs/business-rules.md`](business-rules.md) (BR), and [`docs/architecture.md`](architecture.md)
> (API surface).
>
> Implemented in `frontend/src/` (TanStack Start + Tailwind v4). UI only — no backend
> connection yet (mock data). Brand is blue (`#2563eb`) per the reference, not lagoon.
>
> Status: design proposal. Confirm IA, right-rail visibility, and admin scope with the
> owner before finalising (§15).

---

## 0. Reference update — 7-screen set

The reference image (`.tmp/web-ui.webp`) was expanded to seven screens. The earlier
single-feed reference is still accurate for the home feed; everything below is **new or
changed** and is implemented in `frontend/src/`.

| Screen | What changed / added |
|--------|----------------------|
| **Landing** (`/welcome`) | Full-viewport marketing card with its own nav (Home · Explore · About anchor), "Sign In" + "Get Started" buttons, hero "Connect. Share. Belong.", subtitle, "Get Started →" + "Learn More ↓", and a blue chat-bubble/people illustration. The app Header/Footer are hidden on `/welcome` and `/login`. |
| **Home feed** (`/`) | Unchanged layout; left rail now ends with a **user chip** (avatar + name + `@handle` + chevron) instead of the tagline. |
| **Explore** (`/explore`) | Added a **Suggested Topics** panel (Technology 234K, Travel 156K, Lifestyle 108K, Education 142K, Gaming 128K) with per-topic `+` buttons. Active tab is a solid brand pill. |
| **Profile** (`/profile/:username`) | Added a **cover photo** banner with an overlapping avatar, Edit Profile + settings buttons below the cover, stats row, Posts/Replies/Media/Likes tabs; right rail has **About** (location, GitHub link, joined date) and **Skills** panels. |
| **Messages** (`/messages`) | Now a **two-pane** screen: conversation list with search on the left, chat thread with header, message bubbles, and composer on the right. The standalone thread route still works but is secondary. |
| **Notifications** (`/notifications`) | Added **tabs** (All · Likes · Comments · Follows · Mentions); items show actor avatars, action text, a Follow button or thumbnail, and unread dots. |
| **Create Post** (`/compose`) | Two-column card: text + Photo/Video/Feeling/Poll chips on the left, a dashed **drag-and-drop upload area** ("Upload Image / or drag and drop") on the right, Cancel/Post in a bottom action bar. |

New shared component: **`AppShell`** (`src/components/AppShell.tsx`) wraps every authenticated
screen with `LeftRail` + the `lg:pl-64` content offset, optionally with a right rail.

---

## 1. Design tokens (web)

All values are CSS custom properties from `design.md` §5 plus brand blue added in
`frontend/src/styles.css`. Use tokens, never raw hex.

| Role | Token | Value | Use |
|------|-------|-------|-----|
| Primary text | `--sea-ink` | `#14213d` (light) / `#e3ebff` (dark) | Body, headings, labels |
| Secondary text | `--sea-ink-soft` | `#51617d` / `#b2c2e2` | Captions, metadata, inactive nav |
| Brand | `--brand` | `#2563eb` | Primary buttons, active nav, links, focus |
| Brand dark | `--brand-dark` | `#1d4ed8` | Hover/active brand states |
| Brand light | `--brand-light` | `#eff6ff` | Active nav bg, subtle brand surfaces |
| Accent | `--lagoon` | `#78a9ff` / `#78a9ff` | Secondary accents |
| Brand kicker | `--palm` | `#31558e` / `#a6c4ff` | Eyebrow text |
| Base bg | `--bg-base` | `#f4f6fb` / `#091221` | Page background |
| Card fill | `--surface` | `rgba(255,255,255,0.74)` / `rgba(20,34,58,0.82)` | `.island-shell` cards |
| Card fill strong | `--surface-strong` | `rgba(255,255,255,0.9)` / `rgba(17,29,50,0.94)` | Elevated cards |
| Hairline | `--line` | `rgba(61,91,148,0.15)` / `rgba(166,192,255,0.18)` | Dividers, input borders |
| Header bg | `--header-bg` | `rgba(255,255,255,0.86)` / `rgba(9,18,33,0.86)` | Sticky header (backdrop-blur) |
| Chip | `--chip-bg` / `--chip-line` | — | Pills, brand chip |
| Link bg hover | `--link-bg-hover` | `rgba(255,255,255,0.9)` / `rgba(24,44,49,0.8)` | Ghost button hover |

Typography (design.md §4):
- **Display/headings**: Fraunces (variable, `opsz` 9–144, weights 500/700). `line-height ≈ 1.02–1.05`. Never below ~1.25rem.
- **Body/UI**: Manrope (400–800). Body 400/500, emphasis 600/700, buttons 700, kickers 700 uppercase `0.16em` tracking `0.69rem`.
- Scale: kicker `0.69rem` · caption `0.75rem` · body `1rem` · lead `1.125–1.25rem` · h2 `~1.25rem` · h1 `clamp(2.25rem, 4vw + 1rem, 3.75rem)`.

Motion (design.md §9): `.rise-in` 700ms `cubic-bezier(0.16,1,0.3,1)`, `translateY(12px)` + fade, staggered ~90ms/sibling; hover 180ms ease; respects `prefers-reduced-motion`.

---

## 2. Screen inventory (web)

| Path | Auth | Screen | Primary action |
|------|------|--------|----------------|
| `/welcome` | No | Welcome / landing | Sign in or contact admin |
| `/login` | No | Login | Password sign in (username or email) |
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
| `/settings` | Yes | Member settings | Theme, notification prefs |

Roles: `member`, `moderator`, `admin`. One workspace per deployment (BR-1.1).

---

## 3. Global shell (authenticated screens)

Three-column layout: **left rail / content column / right rail**. Content column is
`.page-wrap` — `min(1080px, 100% - 2rem)`, centered.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Header (sticky): [Brand chip] Search Chafgram…                             │
├──────────┬──────────────────────────────────────────────┬──────────────────┤
│ Left Rail│  Content column  .page-wrap                  │ Right Rail       │
│ (w-64)   │                                              │ (w-80, lg+)      │
│ Home●    │  ┌─────────────────────────────────────────┐ │ Who to follow    │
│ Explore  │  │ Composer: "What's on your mind?"        │ │ [See all →]      │
│ Messages │  │ [Photo] [Video] [Feeling] [Poll] [Post]│ │ ┌─────────┐      │
│ Notifications│ │                                         │ │ avatar · Mey │
│ Profile  │  ┌─────────────────────────────────────────┐ │ Sokka 12.5K f │[Follow]│
│ More     │  │ Post card                               │ │ ┌─────────┐      │
│          │  │ avatar · Dara · 2h ago · ···            │ │ avatar · Dara  │[Follow]│
│          │  │  body text                              │ │ Chan 8.7K f │[Follow]│
│          │  │  [media image]                          │ │ ┌─────────┐      │
│          │  │  ♥ 248              💬 32   ➤ 12   [bookmark]   │ │ avatar · Vizea│[Follow]│
│          │  │  [Like] [Comment] [Share] [Save]        │ │ 6.3K f    │[Follow]│
│          │  └─────────────────────────────────────────┘ │           Trending│
│          │  ┌─────────────────────────────────────────┐ │ 1 Tech & AI …   │
│          │  │ Post card …                             │ │ 2 Life in Cam… │
│          │  └─────────────────────────────────────────┘ │ 3 Developer Tips│
│          └─────────────────────────────────────────────┘ │ 4 Study and Career│
│ [Create Post] blue pill                                  │ 5 Random Thoughts│
│ "Connect. Share. Belong."                                │ [Join Chafgram]  │
└──────────┴──────────────────────────────────────────────┴──────────────────┘
```

**Header** (sticky, `design.md` §7): panel-left toggle button (labels "Collapse navigation" / "Expand navigation") that switches the shared rail state, brand chip + "Chafgram" wordmark (Manrope bold, wordmark hidden when collapsed) and a search input (`.demo-input`). Header padding tracks the rail: expanded `pl-32 sm:pl-40 lg:pl-64`, collapsed `pl-16`. No theme toggle or avatar here — theme switching lives in Settings and the user chip lives in the left rail. No nav links here — nav lives in LeftRail.

**Left rail** (`LeftRail` component): fixed `w-64`, `top-14 bottom-0`, `border-r`, `bg-white/60 backdrop-blur-md`. Brand chip + wordmark at top. Nav links: Home, Explore, Messages (badge 3), Notifications (badge 5), Profile. Active link: `bg-[var(--brand-light)] text-[var(--brand-dark)] font-semibold`. Inactive: `text-[var(--sea-ink-soft)] hover:bg-[var(--brand-light)]/50`. Bottom: blue "Create Post" pill (rounded-full, `border-[var(--brand)] bg-[var(--brand)] text-white`, hover `bg-[var(--brand-dark)] shadow-lg shadow-[var(--brand)]/40`) + user chip with the signed-in member's display name and `@username` from `me`, with a name-initial avatar fallback. The chip is the `AccountMenu` trigger: clicking it opens a popup with only Settings (`/settings`) and Sign out. No hardcoded member names in the shell. **Collapsible:** the header toggle switches the rail between `w-64` and a `w-16` icon-only strip; collapsed links, Create Post and the account trigger keep `aria-label`s and the unread badges remain visible. `AppShell`'s content offset switches from `lg:pl-64` to `lg:pl-16`. The choice persists in `localStorage['rail-collapsed']`.

**Right rail** (`RightRail` component): `w-80`, hidden below `lg` breakpoint. "Who to follow" (kicker + member list + "See all →"), "Trending" (numbered list + external arrows), "Join Chafgram Today" card (kicker + line + blue "Sign Up" pill). **Note:** private/invite-only default (BR-1.5) — copy confirmed with owner.

**Responsive:** Desktop ≥ 1024px: three columns. Tablet 641–1023px: left rail collapses to icons or hides behind menu; right rail hidden; content full width. Mobile ≤ 640px: see `mobile-ui.md`.

---

## 4. Unauthenticated screens

### 4.1 Welcome (`/welcome`)

Full-bleed ambient background (hero washes, `design.md` §5). Centered island-shell card max ~600px, `text-center`.

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Brand chip + wordmark | Header of card | inline-flex gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow; inner `h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]` dot; wordmark `font-bold tracking-tight` |
| Kicker | Below brand | `island-kicker` (letter-spacing 0.16em, uppercase, font-weight 700, font-size 0.69rem, color var(--kicker)) |
| h1 | Below kicker | `display-title` (Fraunces), `text-4xl font-bold text-[var(--sea-ink)] leading-[1.02] sm:text-6xl`, `mb-4` |
| Lead paragraph | Below h1 | `max-w-lg text-base leading-8 text-[var(--sea-ink-soft)] sm:text-lg`, `m-0` |
| Illustration | Below lead | `relative h-64 sm:h-80 rounded-xl overflow-hidden border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)80%,white_20%)]` with radial-gradient overlay + centered speech-bubble glyph SVG (`text-[var(--sea-ink-soft)] h-32 w-32`) |
| [Sign in] | Primary CTA | `flex-1 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] transition hover:bg-[color-mix(in_oklab,var(--brand)30%,var(--surface-strong))] sm:flex-none sm:w-48` → `/login` |
| [Contact your admin] | Secondary | `flex-1 rounded-full border border-[var(--line)] bg-transparent px-6 py-3 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)] sm:flex-none sm:w-48` → `/welcome` |
| Footer note | Bottom | `mt-8 text-[0.75rem] text-[var(--sea-ink-soft)]`: "New to Chafgram? Your admin creates your account via invite. Signup is invite-only by default." |

**Flow:** [Sign in] → `/login`. [Contact your admin] → info alert: signup is `invite_only` by default (BR-1.5); accepts at `POST /api/v1/invites/accept`. No public "Create account" unless `signup_mode=open`.

Traceability: FR-1.1, FR-1.3, BR-1.3, BR-1.5.

### 4.2 Login (`/login`)

Full-bleed ambient background, centered island-shell card max ~480px. No rails/nav.

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Brand chip + wordmark | Top of card | Same as welcome |
| Kicker "Sign in to your workspace" | Below brand | `island-kicker` |
| h1 "Welcome back" | Below kicker | `display-title`, `text-3xl font-bold text-[var(--sea-ink)] leading-[1.02] sm:text-4xl`, `mb-2` |
| Email/username input | Below heading | `.demo-input`: `w-full border border-[var(--line)] rounded-full bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[color-mix(in_oklab,var(--brand)58%,var(--line))] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--brand)24%,transparent)]` |
| Password input | Below email | Same `.demo-input`, `type="password"` |
| [Continue] | Below inputs | `w-full rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)] transition disabled:opacity-55 disabled:transform-none hover:!bg-[color-mix(in_oklab,var(--brand)30%,var(--surface-strong))]`; disabled until email + password filled |
| "Don't have an account? Contact your admin" | Below inputs | `mb-3 text-sm text-[var(--sea-ink-soft)]` + `font-semibold` "Contact your admin" |
| [← Back to welcome] | Link | `text-sm font-semibold text-[var(--brand-dark)] no-underline transition hover:text-[var(--brand)]` → `/welcome` |
| `.demo-alert` | Error state | `rounded-lg border border-[rgba(196,71,71,0.3)] bg-[rgba(196,71,71,0.1)] p-3 text-sm text-[#9f3030]` (never shown by default) |

**Flow:** email/username + password → [Continue] → BFF `loginRequest` → access token in browser memory + HttpOnly `chafgram_refresh` cookie → redirect `/`. A 401 on refresh clears the token and redirects back to `/login`. Errors: `.demo-alert` tinted red, never a bare code — always a path forward.

Traceability: FR-1.3, BR-1.3, BR-1.5, architecture.md §4.1.

---

## 5. Authenticated screens

**Interaction notes — regional busy overlays.** Explicit user mutations (login in §4.2,
home and `/compose` composers, post-card like/comment, messages send, notifications
mark-all-read) wrap their control region in a `relative` root with `aria-busy={busy}`.
The content subtree is `inert` while busy, and a `BusyOverlay` child (§7) renders an
`aria-hidden`, `absolute inset-0` token surface with a spinner and an action-specific label
(e.g. `Signing in…`, `Posting…`, `Saving…`, `Sending…`, `Updating…`). A permanently mounted
`sr-only` sibling of the busy root carries `role="status" aria-live="polite"` with that label
while busy and empty text otherwise, so the announcement is not owned by `aria-busy`. The
visual overlay sits outside the inert subtree. Controls are also `disabled`, and `useBusy`
skips re-entry, so a rapid double click fires one mutation. Initial data loading (feed, comment thread) keeps `sand`/`foam` skeletons and
never shows an overlay; effect-driven `markRead` is excluded too. See [`design.md`](design.md)
§9–§10.

### 5.1 Home feed (`/`) — default landing

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Composer | Top of content column | `.island-shell rise-in rounded-xl overflow-hidden border border-[var(--line)] bg-white shadow-sm`; avatar row + textarea, attachment previews, action row |
| Composer avatar | Left of composer | `Avatar` size 32 |
| "What's on your mind?" | Composer header | `text-[0.875rem] font-medium text-[var(--sea-ink)] leading-none` |
| Textarea | Below header | `w-full mt-2 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline focus:ring-0 resize-none`, `rows={3}` |
| Chips [Photo][Video][Feeling][Poll] | Action row below textarea | `flex flex-wrap items-center gap-1 sm:gap-2`; icon + label. [Photo] opens a hidden `input[type=file][accept="image/*"][multiple]`, [Video] opens `accept="video/*"`; both upload through the `uploadMedia` BFF server function on [Post] and pass the resulting `mediaIds` and post type to `createPost`. Selected files show removable thumbnails before posting; upload failures appear in a `role="alert"` line. [Feeling] and [Poll] are disabled until implemented |
| [Post] button | Right of composer | `rounded-full border px-5 py-2.5 text-sm font-bold text-white shadow-lg shadow-[var(--brand)]/30`; active `bg-[var(--brand)] hover:bg-[var(--brand-dark)]`; disabled `bg-[color-mix(in_oklab,var(--brand)20%,white_80%)] border-transparent text-[var(--brand-dark)]` disabled:opacity-60 |
| Post cards | Below composer, `mt-8 space-y-6` | See §5.1.1 |
| "End of feed" | Bottom | `h-24 flex items-center justify-center text-[var(--sea-ink-soft)] text-sm` |

### 5.1.1 Post card (`.island-shell`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Shell | Card root | `.island-shell rounded-[1.5rem] overflow-hidden border border-[var(--line)] bg-white shadow-sm` |
| Header row | Top of card | `flex gap-3 p-4 sm:p-5`; avatar left, more-menu right (`ml-auto p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]`) |
| Avatar + name + time | Header left | `Link to="/profile/$username" params={{username}}` wrapping `Avatar` size 32 + `flex flex-col leading-none`: name `text-sm font-semibold text-[var(--sea-ink)] block`, meta `text-[0.75rem] text-[var(--sea-ink-soft)] block` (`{createdAt} · public`, `edited` marker if applicable) |
| More menu | Header right | 3-dot circle SVG (`h-4 w-4`) |
| Body | Below header (if present) | `px-4 sm:px-5 pt-1 sm:pt-0 pb-3`: `m-0 text-base text-[var(--sea-ink)] leading-7` |
| Media | Below body (if present) | `px-4 sm:px-5 pb-5 sm:pb-0`: `aspect-video rounded-xl overflow-hidden border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)70%,white_30%)]` + `img h-full w-full object-cover` for photos; `video h-full w-full object-cover controls preload="metadata" playsInline` for VIDEO posts. Both use the 1-hour signed URL from GraphQL `Post.mediaUrls[index]` (`/api/media/:id?exp=…&uid=…&sig=…`, BFF proxy of the backend bytes endpoint with byte-range support). On `onError`, one BFF re-sign swaps `src`; posts without an attachment keep the SVG gradient placeholder. A single photo keeps the full-width frame; 2+ photos render in a `grid grid-cols-2 gap-1` of `aspect-square` tiles with **no cap**. Every photo is wrapped in a button labelled `Preview <alt>`: clicking opens `ImageLightbox` at that image |
| Interaction bar | Below media/body | `px-4 sm:px-5 pt-4 pb-5 sm:pt-0 border-t border-[var(--line)]`: `flex items-baseline justify-between gap-4 sm:gap-6 mb-3` with `♥ {count}` `text-sm font-semibold text-[var(--sea-ink)]` on the left and `{comments} comments` + `{shares} shares` `text-[var(--sea-ink-soft)] text-sm` grouped on the right. Counts and the viewer's reaction state come from the normalized `Post` cache entity, so they update in every feed query after a like/unlike or comment — not just the query that happened to be active |
| Action row | Below interaction bar | `flex items-center gap-5 sm:gap-8 pt-2`: [Like]/[Unlike] (`reactToPost`/`unreactToPost`), [Comment] (toggles thread), [Share] `flex items-center gap-1.5 text-sm font-medium text-[var(--sea-ink-soft)] transition hover:text-[var(--brand-dark)]` with SVG icons; liked state uses `text-[var(--brand-dark)]`; [Save] `ml-auto p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]` bookmark SVG |
| Comment thread | Below action row when [Comment] is active | `mt-4 border-t border-[var(--line)] pt-4`: comments loaded by the `PostComments` query (oldest-first, up to 50) and rendered as rows with avatar + name + relative time + body. Composer below the list: text `Input` + [Post] button (disabled while empty). Submitting appends the returned comment to the visible thread and to the `postComments` cache, and increments the post's `commentCount`, so the new comment is visible immediately without a manual refresh. Empty list → `No comments yet.` |

**States:** loading → `sand`/`foam` shimmer skeletons for feed content; the comment thread shows a `Loading comments…` label until `PostComments` resolves. Empty → empty-state pattern (kicker + one line + one action); error → `.demo-alert`. Tombstoned posts invisible everywhere (BR-3.1); edited posts show `edited` marker (FR-3.2). Optimistic reactions/comments are rolled back on failure (FR-3.8, FR-4.1).

Traceability: FR-3.1, FR-3.2, FR-3.4, FR-3.7, FR-3.8, FR-4.1, FR-4.3, FR-5.1, FR-5.3, BR-2.4, BR-3.1–BR-3.5.

### 5.2 Explore / Watch (`/explore`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Header "Explore" + search | Top | `flex items-center justify-between mb-6`: h1 `m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl`; search button `rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-2 py-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]` |
| Tab pills | Below header | `flex gap-2 overflow-x-auto pb-2 mb-6 no-scrollbar`: each `rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-4 py-1.5 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)] whitespace-nowrap`; tabs: For You / Following / Trending / Nearby |
| Content grid | Below tabs | `grid gap-4 sm:grid-cols-2 lg:grid-cols-3`; each cell `.island-shell feature-card rise-in overflow-hidden rounded-xl` |
| Feature card media | Top of cell | `relative h-48 overflow-hidden`: `img h-full w-full object-cover` + `absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent` + title `absolute bottom-0 left-0 right-0 p-4`: h3 `m-0 text-white font-semibold text-lg shadow-sm` |
| Feature card heart | Bottom of cell | `p-3 flex items-center justify-between`: `♥ text-[var(--lagoon-deep)]` + count `text-sm font-semibold text-[var(--sea-ink)]` |

Traceability: FR-3.5 (discover feed — proposal), FR-5.3, BR-5.1, BR-12 #2.

### 5.3 Notifications (`/notifications`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Header | Top | `flex items-center justify-between mb-6`: h1 `m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl`; unread count `m-0 mt-1 text-sm text-[var(--sea-ink-soft)]`; [Mark all as read] button `rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]` |
| Notification items | List, `space-y-3` | Each `.island-shell rise-in flex items-center gap-3 p-4 sm:p-5 rounded-xl` |
| Avatar | Item left | `Avatar` size 32 |
| Action text | Item center | `m-0 text-base text-[var(--sea-ink)] leading-6`: actor `font-semibold`, action, target in `text-[var(--sea-ink-soft)] font-normal` |
| Timestamp | Below action | `text-[0.75rem] text-[var(--sea-ink-soft)] block` |
| Unread dot | Item right | `ml-auto inline-block h-2 w-2 rounded-full bg-[var(--brand)] shadow-[0_0_0_2px_rgba(255,255,255,0.9)]` |
| Footer | Bottom | `mt-10 flex items-center justify-center text-[var(--sea-ink-soft)] text-sm`: "That's everything so far." |

Types per FR-7.1: new follower, like/reaction, comment on your post, reply to your comment, @mention, direct message. In-app only (BR-7.1). Per-type mute (FR-7.3).

Traceability: FR-7.1, FR-7.2, FR-7.3, BR-7.1–BR-7.3.

### 5.4 Profile (`/profile/:username`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Shell | Card root | `.island-shell rise-in rounded-2xl p-6 sm:p-8 mb-8` |
| Avatar | Header left | `Avatar` size 56 |
| Name + handle | Header center | h1 `m-0 text-3xl font-bold text-[var(--sea-ink)] sm:text-4xl leading-[1.02]`; handle `@username` `ml-1 text-sm font-medium text-[var(--sea-ink-soft)]` |
| Bio | Below name | `m-0 mt-2 max-w-md text-base text-[var(--sea-ink-soft)] leading-7` (≤ 160, FR-2.1) |
| Stats row | Below bio | `mt-5 flex items-center gap-4 sm:gap-6 text-sm`: Posts / Followers / Following, each `font-semibold text-[var(--sea-ink)]` number + `text-[var(--sea-ink-soft)]` label |
| [Edit Profile] | Stats row | `flex-1 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-4 py-2 text-sm font-semibold text-[var(--sea-ink)] transition hover:bg-[color-mix(in_oklab,var(--brand)30%,var(--surface-strong))]` (owner only) |
| [Share] | Stats row | `rounded-full border border-[var(--line)] bg-transparent px-4 py-2 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]` + share SVG |
| Tabs | Below shell | `flex gap-2 border-b border-[var(--line)] pb-4 overflow-x-auto no-scrollbar sm:overflow-visible sm:flex-nowrap`: Posts | Replies | Media | Likes; active `rounded-full border-b-2 border-[var(--lagoon)] px-4 py-1.5 text-sm font-semibold text-[var(--sea-ink)]`; inactive `border-transparent` |
| Posts list | Below the profile shell | Full-width single-column `space-y-6` list of `PostCard`s (same shape as the home feed), from `memberPosts(username, first: 20)` for the displayed member (`/profile/me` resolves to the signed-in username); skeleton while loading, "No posts yet." when empty, visible error message on failure |

**States:** Deactivated → "Deactivated member"; suspended → visible + suspended badge (BR-2.4). Blocked: mutual-effect hiding (BR-2.2).

Traceability: FR-2.1–FR-2.5, BR-2.1–BR-2.4.

### 5.5 Direct messages — list (`/messages`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Header | Top | `flex items-center gap-4 mb-6`: h1 `m-0 text-2xl font-bold text-[var(--sea-ink)] sm:text-3xl`; [New message] button `ml-auto rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]` |
| Shell | List root | `.island-shell rise-in rounded-2xl overflow-hidden border border-[var(--line)]` |
| Section header | Top of shell | `border-b border-[var(--line)] px-4 sm:px-6 py-3 text-xs font-semibold uppercase tracking-wider text-[var(--sea-ink-soft)]` |
| Conversation items | List, `divide-y divide-[var(--line)]` | Each `Link to="/messages/$conversationId" params={{conversationId}}` `block px-4 sm:px-6 py-4 hover:bg-[color-mix(in_oklab,var(--surface-strong)60%,white_40%)] transition` |
| Avatar | Item left | `Avatar` size 40 |
| Name + status | Item center | `flex items-baseline gap-2`: name `text-sm font-semibold text-[var(--sea-ink)] block`; muted badge `inline-flex items-center gap-1 rounded-full border border-[rgba(47,106,74,0.18)] bg-[color-mix(in_oklab,var(--palm)10%,var(--chip-bg))] px-2 py-0.5 text-[0.65rem] font-bold text-[var(--palm)]` with dot `h-1.5 w-1.5 rounded-full bg-[var(--palm)]`; unread badge `ml-auto inline-flex h-5 w-5 items-center justify-center rounded-full border border-white bg-[var(--brand)] px-1 text-[0.625rem] font-bold text-white shadow-sm` |
| Preview | Below name | `m-0 text-[var(--sea-ink-soft)] text-sm truncate` |
| Time | Item right | `text-[0.7rem] text-[var(--sea-ink-soft)] whitespace-nowrap` |

**Rules:** At most one direct conversation per unordered pair (BR-6.1). Blocked members cannot message (BR-6.2). Unread via `last_read_message_id` (FR-6.4). Muted via `muted_until` (FR-6.5). No per-message read receipts (BR-6.6).

Traceability: FR-6.1, FR-6.4, FR-6.5, BR-6.1, BR-6.2.

### 5.6 Direct messages — thread (`/messages/:conversationId`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Header | Top | `mb-6 flex items-center gap-3`: [Back] link `text-[var(--sea-ink-soft)] no-underline transition hover:text-[var(--sea-ink)]` + back-arrow SVG; avatar size 32; name `text-sm font-semibold text-[var(--sea-ink)]`; right: "Delivered" `hidden sm:inline-block text-[0.75rem] text-[var(--sea-ink-soft)]` + more-options button `p-1.5 text-[var(--sea-ink-soft)] transition hover:text-[var(--sea-ink)]` |
| Shell | Message area | `.island-shell rise-in rounded-2xl h-[60vh] flex flex-col overflow-hidden border border-[var(--line)]` |
| Message area | Scrollable | `flex-1 overflow-y-auto p-4 sm:p-6`; `space-y-4` messages |
| Message bubble | Each message | `flex justify-end` (mine) / `justify-start` (theirs); bubble `max-w-[80%] rounded-2xl p-3`; mine `bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] text-[var(--sea-ink)]`; theirs `bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] border border-[var(--line)] text-[var(--sea-ink)]`; sender name `m-0 mb-1 text-xs font-semibold text-[var(--sea-ink-soft)] uppercase tracking-wider` (theirs only); text `m-0 text-sm leading-6 whitespace-pre-wrap`; meta `m-0 mt-1 text-[0.65rem] text-[var(--sea-ink-soft)] text-right` time + `edited` |
| Composer | Bottom | `border-t border-[var(--line)] p-4 sm:p-6 bg-[color-mix(in_oklab,var(--surface-strong)90%,white_10%)]`: `relative` wrapper; input `w-full pl-10 pr-4 py-2 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--surface-strong)88%,white_12%)] text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[color-mix(in_oklab,var(--brand)58%,var(--line))] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--brand)24%,transparent)]`; attach button `absolute left-2 top-1/2 -translate-y-1/2 p-1 text-[var(--sea-ink-soft)]`; send button `absolute right-2 top-1/2 -translate-y-1/2 rounded-full border border-[var(--line)] bg-[var(--brand)] p-1.5 text-[var(--brand-dark)] font-bold transition hover:bg-[var(--brand-dark)] hover:text-white shadow-sm` |

**Rules:** Cursor pagination `(conversation_id, id)` (FR-6.3). Sender name + `edited_at` (BR-6.4). Media via `message_media` (FR-6.6). Groups: create/title/add/remove, group admin role (FR-6.2).

Traceability: FR-6.2, FR-6.3, FR-6.4, FR-6.6, BR-6.3–BR-6.6.

### 5.7 Create post (`/compose`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Header | Top | `mb-6 flex items-center gap-3`: [Back] link + h1 `m-0 text-xl font-bold text-[var(--sea-ink)] sm:text-2xl` "Create post" |
| Shell | Card root | `.island-shell rise-in rounded-2xl p-6 sm:p-8` |
| Avatar + textarea | Top | `flex gap-3 mb-6`: avatar size 40 + `flex-1 min-w-0`: textarea `w-full demo-textarea mt-1 text-base text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] no-underline focus:ring-0`, placeholder "What's on your mind?" |
| Chips | Below textarea | `flex flex-wrap gap-2 mt-3`: [Photo][Video][Feeling][Poll] `rounded-full border px-3 py-1.5 text-xs font-semibold transition`; active `border-[var(--chip-line)] bg-[var(--chip-bg)] text-[var(--sea-ink)]`; inactive `border-transparent bg-transparent text-[var(--sea-ink-soft)] hover:bg-[var(--link-bg-hover)]` |
| Media attach | Below chips | `flex items-start gap-3`: label `text-[var(--sea-ink-soft)] text-sm mt-0.5`; button `flex items-center gap-2 rounded-full border border-[var(--line)] bg-transparent px-4 py-2 text-sm font-semibold text-[var(--sea-ink)] transition hover:bg-[var(--link-bg-hover)]` + attach SVG + "Attach photo or video" |
| Note | Below media | `mb-6 text-[0.75rem] text-[var(--sea-ink-soft)]`: "Posts can include text, photos, or a single video. Mixed photo + video posts aren't supported." |
| Actions | Bottom | `flex justify-end`: [Cancel] `px-6 py-2.5 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)] rounded-full border border-transparent`; [Post] `ml-3 rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-2.5 text-sm font-bold text-[var(--sea-ink)] transition disabled:opacity-55 disabled:transform-none hover:!bg-[color-mix(in_oklab,var(--brand)30%,var(--surface-strong))]` (disabled until valid) |

**Rules:** Text body optional for media-only (FR-3.1); 5,000 char cap (BR-3.3); 1..N images or exactly 1 video, mixed rejected (BR-3.2); video upload progress inline (FR-5.2).

Traceability: FR-3.1, FR-3.2, FR-5.1, FR-5.2, BR-3.2, BR-3.3.

### 5.8 Admin console

Access: `admin` routes enforce `ROLE_ADMIN`; moderator routes `ROLE_MODERATOR|ROLE_ADMIN` (architecture.md §10). Auth failures return HTTP 200 + `errors[]` with `extensions.classification` — assert on `$.errors`, not status.

| Sub-screen | Path | Components |
|------------|------|------------|
| Members | `/admin/members` | `.demo-table-shell` overflow-x table; header `chip-bg`; row hover `lagoon` 8%; columns: avatar, username, display name, role, status, last action; role change buttons; suspend/deactivate/reactivate |
| Workspace settings | `/admin/settings` | Workspace name, slug, logo, primary color, signup mode (`invite_only` default \| `open`); note slug/name changes never rewrite content (BR-1.6); `open` never internet-public (BR-1.5) |
| Audit log | `/admin/audit` | Filter: actor, action, target type, date range; append-only entries; moderator-visible only (BR-9.1) |
| Reports queue | `/admin/reports` | List reports (reason: spam\|harassment\|nsfw\|violence\|impersonation\|other); take ownership (`reviewing`), action (`actioned`) or dismiss (`dismissed`); rate limit 1 per member per target per 24h (BR-9.3, proposal) |

Traceability: FR-1.4, FR-1.5, FR-9.1–FR-9.6, FR-10.1–FR-10.5, BR-1.4–BR-1.6, BR-2.4, BR-9.1–BR-9.5, BR-10.1–BR-10.5, BR-11.3.

### 5.9 Member settings (`/settings`)

**Components (what/where/how):**
| Component | Where | How |
|-----------|-------|-----|
| Appearance section | `.island-shell rise-in rounded-2xl p-6 sm:p-8` | kicker "Appearance"; theme toggle `rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-xs font-semibold text-[var(--sea-ink)] shadow shadow-[0_8px_22px_rgba(30,90,72,0.08)] transition hover:-translate-y-0.5` |
| Notifications section | `.island-shell ...` | kicker "Notifications"; checkbox list `flex items-center justify-between px-4 py-3 cursor-pointer group`: label (title + helper `text-[0.75rem] text-[var(--sea-ink-soft)]`) + checkbox `rounded border-[var(--line)] text-[var(--brand)] focus:ring-[var(--brand)]` |

Traceability: FR-1.7, FR-7.3, design.md §6.

---

## 6. States (all screens)

| State | Treatment |
|-------|-----------|
| Loading | `sand`/`foam` shimmer skeletons (no spinners; design.md §13 #7) |
| Action busy | `BusyOverlay` — `aria-hidden`, `absolute inset-0` token surface + spinner + label over a `relative`, `aria-busy` region; content subtree `inert`; controls disabled; permanently mounted `sr-only` `role="status" aria-live="polite"` sibling of the busy root announces the label |
| Empty | Empty-state pattern: kicker + one line + one action |
| Error | `.demo-alert` tinted, 1rem radius, path forward (never bare code) |
| Success | Lagoon/brand tinted confirmation; auto-dismiss toast (proposal, design.md §8) |
| Skeleton | `sand`/`foam` shimmer |

---

## 7. Component library index (what/where/how)

This section lists every reusable component type used across web screens. Each entry gives **what** it is, **where** it appears, and **how** it is constructed (classes, tokens, variants).

| Component | What | Where | How (key classes/tokens) |
|-----------|------|-------|--------------------------|
| Brand chip | Circular brand mark + wordmark | Header, LeftRail, Welcome, Login | `inline-flex items-center gap-2 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)]`; dot `h-2 w-2 rounded-full bg-[linear-gradient(90deg,#56c6be,#7ed3bf)]` |
| Nav link | Rail navigation item | LeftRail | `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition`; active `bg-[var(--brand-light)] text-[var(--brand-dark)] font-semibold`; inactive `text-[var(--sea-ink-soft)] hover:bg-[var(--brand-light)]/50`; badge `ml-auto inline-flex h-5 w-5 ... bg-[var(--brand)] text-white` |
| Create Post pill | Primary CTA | LeftRail bottom | `w-full flex items-center justify-center gap-2 rounded-full border border-[var(--brand)] bg-[var(--brand)] px-4 py-3 text-sm font-bold text-white transition hover:bg-[var(--brand-dark)] hover:shadow-lg hover:shadow-[var(--brand)]/40` |
| Search input | Header search | Header | `w-full pl-9 pr-3 py-1.5 rounded-full border border-[var(--line)] bg-white/80 text-sm text-[var(--sea-ink)] placeholder-[var(--sea-ink-soft)] outline-none transition focus:border-[var(--brand)] focus:shadow-[0_0_0_3px_color-mix(in_oklab,var(--brand)24%,transparent)]` |
| Theme toggle | Mode switcher | Settings | `rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-3 py-1.5 text-sm font-semibold text-[var(--sea-ink)] shadow shadow-[0_8px_22px_rgba(57,91,158,0.08)] transition hover:-translate-y-0.5` |
| Avatar | Circular profile image | Everywhere | `relative inline-flex shrink-0 items-center justify-center overflow-hidden border border-[var(--line)] rounded-full`; sizes 24/32/40/56; `img h-full w-full object-cover` or a single uppercase name initial on a deterministic `--avatar-tint-*` background (`text-[var(--sea-ink)] font-semibold`) |
| Island shell | Card container | Every card | `.island-shell`: 1px `--line`, `165deg` `--surface-strong`→`--surface`, `--inset-glint`, soft blue shadow, `blur(4px)`, radius `0.75rem` |
| Composer | Post creation | Home, Compose | `.island-shell rise-in relative overflow-hidden rounded-xl p-6 sm:p-8`; textarea + chips + blue Post button |
| Post card | Feed item | Home, Explore, Profile | `.island-shell rounded-xl overflow-hidden border border-[var(--line)] bg-white shadow-sm`; header, body, media, interaction bar, action row, optional comment thread |
| BusyOverlay | Regional indeterminate-action overlay | Login, Composer, Compose, PostCard, Messages, Notifications | `aria-hidden="true"`; `absolute inset-0 z-10 flex flex-col items-center justify-center gap-2 bg-[var(--surface-strong)] backdrop-blur-[2px]`; spinner `h-6 w-6 animate-spin rounded-full border-2 border-[var(--lagoon)] border-t-[var(--brand)] motion-reduce:animate-none`; label `text-sm font-semibold text-[var(--sea-ink)]`; sibling of an `inert` content subtree inside a `relative aria-busy` root; live status is a permanently mounted `sr-only` `role="status" aria-live="polite"` sibling of that root; driven by `useBusy` |
| ImageLightbox | Full-size image preview with navigation | PostCard | Base UI `Dialog` portal; backdrop `fixed inset-0 z-50 bg-[color-mix(in_oklab,var(--sea-ink)70%,transparent)]`; centered `z-50` popup with `bg-[var(--surface-strong)]`, image `object-contain`, labelled Previous/Next buttons (disabled at bounds), `n / N` live counter, close button; ←/→ and Escape supported |
| AccountMenu | Signed-in member chip and account popup | LeftRail bottom | `relative` wrapper; trigger is a `<button aria-label="Account menu" aria-expanded>` holding Avatar + name + `@username` + chevron; popup `absolute bottom-full mb-2 z-30 rounded-xl border border-[var(--line)] bg-[var(--surface-strong)] p-1`; Settings link + Sign out button only, closes on Escape / outside pointerdown / item activation / route change; Sign out uses `logoutRequest` + `useBusy` |
| Pill / tag | Small label | RightRail, chips | `.demo-pill`: `inline-flex items-center gap-0.35 rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)] px-0.35 py-0.65 text-[0.75rem] font-weight-700 text-[var(--sea-ink-soft)]` |
| Button primary | Main action | Login, Welcome, Composer | `rounded-full border border-[var(--line)] bg-[color-mix(in_oklab,var(--brand)22%,var(--surface-strong))] px-6 py-3 text-sm font-semibold text-[var(--sea-ink)]` active; disabled variants |
| Button secondary | Ghost action | Login, Welcome | `rounded-full border border-[var(--line)] bg-transparent px-6 py-3 text-sm font-semibold text-[var(--sea-ink-soft)] transition hover:bg-[var(--link-bg-hover)]` |
| Input | Text entry | Login, Settings | `.demo-input` per §4.2 |
| Alert | Error/info message | Login | `.demo-alert`: `rounded-lg border ... bg-[rgba(...)] p-3 text-sm` tinted |
| Empty state | No content | Feed, Explore | kicker + one line + one action; never dead end |
| Badge | Unread/muted indicator | LeftRail, Messages, Notifications | `inline-flex h-5 w-5 items-center justify-center rounded-full ... text-[0.625rem] font-bold text-white shadow-sm` |

---

## 8. Responsive behavior

| Breakpoint | Layout |
|------------|--------|
| Desktop ≥ 1024px | Three-column shell: left rail / content `.page-wrap` / right rail |
| Tablet 641–1023px | Left rail collapses to icons or hides behind menu; right rail hidden; content full width |
| Mobile ≤ 640px | See `mobile-ui.md`; nav wraps to second row; header search may tuck into menu |

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

1. **Sign in:** welcome → `/login` → email/username + password → token pair (access in memory, refresh in an HttpOnly cookie) → `/`.
2. **Create post:** composer → media picker (1..N images \| 1 video) → [Post] → feed insert with `rise-in`.
3. **Follow:** profile → [Follow] ghost → brand active; follower count derived (BR-2.3, never stored).
4. **Message:** `/messages` → open conversation → send → history cursor-paginated; unread badge via `last_read_message_id`.
5. **Moderate:** `/admin/reports` → review → action/dismiss → audit entry (append-only).
6. **Admin member action:** `/admin/members` → role/suspend/deactivate → audit entry; deactivated can't auth (FR-1.4).

---

## 11. Traceability matrix (web)

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
| Settings | FR-1.7, FR-7.3 | design.md §6 |

---

## 13. Implementation mapping (web → frontend/src)

| Spec section | Screen/component | Frontend file |
|--------------|------------------|---------------|
| §0/§3 Global shell | AppShell | `src/components/AppShell.tsx` |
| §3 Global shell | Header | `src/components/Header.tsx` (hidden on `/welcome`, `/login`) |
| §3 Global shell | LeftRail | `src/components/LeftRail.tsx` (bottom user chip opens AccountMenu) |
| §3 Global shell | AccountMenu | `src/components/AccountMenu.tsx` (Settings + Sign out popup) |
| §3 Global shell | RightRail | `src/components/RightRail.tsx` |
| §0 Explore | Suggested Topics panel | `src/routes/explore.tsx` |
| §0 Profile | Cover + About/Skills panels | `src/routes/profile.$username.tsx` |
| §0 Notifications | Tabs + items | `src/routes/notifications.tsx` |
| §0 Compose | Drag-drop upload area | `src/routes/compose.tsx` |
| §0 Messages | Two-pane list + thread | `src/routes/messages.tsx` |
| §4.1 Welcome | Welcome page | `src/routes/welcome.tsx` |
| §4.2 Login | Login page | `src/routes/login.tsx` |
| §5.1 Home feed | Home page | `src/routes/index.tsx` |
| §5.1.1 Post card | Post card | `src/components/PostCard.tsx` |
| §5.1 Composer | Composer | `src/components/Composer.tsx` |
| §5 Interaction notes | BusyOverlay (regional action overlay) | `src/components/BusyOverlay.tsx` |
| §5 Interaction notes | useBusy (busy state + re-entry guard) | `src/hooks/useBusy.ts` |
| §5.2 Explore / Watch | Explore page | `src/routes/explore.tsx` |
| §5.3 Notifications | Notifications page | `src/routes/notifications.tsx` |
| §5.4 Profile | Profile page | `src/routes/profile.\$username.tsx` |
| §5.5 Messages list | Messages list | `src/routes/messages.tsx` |
| §5.6 Messages thread | Message thread | `src/routes/messages.\$conversationId.tsx` |
| §5.7 Create post | Compose page | `src/routes/compose.tsx` |
| §5.8 Admin | Admin pages | *(not yet implemented — placeholder routes)* |
| §5.9 Member settings | Settings page | `src/routes/settings.tsx` |
| §7 Component library | Avatar | `src/components/Avatar.tsx` |
| §7 Component library | Theme toggle | `src/components/ThemeToggle.tsx` |
| §7 Component library | Footer | `src/components/Footer.tsx` |
| §7 Component library | Utilities | `src/lib/utils.ts` |
| §1 Design tokens | Token definitions | `src/styles.css` |
| — | Route tree (generated) | `src/routeTree.gen.ts` |
| — | Root shell | `src/routes/__root.tsx` |
| — | Router | `src/router.tsx` |

Tests: `src/components/Avatar.test.tsx`, `src/components/BusyOverlay.test.tsx`, `src/components/Composer.test.tsx`, `src/components/PostCard.test.tsx`, `src/hooks/useBusy.test.tsx`, `src/test/setupTests.ts`, `vitest.config.ts`

---

## 14. Open questions (confirm with owner)

1. Final IA: left rail vs top tabs for Feed · Watch · Messages · Notifications — resolved for web (rail) per design.md §13 #1; still open for tablet.
2. Right rail ("Who to follow"/"Trending") ships in v1 or behind a flag — discover feed on/off default (BR-12 #2).
3. "Join Chafgram today" card copy: private/invite-only default (BR-1.5) conflicts with public signup framing — confirm copy.
4. Brand mark for Chafgram (the rail's white "C" disc is a placeholder; design.md §14 #2).
5. Watch: vertical shorts vs 16:9 inline player vs both (design.md §14 #1).
6. Dark-mode watch view: pure `--bg-base` theater or lifted surface (design.md §14 #5).
7. Admin surface scope and which routes are `moderator`-visible vs `admin`-only.

---

*Related:* [`mobile-ui.md`](mobile-ui.md) · [`design.md`](design.md) · [`product.md`](../product.md) · [`docs/functional-requirements.md`](functional-requirements.md) · [`docs/business-rules.md`](business-rules.md) · [`docs/architecture.md`](architecture.md)

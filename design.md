# Chafgram — Design

> How Chafgram looks, feels, and behaves. This document defines the design system for the
> product UI (the `frontend/` app). It is grounded in the visual language already present in
> the codebase and extends it for the full product. Decisions not covered here should be
> made to be consistent with this document, then added to it.
>
> **Status & related docs.** This is the *system* (tokens, principles, components). The
> screen-by-screen contracts — wireframes plus what/where/how for every control — live in
> [`docs/web-ui.md`](docs/web-ui.md) and [`docs/mobile-ui.md`](docs/mobile-ui.md). Where this
> doc names a concrete value, that value is the one currently in `frontend/src/styles.css`;
> proposals are marked *(proposal)*. Member surfaces are implemented; admin surfaces are
> still mocked.

## 1. Design intent

Chafgram is a private social platform for companies. Its design must do two jobs at once:

1. **Feel like a place people want to be** — warm, organic, calm; the polish of a modern
   consumer social app, not the beige austerity of enterprise software.
2. **Reassure the buyer** — companies trust what looks deliberate, stable, and under
   control. No gimmicks, no dark patterns, no growth-hack chrome.

The member UI follows the supplied web references: a pale blue canvas, white surfaces,
navy text, and clear brand blue for actions. Fraunces display type and Manrope UI type keep
the interface calm and human; subtle blue shadows and soft corners provide depth without
making every surface feel like a floating card.

**One line:** *calm, clear, human — the internal social space that feels like a well-kept
harbor, not a public square* — with brand blue as the signal of "you can act here."

## 2. Principles

1. **Calm over loud.** Generous spacing, low-contrast surfaces, a clean canvas. Attention is
   earned by content, not by colour or motion.
2. **Editorial, not corporate.** Fraunces gives headlines a literary, considered voice; the
   app should not whisper "mandatory training."
3. **One idea per screen.** Every view has a single primary action or a single reading
   focus; secondary actions recede.
4. **Depth through light, not lines.** Surfaces are separated by translucency, a hairline,
   and soft shadow — never by heavy borders or boxes.
5. **Blue means action.** Brand blue marks the one thing you can do on a surface: the
   primary button, the active nav item, a link, an unread badge. Everything else is ink or
   neutral. See the **Blue Means Action Rule** (§5).
6. **Motion is tidal.** Gentle, decelerating, brief. Nothing bounces; nothing snaps.
7. **Accessible by default.** 4.5:1 text contrast, visible focus, reduced-motion support,
   keyboard-navigable everything.
8. **Light and dark are equal citizens.** Dark mode is not an inversion; it is a
   re-lit scene (see §6).

## 3. Voice & copy

- Plain, warm, direct. Short sentences. No exclamation marks unless celebrating something
  real.
- Address the member as "you"; refer to the company as "your company" / "your workspace".
- Admin copy is precise and action-oriented ("Deactivate member", "Export members").
  Member copy is conversational ("Share something with your workspace").
- Errors say what happened and what to do next; never a bare code without a path forward.
- Empty states invite action ("Nothing here yet — be the first to post") rather than
  apologizing.
- UI copy for the shipped screens is enumerated in [`docs/web-ui.md`](docs/web-ui.md) §4–§5
  and [`docs/mobile-ui.md`](docs/mobile-ui.md) §4–§5.

## 4. Typography

| Role | Typeface | Source | Usage |
|------|----------|--------|-------|
| Display / headings | **Fraunces** (variable, `opsz` 9–144, weights 500/700) | Google Fonts | Page titles, hero lines, editorial moments |
| Body / UI | **Manrope** (400–800) | Google Fonts | Everything else: nav, body, buttons, labels, captions |
| Code | system monospace stack | — | Inline `code`, only inside docs-like contexts |

Geist Variable is installed via `@fontsource-variable/geist` for shadcn-style
primitives in `src/components/ui/`; it is not the UI typeface — Manrope remains
the default (`--font-sans` in `styles.css`).

Rules:

- Display (`Fraunces`) is for short, high-impact lines only. Never body text, never UI
  labels.
- Body text is Manrope 400/500; emphasis is 600/700; buttons 700; kickers 700 uppercase
  with `0.16em` tracking at `0.69rem`.
- Scale (fluid where possible): kicker `0.69rem` · caption `0.75rem` · body `1rem` ·
  lead `1.125–1.25rem` · h2 `~1.25rem` · h1 `clamp(2.25rem, 4vw + 1rem, 3.75rem)` with
  `line-height ≈ 1.02–1.05`.
- `.display-title` (repo utility class) applies the serif; use it for h1-level moments.
- Link colour is `inherit` globally — components own their text colour. Never rely on the
  UA/default underline; make interactive text distinguishable by weight or colour.

## 5. Color system

Tokens are CSS custom properties on `:root` (light) and `:root[data-theme="dark"]`
(explicit dark), plus a `prefers-color-scheme` fallback for `auto`. Use token names, never
raw hex, in components.

### Light (default)

| Token | Value | Role |
|-------|-------|------|
| `--brand` | `#2563eb` | **Primary interactive** — filled buttons, active nav, unread badges, focus |
| `--brand-dark` | `#1d4ed8` | Brand hover/active state; link text |
| `--brand-light` | `#eff6ff` | Active nav wash, subtle brand surfaces |
| `--sea-ink` | `#14213d` | Primary navy text |
| `--sea-ink-soft` | `#51617d` | Secondary text, icons |
| `--lagoon` | `#78a9ff` | Supporting blue — focus washes and highlights |
| `--lagoon-deep` | `#2563eb` | Strong supporting blue |
| `--palm` | `#31558e` | Deep blue for kickers |
| `--sand` | `#e8eef9` | Pale blue neutral |
| `--foam` | `#f7f9fe` | Near-white cool surface |
| `--bg-base` | `#f4f6fb` | Page background base |
| `--surface` | `rgba(255,255,255,0.74)` | Card fill (translucent) |
| `--surface-strong` | `rgba(255,255,255,0.9)` | Elevated card fill |
| `--line` | `rgba(61,91,148,0.15)` | Hairline borders |
| `--kicker` | `#31558e` | Eyebrow text |
| `--header-bg` | `rgba(255,255,255,0.86)` | Sticky header (backdrop-blur) |
| `--chip-bg` / `--chip-line` | `rgba(255,255,255,0.8)` / `rgba(61,91,148,0.18)` | Pills, brand chip |
| `--hero-a` / `--hero-b` | `rgba(96,165,250,0.3)` / `rgba(37,99,235,0.14)` | Background washes (marketing/welcome only — see below) |
| `--inset-glint` | `rgba(255,255,255,0.82)` | Inner top highlight on shells |

### Dark

| Token | Value | Role |
|-------|-------|------|
| `--brand` / `--brand-dark` / `--brand-light` | *inherited from light* (`#2563eb` / `#1d4ed8` / `#eff6ff`) | Brand tokens are currently mode-agnostic; see §14 for the dark link-contrast follow-up |
| `--sea-ink` | `#e3ebff` | Primary text |
| `--sea-ink-soft` | `#b2c2e2` | Secondary text |
| `--lagoon` | `#78a9ff` | Supporting blue |
| `--lagoon-deep` | `#8db6ff` | Strong supporting blue |
| `--palm` | `#a6c4ff` | Lifted blue for kickers |
| `--sand` / `--foam` | `#111c31` / `#14213a` | Cool, near-black surfaces |
| `--bg-base` | `#091221` | Page background |
| `--surface` / `--surface-strong` | `rgba(20,34,58,0.82)` / `rgba(17,29,50,0.94)` | Cards |
| `--line` | `rgba(166,192,255,0.18)` | Hairline |
| `--kicker` | `#a6c4ff` | Eyebrow text |
| `--header-bg` | `rgba(9,18,33,0.86)` | Sticky header |
| `--chip-bg` / `--chip-line` | `rgba(20,34,58,0.9)` / `rgba(166,192,255,0.24)` | Pills |
| `--hero-a` / `--hero-b` | `rgba(96,165,250,0.18)` / `rgba(37,99,235,0.12)` | Background washes |
| `--inset-glint` | `rgba(206,222,255,0.12)` | Inner highlight |

### Background construction

The shipped member app uses a **flat, clean canvas**: `body` is `#f4f6fb` with no aurora
and no blueprint grid. Depth comes from the shells, not from wallpaper. The ambient
radial washes (`--hero-a` / `--hero-b`) and `body::before` / `body::after` effects belong to
**marketing, welcome, and login** surfaces only — add them locally to those surfaces, do not
restore them globally. *(The starter template originally applied them to `body`; that was
removed when the member UI was built.)*

**The Flat Canvas Rule.** Member screens are flat at rest. If a surface needs atmosphere,
scope the gradient to that component — never to `body`.

### Status colors *(proposal)*

Success → palm family; warning → amber (`rgba(193,126,42,…)` as used in `.demo-alert`);
danger → `#c44747` family (`rgba(196,71,71,…)`, text `#9f3030` light / lifted in dark);
info → brand blue. All status UI uses tinted backgrounds at ~10% alpha with matching 30% alpha
borders, never solid fills.

### Color hierarchy

**The Blue Means Action Rule.** Brand blue marks the single primary action on a surface —
filled button, active nav item, link, or unread badge. If everything is blue, nothing is.
Lagoon and palm are supporting: focus washes, kickers, ambient moments, and status. Never
use blue for static text or decoration.

## 6. Theming

- Modes: **light · dark · auto** (system). Persisted in `localStorage["theme"]`; resolved
  before paint by the inline `THEME_INIT_SCRIPT` in `__root.tsx` (no FOUC).
- `data-theme="light|dark"` on `<html>` selects explicit mode; absent + `prefers-color-scheme`
  drives auto. `colorScheme` is set on the root for form controls/scrollbars.
- Toggle cycles light → dark → auto (`ThemeToggle.tsx`).
- Every new component must be tested in all three modes; dark is a re-light, not a filter.

## 7. Layout & composition

- **Content column:** `.page-wrap` — `min(1080px, 100% - 2rem)`, centered. Wide canvases
  (tables, video grids) may extend to 1280px as `.demo-page-wide` does.
- **App shell (web):** three columns — a fixed `w-64` left rail (`LeftRail`), a fluid content
  column offset by `lg:pl-64`, and a `w-80` right rail (`RightRail`) shown at `lg` and up.
  The sticky header sits above the content region and is also offset by `lg:pl-64`.
- **Left rail:** brand chip + wordmark; nav links Home · Explore · Messages · Notifications
  · Profile · More with unread badges; bottom "Create Post" pill and the tagline
  "Connect. Share. Belong." Active item uses `--brand-light` background + `--brand-dark`
  text; inactive uses `--sea-ink-soft`.
- **Right rail:** Who to follow · Trending · Join card. Hidden below `lg` so the feed stays
  readable on tablet.
- **Mobile shell:** single column with a fixed bottom tab bar (Home · Explore · `+` ·
  Messages · Profile), 44px+ targets and safe-area insets. See `docs/mobile-ui.md` §3.
- **Shells:** `.island-shell` — 1px `--line` border, `165deg` gradient from
  `--surface-strong` to `--surface`, inner `--inset-glint` top highlight, two-layer soft
  blue shadow, `blur(4px)`. Corner radii: screen shells `0.75rem`; nested cards `0.75rem`.
- **Spacing rhythm:** 4/8 base; section gaps 2rem (`mt-8`); shell padding 1.5rem → 2.5rem
  responsive; page gutters 1rem.
- **Hero pattern:** eyebrow kicker → display-title (max ~3xl lines) → lead paragraph →
  pill-shaped CTA row (primary brand pill; secondary ghost pill). Two ambient radial
  glows clipped inside the shell (`-left-20 -top-24` style), pointer-events none — welcome
  and login only.
- **Feature grids:** 2×2 on `sm`, 4-up on `lg`; each cell an `.island-shell.feature-card`
  with hover lift (`translateY(-2px)`) and lagoon border-mix.

## 8. Components

| Component | Spec |
|-----------|------|
| Brand chip | `rounded-full border border-[var(--chip-line)] bg-[var(--chip-bg)]`; in the app rail the mark is a `h-8 w-8 rounded-full bg-[var(--brand)]` disc with a white "C" and a soft brand shadow |
| Nav link (rail) | `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium`; active `bg-[var(--brand-light)] text-[var(--brand-dark)] font-semibold`; inactive `--sea-ink-soft`; badge is a brand-filled circle |
| Button (primary) | `rounded-full border border-[var(--brand)] bg-[var(--brand)] px-5 py-2.5 text-sm font-bold text-white shadow-lg shadow-[var(--brand)]/30`; hover `--brand-dark`. `.demo-button` (tonal lagoon) remains for legacy form contexts |
| Button (secondary) | transparent/ghost, `--sea-ink`, hover `--link-bg-hover` |
| Card / post | `.island-shell rounded-xl overflow-hidden border border-[var(--line)] bg-white shadow-sm`; header, body, media, interaction bar, action row |
| Composer | `.island-shell rounded-xl`; prompt row → `--line` divider → icon chips [Photo][Video][Feeling][Poll] + solid brand Post button |
| Pill / tag | `.demo-pill` — rounded-full, chip tokens, 0.75rem 700 |
| Input | `.demo-input` — 0.75rem radius, `--line` border, focus `--lagoon-deep` border + 3px `--lagoon` 24% ring *(legacy; new inputs use `--brand` focus — migrate on touch)* |
| Alert | `.demo-alert` tinted amber/red variants, 1rem radius |
| Table | `.demo-table-shell` overflow-x; header `chip-bg`; row hover `lagoon` 8% |
| Bottom tab bar | fixed, five destinations, active `--brand` icon + label, `--sea-ink-soft` inactive, safe-area inset |
| Empty state | kicker + one line of guidance + one action; never a dead end |
| Toast *(proposal)* | bottom-center, `surface-strong`, `--line` border, auto-dismiss 4s |

Per-component what/where/how detail lives in [`docs/web-ui.md`](docs/web-ui.md) §7 and
[`docs/mobile-ui.md`](docs/mobile-ui.md) §6.

## 9. Motion

- **Entrance:** `.rise-in` — 700ms `cubic-bezier(0.16,1,0.3,1)`, `translateY(12px) + fade`,
  staggered ~90ms per sibling.
- **Hover/press:** 180ms `ease` on background/color/border/transform; lifts ≤ 2px.
- **Nav active state:** instant pill background/colour change (no sliding underline in the
  rail pattern); the legacy `.nav-link` underline remains for horizontal top-nav contexts.
- **Global:** interactive elements transition `background-color, color, border-color,
  transform` 180ms ease (see `styles.css` catch-all).
- **Rules:** no springs/bounce; no parallax; durations ≤ 700ms; everything must respect
  `prefers-reduced-motion: reduce` (disable `rise-in`, keep only instant state changes).
  Infinite motion (spinners, pulsing dots) only for genuinely indeterminate states.

## 10. Accessibility

- Text contrast ≥ 4.5:1 (tokens above satisfy this on their intended surfaces); large
  display type ≥ 3:1.
- Visible focus: 3px `--lagoon` 24% ring on legacy inputs; 3px `--brand` 24% ring on new
  inputs; equivalent `:focus-visible` outline on all interactive elements — never
  `outline: none` without a replacement.
- Icons that carry meaning have `aria-label` (e.g. theme toggle announces current mode);
  decorative icons `aria-hidden`.
- Touch targets ≥ 44px on mobile; nav wraps to a second row at ≤ 640px where a horizontal
  nav is used.
- Colour is never the only signal (status pills pair tint + text; unread pairs badge + text).
- Reduced motion honored (§9). Document language `en` is set on `<html>`.

## 11. Iconography & imagery

- Stroke icons, 1.5px, round caps/joins; 16–24px grid; currentColor. (X/GitHub brand glyphs
  in header/footer are filled exceptions.)
- Avatars: circular, `--line` ring; without an image, show **one** uppercase initial of
  the name on a stable tint derived from that name (`--avatar-tint-1`…`--avatar-tint-6`
  in `styles.css`, computed in `src/lib/avatar.ts`). Sizes 24/32/40/56. Do not point `src`
  at a placeholder file that does not exist — omit `src` so the initial renders.
- Imagery (video thumbnails, post media) is always user content; the UI never fabricates
  stock photography. Placeholders are inline SVG gradients or tonal lagoon/sand fills with
  a centred glyph (`src/lib/placeholders.ts`), never remote URLs that render blank offline.

## 12. Do / Don't

**Do**
- Reach for tokens; add a new token before adding a new raw value.
- Use brand blue for the one primary action on a surface (§5 Blue Means Action Rule).
- Keep shells floating on the flat canvas; let content breathe.
- Use kickers + display type to establish hierarchy on every new surface.
- Test every component in light, dark, and auto.

**Don't**
- Don't paint blue onto static text or decoration; it is an action signal.
- Don't use pure black/white (`#000`/`#fff`) text; use `--sea-ink` / `--foam`.
- Don't stack heavy shadows or borders; if a divider is needed, `--line` hairline only.
- Don't restore the starter aurora/grid to `body` (§5 Flat Canvas Rule).
- Don't use Fraunces below ~1.25rem.
- Don't animate layout properties (width/height/top); transform and opacity only.
- Don't put admin-only jargon in member-facing copy.
- Don't rely on external image URLs for mock UI; generate local SVG placeholders.

## 13. Applying this to the product (current state)

The member-facing UI is implemented in `frontend/src/` as UI-only screens (mock data, no
backend connection). It follows this system as follows:

1. **Shell → app shell (done).** Sticky header + fixed left rail + content column + optional
   right rail on web; bottom tab bar on mobile. `AppShell` provides the shared rail/offset.
   The left rail ends with a user chip; the header and footer are hidden on public pages.
   The rail-vs-tabs IA question is resolved for web; mobile keeps the bottom bar.
2. **Feed (done).** Composer + reverse-chronological post cards in `.island-shell` cards;
   media is locally generated SVG; video posts will open an immersive watch view later.
3. **Explore / Profile / Notifications / Messages (done).** Explore has a Suggested Topics
   panel; Profile has a cover banner with About/Skills panels; Notifications has type tabs;
   Messages is a two-pane list + thread.
4. **Compose (done).** Dedicated `/compose` route (mobile: bottom sheet); two-column card
   with a dashed drag-and-drop upload area; one primary "Post"; video upload shows progress
   inline when wired to the backend.
5. **Auth / landing (done).** `/welcome` is a full-viewport marketing card with its own nav,
   hero, and illustration; `/login` is passkey-first with password fallback. Both use the
   ambient hero treatment scoped to those pages (app chrome hidden).
6. **Admin console (not yet implemented).** Same tokens, denser layout, `.demo-table`
   patterns; clearly separated from member surfaces.
7. **Empty/loading/error states** follow §8; skeletons use `sand`/`foam` shimmer, never
   spinners for content regions.
8. **What stays:** the token set, the flat canvas, `island-shell`, `rise-in`, theme toggle,
   Fraunces/Manrope pairing — these are the brand.

Screen contracts: [`docs/web-ui.md`](docs/web-ui.md) and [`docs/mobile-ui.md`](docs/mobile-ui.md).

## 14. Open questions

1. Watch experience: vertical shorts vs. 16:9 inline player vs. both.
2. Brand mark for Chafgram (the rail's white "C" disc is a placeholder); favicon/app icons.
3. Whether the marketing/landing page (if any) shares this system or gets its own.
4. Density preference for admin tables and data-heavy views.
5. Dark-mode watch view: pure `--bg-base` or lifted surface for the theater.
6. **Dark-mode brand link contrast.** `--brand*` tokens are currently mode-agnostic;
   `--brand-dark` (`#1d4ed8`) on a dark surface is low contrast for link text. Introduce a
   lifted `--brand-link` token for text links in dark mode (keep `--brand` for filled
   buttons, whose white text needs the deeper blue), then migrate `text-[var(--brand-dark)]`
   usages. *(Resolved in doc as follow-up; not yet in code.)*
7. Legacy `.demo-input` still uses a lagoon focus ring while new inputs use brand — migrate
   the utility to `--brand` for consistency.

---

*Related:* [`product.md`](product.md) · [`docs/web-ui.md`](docs/web-ui.md) ·
[`docs/mobile-ui.md`](docs/mobile-ui.md) · [`docs/architecture.md`](docs/architecture.md)

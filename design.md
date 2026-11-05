# Chafgram — Design

> How Chafgram looks, feels, and behaves. This document defines the design system for the
> product UI (the `frontend/` app). It is grounded in the visual language already present in
> the codebase and extends it for the full product. Decisions not covered here should be
> made to be consistent with this document, then added to it.

## 1. Design intent

Chafgram is a private social platform for companies. Its design must do two jobs at once:

1. **Feel like a place people want to be** — warm, organic, calm; the polish of a modern
   consumer social app, not the beige austerity of enterprise software.
2. **Reassure the buyer** — companies trust what looks deliberate, stable, and under
   control. No gimmicks, no dark patterns, no growth-hack chrome.

The existing identity — a deep-sea, lagoon-and-palm palette, editorial serif display type
paired with a friendly grotesque — already does this. Chafgram keeps and evolves it; it is
not replaced.

**One line:** *calm, tidal, human — the internal social space that feels like a well-kept
harbor, not a public square.*

## 2. Principles

1. **Calm over loud.** Soft gradients, generous spacing, low-contrast surfaces. Attention
   is earned by content, not by color or motion.
2. **Editorial, not corporate.** Fraunces gives headlines a literary, considered voice;
   Enterprise UI should not whisper "mandatory training."
3. **One idea per screen.** Every view has a single primary action or a single reading
   focus; secondary actions recede.
4. **Depth through light, not lines.** Surfaces are separated by translucency, blur, and
   soft shadow — never by heavy borders or boxes.
5. **Motion is tidal.** Gentle, decelerating, brief. Nothing bounces; nothing snaps.
6. **Accessible by default.** 4.5:1 text contrast, visible focus, reduced-motion support,
   keyboard-navigable everything.
7. **Light and dark are equal citizens.** Dark mode is not an inversion; it is a
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

## 4. Typography

| Role | Typeface | Source | Usage |
|------|----------|--------|-------|
| Display / headings | **Fraunces** (variable, `opsz` 9–144, weights 500/700) | Google Fonts | Page titles, hero lines, editorial moments |
| Body / UI | **Manrope** (400–800) | Google Fonts | Everything else: nav, body, buttons, labels, captions |
| Code | system monospace stack | — | Inline `code`, only inside docs-like contexts |

Rules:

- Display (`Fraunces`) is for short, high-impact lines only. Never body text, never UI
  labels.
- Body text is Manrope 400/500; emphasis is 600/700; buttons 700; kickers 700 uppercase
  with `0.16em` tracking at `0.69rem`.
- Scale (fluid where possible): kicker `0.69rem` · caption `0.75rem` · body `1rem` ·
  lead `1.125–1.25rem` · h2 `~1.25rem` · h1 `clamp(2.25rem, 4vw + 1rem, 3.75rem)` with
  `line-height ≈ 1.02–1.05`.
- `.display-title` (repo utility class) applies the serif; use it for h1-level moments.

## 5. Color system

Tokens are CSS custom properties on `:root` (light) and `:root[data-theme="dark"]`
(explicit dark), plus a `prefers-color-scheme` fallback for `auto`. Use token names, never
raw hex, in components.

### Light (default)

| Token | Value | Role |
|-------|-------|------|
| `--sea-ink` | `#173a40` | Primary text (deep sea slate) |
| `--sea-ink-soft` | `#416166` | Secondary text, icons |
| `--lagoon` | `#4fb8b2` | Accent — links, highlights, focus washes |
| `--lagoon-deep` | `#328f97` | Accent strong — link text, active states |
| `--palm` | `#2f6a4a` | Deep green — kickers, brand gradient end |
| `--sand` | `#e7f0e8` | Warm neutral base |
| `--foam` | `#f3faf5` | Near-white green-tinted surface |
| `--bg-base` | `#e7f3ec` | Page background base |
| `--surface` | `rgba(255,255,255,0.74)` | Card fill (translucent) |
| `--surface-strong` | `rgba(255,255,255,0.9)` | Elevated card fill |
| `--line` | `rgba(23,58,64,0.14)` | Hairline borders |
| `--kicker` | `rgba(47,106,74,0.9)` | Eyebrow text |
| `--header-bg` | `rgba(251,255,248,0.84)` | Sticky header (backdrop-blur) |
| `--chip-bg` / `--chip-line` | `rgba(255,255,255,0.8)` / `rgba(47,106,74,0.18)` | Pills, brand chip |
| `--hero-a` / `--hero-b` | `rgba(79,184,178,0.36)` / `rgba(47,106,74,0.2)` | Background washes |
| `--inset-glint` | `rgba(255,255,255,0.82)` | Inner top highlight on shells |

### Dark

| Token | Value | Role |
|-------|-------|------|
| `--sea-ink` | `#d7ece8` | Primary text |
| `--sea-ink-soft` | `#afcdc8` | Secondary text |
| `--lagoon` | `#60d7cf` | Accent (lifted for dark) |
| `--lagoon-deep` | `#8de5db` | Accent strong |
| `--palm` | `#6ec89a` | Lifted palm |
| `--sand` / `--foam` | `#0f1a1e` / `#101d22` | Neutrals (near-black green) |
| `--bg-base` | `#0a1418` | Page background |
| `--surface` / `--surface-strong` | `rgba(16,30,34,0.8)` / `rgba(15,27,31,0.92)` | Cards |
| `--line` | `rgba(141,229,219,0.18)` | Hairline |
| `--kicker` | `#b8efe5` | Eyebrow text |
| `--header-bg` | `rgba(10,20,24,0.8)` | Sticky header |
| `--chip-bg` / `--chip-line` | `rgba(13,28,32,0.9)` / `rgba(141,229,219,0.24)` | Pills |
| `--hero-a` / `--hero-b` | `rgba(96,215,207,0.18)` / `rgba(110,200,154,0.12)` | Background washes |
| `--inset-glint` | `rgba(194,247,238,0.14)` | Inner highlight |

### Background construction

`body` layers, in order: two large radial washes (`--hero-a` top-left, `--hero-b`
top-right), a faint lagoon wash at the bottom center, then a vertical
`sand → foam → bg-base` gradient; plus a `body::before` aurora (three soft radial
glows, opacity 0.28) and a `body::after` blueprint grid (28px, masked to fade by 78%
radial). Dark mode re-lights the same structure with lower-opacity teal glows. Do not
add busy patterns; the atmosphere should read as weather, not wallpaper.

### Status colors *(proposal)*

Success → palm family; warning → amber (`rgba(193,126,42,…)` as used in `.demo-alert`);
danger → `#c44747` family (`rgba(196,71,71,…)`, text `#9f3030` light / lifted in dark);
info → lagoon. All status UI uses tinted backgrounds at ~10% alpha with matching 30% alpha
borders, never solid fills.

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
- **Shells:** `.island-shell` — 1px `--line` border, `165deg` gradient from
  `--surface-strong` to `--surface`, inner `--inset-glint` top highlight, two-layer soft
  shadow (`0 22px 44px rgba(30,90,72,0.1)` + `0 6px 18px rgba(23,58,64,0.08)`), `blur(4px)`.
  Corner radii: shells `1rem–2rem`; cards `0.9–1rem`.
- **Spacing rhythm:** 4/8 base; section gaps 2rem (`mt-8`); shell padding 1.5rem → 2.5rem
  responsive; page gutters 1rem.
- **Hero pattern:** eyebrow kicker → display-title (max ~3xl lines) → lead paragraph →
  pill-shaped CTA row (primary tonal lagoon pill; secondary ghost pill). Two ambient radial
  glows clipped inside the shell (`-left-20 -top-24` style), pointer-events none.
- **Feature grids:** 2×2 on `sm`, 4-up on `lg`; each cell an `.island-shell.feature-card`
  with hover lift (`translateY(-2px)`) and lagoon border-mix.
- **Sticky header:** blur backdrop, hairline bottom border, brand chip (rounded-full,
  pulsing two-tone dot), left nav links with animated underline (`scaleX 0→1`), right
  utility icons + theme toggle.

## 8. Components

| Component | Spec |
|-----------|------|
| Brand chip | rounded-full, `chip-bg` + `chip-line`, dot `linear-gradient(90deg,#56c6be,#7ed3bf)` |
| Nav link | `--sea-ink-soft`, underline 2px gradient `lagoon → #7ed3bf`, `scaleX` transition 170ms |
| Button (primary) | rounded-full tonal lagoon (`rgba(79,184,178,0.14→0.24)` hover), `lagoon-deep` text, or `.demo-button` for form contexts |
| Button (secondary) | transparent/ghost, `--sea-ink`, hover `--link-bg-hover` |
| Card | `.island-shell`; hover only if interactive |
| Pill / tag | `.demo-pill` — rounded-full, chip tokens, 0.75rem 700 |
| Input | `.demo-input` — 0.75rem radius, `--line` border, focus `lagoon-deep` border + 3px `lagoon` 24% ring |
| Alert | `.demo-alert` tinted amber/red variants, 1rem radius |
| Table | `.demo-table-shell` overflow-x; header `chip-bg`; row hover `lagoon` 8% |
| Empty state | kicker + one line of guidance + one action; never a dead end |
| Toast *(proposal)* | bottom-center, `surface-strong`, `--line` border, auto-dismiss 4s |

## 9. Motion

- **Entrance:** `.rise-in` — 700ms `cubic-bezier(0.16,1,0.3,1)`, `translateY(12px) + fade`,
  staggered ~90ms per sibling.
- **Hover/press:** 180ms `ease` on background/color/border/transform; lifts ≤ 2px.
- **Nav underline:** 170ms `ease`, `scaleX` from left.
- **Global:** interactive elements transition `background-color, color, border-color,
  transform` 180ms ease (see `styles.css` catch-all).
- **Rules:** no springs/bounce; no parallax; durations ≤ 700ms; everything must respect
  `prefers-reduced-motion: reduce` (disable `rise-in`, keep only instant state changes).
  Infinite motion (spinners, pulsing dots) only for genuinely indeterminate states.

## 10. Accessibility

- Text contrast ≥ 4.5:1 (tokens above satisfy this on their intended surfaces); large
  display type ≥ 3:1.
- Visible focus: 3px `lagoon` 24% ring on inputs; equivalent `:focus-visible` outline on
  all interactive elements — never `outline: none` without a replacement.
- Icons that carry meaning have `aria-label` (e.g. theme toggle announces current mode);
  decorative icons `aria-hidden`.
- Touch targets ≥ 44px on mobile; nav wraps to a second row at ≤ 640px (existing pattern).
- Color is never the only signal (status pills pair tint + text).
- Reduced motion honored (§9). Document language `en` is set on `<html>`.

## 11. Iconography & imagery

- Stroke icons, 1.5px, round caps/joins; 16–24px grid; currentColor. (X/GitHub brand glyphs
  in header/footer are filled exceptions.)
- Avatars: circular, `--line` ring, initials fallback on `shell`-toned fill; sizes
  24/32/40/56.
- Imagery (video thumbnails, post media) is always user content; the UI never fabricates
  stock photography. Placeholders are tonal lagoon/sand gradients with a centered glyph.

## 12. Do / Don't

**Do**
- Reach for tokens; add a new token before adding a new raw value.
- Keep shells floating on the ambient background; let content breathe.
- Use kickers + display type to establish hierarchy on every new surface.
- Test every component in light, dark, and auto.

**Don't**
- Don't introduce a third accent hue; lagoon + palm carry the brand.
- Don't use pure black/white (`#000`/`#fff`) text; use `--sea-ink` / `--foam`.
- Don't stack heavy shadows or borders; if a divider is needed, `--line` hairline only.
- Don't use Fraunces below ~1.25rem.
- Don't animate layout properties (width/height/top); transform and opacity only.
- Don't put admin-only jargon in member-facing copy.

## 13. Applying this to the product (evolution)

The current app is the starter shell; the product builds on it as follows:

1. **Shell → App shell.** Header gains workspace identity (company logo/name slot, member
   avatar menu); footer stays minimal. Left rail or top tabs for Feed · Watch · Messages ·
   Notifications — final pattern to be prototyped before committing.
2. **Feed.** Single-column `page-wrap` list of `island-shell` posts; video posts open an
   immersive watch view (dark-first by design).
3. **Compose.** Modal or dedicated route; one primary action ("Post"); video upload shows
   progress inline.
4. **Admin console.** Same tokens, denser layout, `.demo-table` patterns; clearly separated
   from member surfaces.
5. **Empty/loading/error states** follow §8; skeletons use `sand`/`foam` shimmer, never
   spinners for content regions.
6. **What stays:** the token set, the background atmosphere, `island-shell`, `rise-in`,
   nav underline, theme toggle, Fraunces/Manrope pairing — these are the brand.

## 14. Open questions

1. Final information architecture for the member shell (rail vs. top tabs) — needs a
   prototype pass.
2. Watch experience: vertical shorts vs. 16:9 inline player vs. both.
3. Brand mark for Chafgram (current dot is a placeholder); favicon/app icons.
4. Whether the marketing/landing page (if any) shares this system or gets its own.
5. Density preference for admin tables and data-heavy views.
6. Dark-mode watch view: pure `--bg-base` or lifted surface for the theater.

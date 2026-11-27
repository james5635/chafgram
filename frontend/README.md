# Chafgram — Frontend

TanStack Start (React 19, Vite, Tailwind v4) app for Chafgram, the private social platform
for companies. This package is **UI-only** at the moment — screens use mock data and are not
connected to the backend yet.

## Getting started

```bash
pnpm install
pnpm run dev       # dev server on port 3000
```

```bash
pnpm run build     # production build (also regenerates the route tree)
pnpm run preview   # serve the production build
pnpm run test      # Vitest + React Testing Library
```

See `docs/web-ui.md` and `docs/mobile-ui.md` for the complete UI specification (wireframes
plus what/where/how for every component), and `design.md` for tokens and motion.

## Routes

Routes are files in `src/routes/`; `src/routeTree.gen.ts` is **generated — never edit it**
(it regenerates on dev/build). Add a route by adding a file.

| Path | Screen |
|------|--------|
| `/welcome` | Welcome / landing |
| `/login` | Password sign in (username or email) |
| `/` | Home feed (composer + posts) |
| `/explore` | Explore / Watch |
| `/notifications` | Activity list |
| `/profile/$username` | Member profile |
| `/messages` | Conversation list |
| `/messages/$conversationId` | Message thread |
| `/compose` | Create post |
| `/settings` | Member settings |

Use TanStack Router's `Link` for client-side navigation:

```tsx
import { Link } from '@tanstack/react-router'

<Link to="/explore">Explore</Link>
```

The root layout is `src/routes/__root.tsx`; anything there appears on every route. It renders
the header, the route content, the footer, and the pre-paint theme script.

## Structure

- `src/routes/` — file-based routes (keep these thin)
- `src/components/` — shared UI: `Header`, `Footer`, `LeftRail`, `RightRail`, `Avatar`,
  `Composer`, `PostCard`, `ThemeToggle`
- `src/lib/` — helpers (`cn`, local SVG placeholders)
- `src/styles.css` — Tailwind v4 + design tokens
- `src/test/` — Vitest setup

## Styling

Tailwind v4 (CSS-first via `@tailwindcss/vite`) plus design tokens in `src/styles.css`.
Use tokens (`--brand`, `--sea-ink`, `--lagoon`, …) rather than raw hex. Light/dark/auto
theming is resolved before paint by the inline `THEME_INIT_SCRIPT` in `__root.tsx`.

## Notes

- Devtools (TanStack Router/Devtools) render in the corner in dev only — the build plugin
  strips them from production output. Don't add them to production code paths.
- `pnpm run build` regenerates `src/routeTree.gen.ts`; commit the regenerated file.

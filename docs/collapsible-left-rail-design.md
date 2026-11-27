# Collapsible left rail design

Date: 2026-11-27
Status: implemented

## Intent

Give the viewer a way to collapse the fixed left navigation rail to an
icon-only strip and expand it again, freeing horizontal space for the content
column while keeping navigation one click away.

## Goals

- New shared state: `frontend/src/hooks/useRail.tsx` exporting `RailProvider`
  and `useRail()` with `{ collapsed, toggle }`, persisted in
  `localStorage['rail-collapsed']`. Default expanded; saved value applied after
  hydration.
- Provider wrapped around Header + route content + Footer in
  `frontend/src/routes/__root.tsx`, so all three consumers share one state.
- `LeftRail` is `w-64` expanded and `w-16` collapsed. Collapsed shows only
  icons, unread badges, the Create Post icon button and the avatar; every
  icon-only control keeps an accessible name (`aria-label`).
- `Header` gets a toggle button (Panel Left icon) with a dynamic label
  ("Collapse navigation" / "Expand navigation"), and changes its brand/search
  padding to match the rail width.
- `AppShell` changes its content offset from `lg:pl-64` to `lg:pl-16` when
  collapsed.
- `AccountMenu` accepts `collapsed?: boolean` and renders only the avatar plus
  "Account menu" label in that mode; the menu itself is unchanged.
- Tests for the hook, rail, header, AppShell and collapsed account menu;
  `just verify` green.

## Non-goals / anti-goals

- No mobile/tablet IA redesign — the strip follows the same breakpoints as
  today's rail.
- No animation beyond existing transition utilities; no dragging or resizing.
- No changes to navigation destinations, menus, or the right rail.
- No new dependencies.

## Constraints / decisions

- Toggle placement is in the header, consistently visible in both modes; I
  chose it because the brand area already reserves space there.
- Collapsed state persists via `localStorage`, following the existing theme
  pattern; a brief expanded-first paint on reload is acceptable (no new
  inline head script).
- Tailwind tokens and existing icon set (lucide); focus-visible styles on the
  toggle and icon buttons.
- The right rail and content column keep their existing widths; only the left
  offset changes.

## Approaches considered

1. **Shared context/hook (chosen)** — single source of truth across Header,
   AppShell and LeftRail; easy to test with a controlled provider.
2. **CSS variable on `<html>`** — less React plumbing but harder SSR/hydration
   reasoning and the toggle still needs React state.
3. **`AppShell` props** — cannot reach `Header` (rendered in `__root.tsx`)
   without context or a DOM hack.

## Left to the builder

- Exact icon-only padding and badge placement, provided the strip is `w-16`
  and all controls remain labelled and keyboard reachable.
- Whether the toggle uses `PanelLeft` / `PanelLeftClose` or one icon with a
  rotated state; accessible name is required either way.

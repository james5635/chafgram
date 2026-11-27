# Account menu design

Date: 2026-11-27
Status: implemented

The repo ignores `docs/superpowers/`, so this short design is tracked under `docs/`.

## Intent

Clicking the avatar chip at the bottom-left of the navigation rail currently
navigates to the profile page and shows no menu. Replace that link with an
account menu containing exactly two actions: **Settings** and **Sign out**.
Remove the "More" nav item (the only other visible link to `/settings`); the
settings page itself stays unchanged.

## Goals

- New `frontend/src/components/AccountMenu.tsx` owns the open state, the avatar
  trigger, and the menu contents. `LeftRail` renders it in place of the current
  avatar link and drops `More` from `navLinks`.
- The menu shows only Settings (link to `/settings`) and Sign out (button).
- Sign out calls the existing `logoutRequest()` server function. The BFF already
  sets the re-login response header on every outcome, and `useSessionGuard`
  already navigates to `/login` on that signal — reuse it, do not add a second
  auth flow.
- Re-entry guard on Sign out so rapid clicks cannot fire two logout requests.
- Colocated Vitest/RTL tests for the menu and updated shell tests. `just verify`
  green.

## Non-goals / anti-goals

- No "Your profile" item in the menu — the rail still has a Profile link.
- No changes to `/settings`, auth/BFF token handling, or the backend.
- No global menu state, no modal, no focus trap, no full-screen overlay.
- No new dependencies.

## Constraints and decisions

- Use the existing avatar chip content (avatar, display name, `@username`,
  chevron), now inside a `<button type="button">` labelled "Account menu" with
  `aria-expanded`. The chevron becomes the open indicator.
- The popup sits above the trigger in a `relative` parent, aligned to the rail,
  using existing token surfaces and rounded corners. No raw hex.
- Use a labelled container with real link/button controls rather than
  `role="menu"`: standard Tab/Enter/Space behavior then works without custom
  arrow-key roving. Escape closes and returns focus to the trigger; outside
  click closes; activating an item closes.
- Sign out uses `useBusy().run` as the re-entry guard and disables both items
  while the request is in flight, with the button text changing to
  "Signing out…". No `BusyOverlay`: the menu is small, the request ends in a
  navigation, and the overlay's approved label set has no sign-out label.
- Keep the current hydration-safe display name/username behavior from
  `LeftRail` (no hardcoded member).

## Approaches considered

1. **Inline state in `LeftRail`** — fewest files, but mixes menu behavior with
   navigation and forces shell-level tests. Rejected.
2. **Separate `AccountMenu` component (chosen)** — testable in isolation, keeps
   the rail declarative, and leaves a clean seam if the menu grows.
3. **Native `<details>`** — still needs JavaScript for outside click and Escape,
   and is harder to style consistently. Rejected.

## Left to the builder

- Exact popup width/spacing classes, as long as tokens are used and the menu
  stays inside the fixed 16rem rail.
- Whether to close the menu on route change (recommended: yes, via
  `useLocation`), provided the tests cover it.

# Account Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bottom-left avatar link with an account menu offering only Settings and Sign out, and remove the "More" nav item.

**Architecture:** A new `AccountMenu` component owns open state, the avatar trigger, the two items, and sign-out handling. `LeftRail` stays declarative: it keeps the `me` query and passes display name and username as props. Sign out reuses the existing `logoutRequest` server function and the auth signed-out signal that navigates to `/login`.

**Tech Stack:** React 19, TypeScript strict, TanStack Router, Tailwind v4 tokens, Vitest + React Testing Library.

**Spec:** `docs/account-menu-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- New component: `frontend/src/components/AccountMenu.tsx` with signature `AccountMenu({ displayName, username }: { displayName: string; username: string })`.
- Menu items: exactly **Settings** (link to `/settings`) and **Sign out** (button). No "Your profile" item.
- Trigger: `<button type="button">` with accessible name "Account menu" and `aria-expanded` reflecting open state. The avatar chip content (avatar, display name, `@username`, chevron) stays.
- Popup: labelled container with real link/button controls (no `role="menu"`), positioned above the trigger in a `relative` parent, existing token surfaces/rounded corners, no raw hex.
- Close on Escape (return focus to trigger), outside click, and item activation; recommended: also close on route change via `useLocation`.
- Sign out: call `logoutRequest({})`, then `clearAccessToken()` in `finally`; swallow errors so no unhandled rejection. Re-entry guarded by `useBusy().run`; items disabled while busy; button text "Signing out…". No `BusyOverlay`.
- Remove `More` and the `MoreHorizontal` import from `LeftRail`; keep the Profile nav link.
- No new dependencies; no hardcoded member names; hydration-safe name handling stays in `LeftRail`.
- Colocated Vitest/RTL tests; `just verify` green.

## Review Focus

1. Rapid double-click on Sign out must fire exactly one logout request — pinned in Task 1 (`useBusy` re-entry).
2. A failing logout request must not leave busy state stuck or an unhandled rejection — pinned in Task 1.
3. Escape closes the menu and returns focus to the trigger; an outside pointer click closes it — pinned in Task 1.
4. Removing "More" must not remove the only route to Settings — pinned in Task 2 (menu contains a `/settings` link and no "More" nav item remains).
5. The chip must still show the signed-in member from `me`, not a hardcoded name — covered by existing `shell.test.tsx`, asserted again in Task 2.

---

### Task 1: AccountMenu component

**Files:**
- Create: `frontend/src/components/AccountMenu.tsx`
- Test: `frontend/src/components/AccountMenu.test.tsx`

**Interfaces:**
- Consumes: `useBusy()` from `../hooks/useBusy` → `{ busy, run }`; `logoutRequest` from `../lib/session-server`; `clearAccessToken` from `../lib/auth-client`; `Link`, `useLocation` from `@tanstack/react-router`; `Avatar`, `AvatarFallback` from `@/components/ui/avatar`.
- Produces: `AccountMenu({ displayName, username }: { displayName: string; username: string })` consumed by Task 2.

- [ ] **Step 1: Write failing tests**

Mock `@tanstack/react-router` (`Link` as an anchor forwarding props, `useLocation` with a controllable pathname), `../lib/session-server` (`logoutRequest` as a controllable promise), and `../lib/auth-client` (`clearAccessToken` as a spy). Tests:
  - trigger renders with name "Account menu", `aria-expanded="false"`, and no menu items initially;
  - clicking the trigger shows a Settings link with `href="/settings"` and a Sign out button, and `aria-expanded="true"`;
  - Escape closes the menu and focus returns to the trigger;
  - a pointerdown outside the menu closes it;
  - clicking the Settings link closes the menu;
  - two rapid Sign out clicks call `logoutRequest` exactly once; while pending, Sign out and Settings are disabled and Sign out reads "Signing out…"; after resolution, `clearAccessToken` was called once;
  - a rejected `logoutRequest` still calls `clearAccessToken` once and does not leave items disabled.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/AccountMenu.test.tsx`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement the component**

Implement the exact signature from Interfaces. Keep the popup as a sibling of the trigger inside a `relative` wrapper; use `useEffect` for document `pointerdown` and `keydown` listeners while open, plus a `useLocation` pathname effect that closes on navigation. Sign out handler calls `void run(async () => { try { await logoutRequest({}) } finally { clearAccessToken() } })`.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/AccountMenu.tsx frontend/src/components/AccountMenu.test.tsx
git commit -m "feat(frontend): add account menu with settings and sign out"
```

---

### Task 2: Wire the menu into LeftRail and remove "More"

**Files:**
- Modify: `frontend/src/components/LeftRail.tsx`
- Test: `frontend/src/components/shell.test.tsx`

**Interfaces:**
- Consumes: `AccountMenu({ displayName, username })` from Task 1.
- Produces: no new cross-task interface.

- [ ] **Step 1: Write failing tests**

In `shell.test.tsx`: assert there is no link named "More", no link named "Your profile", and a button named "Account menu" is present; clicking it reveals Settings and Sign out; the "Profile" nav link still exists. Keep the existing assertions that the signed-in member's name/handle come from `me`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/shell.test.tsx`
Expected: FAIL — "More" and "Your profile" links still present.

- [ ] **Step 3: Replace the chip and remove the nav item**

Remove `MoreHorizontal` from the lucide import and `{ to: '/settings', label: 'More', icon: MoreHorizontal }` from `navLinks`. Replace the avatar `Link` block with `<AccountMenu displayName={displayName} username={username} />`. Keep the Create Post button and all other nav links unchanged.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/LeftRail.tsx frontend/src/components/shell.test.tsx
git commit -m "feat(frontend): open account menu from user chip"
```

---

### Task 3: Docs and full verification

**Files:**
- Modify: `docs/web-ui.md` (§3 global shell description at ~line 133 and component library index at ~line 371)
- Modify: `docs/account-menu-design.md` (status: implemented)
- Add: `docs/account-menu-plan.md` (currently untracked)

**Interfaces:**
- Consumes: all tasks.
- Produces: docs matching shipped behavior.

- [ ] **Step 1: Document the menu**

In `docs/web-ui.md`, describe the left rail's bottom user chip as an AccountMenu trigger with a Settings / Sign out popup (no "More" nav item), and add an `AccountMenu` row to the component library index. Mark the design status implemented. Commit the plan doc as-is.

- [ ] **Step 2: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build green.

- [ ] **Step 3: Commit**

```bash
git add docs/web-ui.md docs/account-menu-design.md docs/account-menu-plan.md
git commit -m "docs: document account menu and remove More nav item"
```

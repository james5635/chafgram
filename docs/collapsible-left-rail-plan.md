# Collapsible Left Rail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the viewer collapse the left navigation rail to an icon-only strip and expand it again, with the state persisted.

**Architecture:** A `RailProvider` context holds `collapsed` and `toggle`, persisted in `localStorage`. It wraps the app in `__root.tsx`. `LeftRail`, `AccountMenu`, `Header` and `AppShell` consume it so the rail width, header offset and content offset stay in sync.

**Tech Stack:** React 19, TypeScript strict, TanStack Router, Tailwind v4 tokens, Vitest + RTL.

**Spec:** `docs/collapsible-left-rail-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- New file `frontend/src/hooks/useRail.tsx` exporting `RailProvider` and `useRail(): { collapsed: boolean; toggle: () => void }`.
- Persistence key exactly `localStorage['rail-collapsed']`; default `false`; read after hydration, written on toggle.
- Expanded rail is `w-64`; collapsed rail is `w-16`, icon-only, with unread badges still visible.
- Every icon-only control keeps an accessible name via `aria-label`.
- `AccountMenu` gains `collapsed?: boolean`; collapsed renders only Avatar with `aria-label="Account menu"` (no name, handle or chevron); the menu contents are unchanged.
- Header toggle uses lucide panel-left icons with labels "Collapse navigation" / "Expand navigation".
- `AppShell` content offset: `lg:pl-64` expanded, `lg:pl-16` collapsed. Header brand/search padding must match the rail width at `lg`.
- No new dependencies; tokens only; `just verify` green.

## Review Focus

1. Collapsed state persists across remount — pinned in Task 1 (`renderHook` with pre-set localStorage).
2. Icon-only rail stays usable and labelled — pinned in Task 2 (aria-labels, avatar-only trigger, menu still opens).
3. Header and content offsets track the rail — pinned in Task 3 (class assertions after toggle).
4. Provider is actually wired into the app — pinned in Task 4 (root render/consumer test).
5. Expanded behavior does not regress — existing shell/header/account tests must stay green.

---

### Task 1: Shared rail state

**Files:**
- Create: `frontend/src/hooks/useRail.tsx`
- Test: `frontend/src/hooks/useRail.test.tsx`

**Interfaces:**
- Produces: `RailProvider` and `useRail(): { collapsed: boolean; toggle: () => void }`.

- [ ] **Step 1: Write failing tests**

Use `renderHook` with a wrapper that renders `<RailProvider>{children}</RailProvider>`:
  - defaults to `collapsed === false`;
  - `toggle()` flips `collapsed` to `true` and writes `'1'` to `localStorage['rail-collapsed']`;
  - a provider mounted with `localStorage['rail-collapsed'] === '1'` starts collapsed after effects, and toggling back writes `'0'`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/hooks/useRail.test.tsx`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement provider and hook**

Implement exactly the exported names/signature. Use `useState(false)`, a mount effect reading the localStorage key (`=== '1'`), and an effect or toggle that writes `'1'`/`'0'`. Avoid touching localStorage during SSR.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/hooks/useRail.tsx frontend/src/hooks/useRail.test.tsx
git commit -m "feat(frontend): add shared rail collapse state"
```

---

### Task 2: Icon-only rail and account trigger

**Files:**
- Modify: `frontend/src/components/LeftRail.tsx`
- Modify: `frontend/src/components/AccountMenu.tsx`
- Test: `frontend/src/components/shell.test.tsx`
- Test: `frontend/src/components/AccountMenu.test.tsx`

**Interfaces:**
- Consumes: `useRail()` from Task 1; `AccountMenu({ displayName, username, collapsed? })` produced here.

- [ ] **Step 1: Write failing tests**

In `shell.test.tsx`, wrap renders in `RailProvider` and add a small test-only control that calls `useRail().toggle()`. Assert that after toggling: the `<aside aria-label="Main navigation">` has `w-16` (not `w-64`), nav links still resolve by role/label with no visible text labels, unread badges remain, Create Post is an icon-only button named "Create post", and the account trigger is still named "Account menu".

In `AccountMenu.test.tsx`, render `<AccountMenu displayName="Rojame" username="rojame" collapsed />` and assert the name/handle/chevron are absent, the trigger keeps `aria-label="Account menu"`, and clicking it still shows Settings and Sign out.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/shell.test.tsx src/components/AccountMenu.test.tsx`
Expected: FAIL on collapsed assertions.

- [ ] **Step 3: Implement collapsed rendering**

`LeftRail`: consume `useRail()`, switch root width between `w-64 px-3` and `w-16 px-2`; when collapsed, nav links show only the icon with `aria-label={link.label}` (badges still rendered), Create Post shows only the Plus icon, and `<AccountMenu>` receives `collapsed`. `AccountMenu`: add the optional prop and conditionally hide the name/handle span and chevron. Keep all menu behavior unchanged.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/LeftRail.tsx frontend/src/components/AccountMenu.tsx frontend/src/components/shell.test.tsx frontend/src/components/AccountMenu.test.tsx
git commit -m "feat(frontend): collapse the rail to labelled icons"
```

---

### Task 3: Header toggle and content offset

**Files:**
- Modify: `frontend/src/components/Header.tsx`
- Modify: `frontend/src/components/AppShell.tsx`
- Test: `frontend/src/components/Header.test.tsx`
- Test: `frontend/src/components/shell.test.tsx`

**Interfaces:**
- Consumes: `useRail()` from Task 1.

- [ ] **Step 1: Write failing tests**

In `Header.test.tsx`, render `<RailProvider><Header /></RailProvider>`. Assert a button named "Collapse navigation" exists; click it and assert "Expand navigation" appears. Assert the header's inner flex container has `lg:pl-64` before and `lg:pl-16` after.

In `shell.test.tsx`, assert `AppShell`'s offset wrapper has `lg:pl-64` by default; after toggling, it has `lg:pl-16`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/Header.test.tsx src/components/shell.test.tsx`
Expected: FAIL — no toggle button / no dynamic offset.

- [ ] **Step 3: Add the toggle and dynamic offsets**

`Header`: consume `useRail()`, add a labelled toggle button with a lucide panel-left icon before the brand link, and switch the inner container padding between `pl-32 sm:pl-40 lg:pl-64` and `pl-16` (collapsed). `AppShell`: consume `useRail()` and switch its offset wrapper between `lg:pl-64` and `lg:pl-16`. Use `focus-visible` styles on the toggle.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/Header.tsx frontend/src/components/AppShell.tsx frontend/src/components/Header.test.tsx frontend/src/components/shell.test.tsx
git commit -m "feat(frontend): toggle rail width from the header"
```

---

### Task 4: Wire provider, docs, full verification

**Files:**
- Modify: `frontend/src/routes/__root.tsx`
- Modify: `docs/web-ui.md` (§3 shell, §7 component index if needed)
- Modify: `docs/collapsible-left-rail-design.md` (status: implemented)
- Add: `docs/collapsible-left-rail-plan.md` (currently untracked)

- [ ] **Step 1: Wire the provider**

In `RootDocument`, wrap the Header/children/Footer tree with `<RailProvider>` inside `ApolloProvider`. Run the full frontend test suite to confirm no consumer is outside the provider.

- [ ] **Step 2: Update docs**

In `docs/web-ui.md`, describe the collapsed `w-16` icon strip, the header toggle and the dynamic `lg:pl-16` offset. Mark the design status implemented. Commit the plan doc as-is.

- [ ] **Step 3: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build green.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/routes/__root.tsx docs/web-ui.md docs/collapsible-left-rail-design.md docs/collapsible-left-rail-plan.md
git commit -m "feat(frontend): wire rail provider into app shell"
```

# Interaction Blocking Overlays Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add reusable regional loading overlays that make async frontend actions visibly busy, inert to pointer and keyboard input, and immune to duplicate submissions.

**Architecture:** A small `BusyOverlay` primitive and `useBusy` hook own the busy state and re-entry guard. Each route or component wraps its busy region with `aria-busy`, applies React 19 `inert` to its content subtree, and renders the `aria-hidden` overlay outside that inert subtree. Its live status is a permanently mounted, `sr-only` `role="status" aria-live="polite"` sibling outside the `aria-busy` root, so the announcement cannot be deferred by `aria-busy`.

**Tech Stack:** React 19, TypeScript strict, TanStack Start/React Router, Tailwind v4 tokens, Vitest + React Testing Library.

**Spec:** `docs/interaction-blocking-overlays-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- New primitive: `frontend/src/components/BusyOverlay.tsx` with signature `BusyOverlay({ label }: { label: string })`.
- New hook: `frontend/src/hooks/useBusy.ts` with signature `useBusy(): { busy: boolean; run: <T>(task: () => Promise<T>) => Promise<T | undefined> }`.
- `run` returns `undefined` when skipped because the region is already busy; clears busy in `finally` on success and failure.
- Busy region root carries `aria-busy={busy}`; its **content subtree** gets React 19 `inert={busy ? true : undefined}`; the visual overlay is outside that subtree (but inside the `relative` root) and is `aria-hidden="true"`.
- Overlay: `aria-hidden="true"`, `absolute inset-0 z-10`, token surface with `backdrop-blur-[2px]`, `animate-spin` ring with `motion-reduce:animate-none`, label in `text-sm font-semibold text-[var(--sea-ink)]`.
- Each adoption site renders one permanently mounted sibling of the busy root (React fragment, no DOM wrapper): `<span className="sr-only" role="status" aria-live="polite">{busy ? busyLabel : ''}</span>`. It is never owned by the `aria-busy` element; `busyLabel` is a local const shared with the overlay.
- Action-specific labels: `Signing in…`, `Posting…`, `Saving…`, `Sending…`, `Updating…`.
- Controls inside a busy region are also `disabled={busy}`.
- No new dependencies; use Tailwind tokens, never raw hex.
- Motion ≤700ms, no springs/bounce, respect `prefers-reduced-motion` (existing global rule).
- Thread query loading and effect-driven `markRead` stay outside overlays; only explicit user mutations are covered.
- Every change ships with colocated Vitest/RTL tests; `just verify` green.

## Review Focus

1. Rapid double click / Enter on a submit button must fire exactly one mutation — pinned in Task 1 (`useBusy` re-entry) and Task 2 (login/composer rapid-click tests).
2. A failed async task must clear the overlay, re-enable controls, and leave the existing error message visible — pinned in Task 1.
3. Keyboard focus must not reach inputs or links in a busy region, while `role="status"` remains announced — pinned in Task 1 (`inert` + live-status test).
4. Post-card like and comment-submit must not stack overlays or block the whole feed — pinned in Task 3.
5. Initial feed/thread loading and automatic `markRead` must keep their skeletons/text and never show an overlay — pinned in Task 4.

---

### Task 1: BusyOverlay primitive and useBusy hook

**Files:**
- Create: `frontend/src/components/BusyOverlay.tsx`
- Create: `frontend/src/components/BusyOverlay.test.tsx`
- Create: `frontend/src/hooks/useBusy.ts`
- Create: `frontend/src/hooks/useBusy.test.tsx`

**Interfaces:**
- Consumes: React 19, Tailwind tokens from `src/styles.css`.
- Produces: `BusyOverlay({ label })` and `useBusy()` per Global Constraints; later tasks import both.

- [x] **Step 1: Write failing tests**

`BusyOverlay.test.tsx`: render the full pattern — `<span className="sr-only" role="status" aria-live="polite">Saving…</span>` sibling of `<div className="relative" aria-busy="true"><div inert><input aria-label="Name" /></div><BusyOverlay label="Saving…" /></div>`. Assert exactly one `role="status"` with `aria-live="polite"` and the label; the visual overlay has `absolute`, `inset-0`, `z-10`, `backdrop-blur-[2px]`, a spinner, and `aria-hidden="true"`; the status is outside both the `aria-busy` root and the `inert` subtree that contains the input.

`useBusy.test.tsx`: use `renderHook`; assert (a) a second `run()` called before the first resolves returns `undefined` and does not invoke the task; (b) after the first resolves, `busy` becomes `false` and the result is returned; (c) a rejected task clears `busy` and rethrows/rejects to the caller; (d) `busy` is `true` while the task is pending.

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/BusyOverlay.test.tsx src/hooks/useBusy.test.tsx`
Expected: FAIL — modules do not exist.

- [x] **Step 3: Implement the primitive and hook**

`BusyOverlay` renders one presentational div with the exact classes and `aria-hidden="true"` from Global Constraints, containing a spinner ring and the label; the live region is owned by the adoption site. `useBusy` stores `busy` in `useState`, uses a ref guard against re-entry, and wraps `await task()` in `try/finally`.

- [x] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/components/BusyOverlay.tsx frontend/src/components/BusyOverlay.test.tsx frontend/src/hooks/useBusy.ts frontend/src/hooks/useBusy.test.tsx
git commit -m "feat(frontend): add regional busy overlay and hook"
```

---

### Task 2: Login, home composer, and /compose page

**Files:**
- Modify: `frontend/src/components/LoginScreen.tsx`
- Modify: `frontend/src/components/Composer.tsx`
- Modify: `frontend/src/routes/compose.tsx`
- Test: `frontend/src/components/LoginScreen.test.tsx`
- Test: `frontend/src/components/Composer.test.tsx`
- Test: `frontend/src/routes/compose.test.tsx`

**Interfaces:**
- Consumes: `BusyOverlay`, `useBusy` from Task 1.
- Produces: no new cross-task interface.

- [x] **Step 1: Write failing tests**

In each test file, add a case that (a) clicking the submit button twice rapidly calls the underlying server function/mutation exactly once, and (b) while the async action is pending, `getByRole('status')` shows the action label and the region root has `aria-busy="true"` with its content subtree inert. Use existing mocks (`loginRequest`, `onSubmit`, `CreatePostDocument`) and controllable promises.

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/LoginScreen.test.tsx src/components/Composer.test.tsx src/routes/compose.test.tsx`
Expected: FAIL — no overlay/re-entry guard yet.

- [x] **Step 3: Wrap each action region**

Replace local `pending`/`submitting` flags with `useBusy`. Wrap form/card content in `relative` root + `inert` content subtree, render `<BusyOverlay label="Signing in…" />`, `"Posting…"`, or `"Updating…"` per screen. Keep existing error messages and `disabled` states.

- [x] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/components/LoginScreen.tsx frontend/src/components/LoginScreen.test.tsx frontend/src/components/Composer.tsx frontend/src/components/Composer.test.tsx frontend/src/routes/compose.tsx frontend/src/routes/compose.test.tsx
git commit -m "feat(frontend): block login and composer interactions while posting"
```

---

### Task 3: Post card actions

**Files:**
- Modify: `frontend/src/components/PostCard.tsx`
- Test: `frontend/src/components/PostCard.test.tsx`

**Interfaces:**
- Consumes: `BusyOverlay`, `useBusy` from Task 1.
- Produces: no new cross-task interface.

- [x] **Step 1: Write failing tests**

Add tests: (a) rapid double click on Like fires one reaction mutation and shows a status overlay during the pending request; (b) submitting a comment twice rapidly creates one comment and shows `Saving…` while pending; (c) opening the comment thread still shows `Loading comments…` text and never a status overlay (query loading stays outside overlays).

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/PostCard.test.tsx`
Expected: FAIL.

- [x] **Step 3: Apply regional overlays**

Use one `useBusy` flag for the post card's action row + thread region: wrap action row and thread in the relative/inert structure, render the overlay with the relevant label. `reactToPost`/`unreactToPost`, `createComment`, and the comment form's submit go through `run`. Keep `showComments`, `showCommentInput`, and query loading behavior unchanged.

- [x] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/components/PostCard.tsx frontend/src/components/PostCard.test.tsx
git commit -m "feat(frontend): block post-card actions while requests run"
```

---

### Task 4: Messages and notifications actions

**Files:**
- Modify: `frontend/src/routes/messages.tsx`
- Modify: `frontend/src/routes/notifications.tsx`
- Test: `frontend/src/routes/messages.test.tsx`
- Test: `frontend/src/routes/notifications.test.tsx`

**Interfaces:**
- Consumes: `BusyOverlay`, `useBusy` from Task 1.
- Produces: no new cross-task interface.

- [x] **Step 1: Write failing tests**

Messages: rapid double send calls `SendMessageDocument` once, `Sending…` status appears while pending, and switching conversations does **not** show an overlay (effect `markRead` stays outside). Notifications: clicking `[Mark all read]` twice calls `MarkNotificationsReadDocument` once and shows `Updating…` while pending.

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/routes/messages.test.tsx src/routes/notifications.test.tsx`
Expected: FAIL.

- [x] **Step 3: Apply overlays to explicit actions only**

Wrap the message composer/thread panel and the notifications action bar with the relative/inert structure + overlay. Route explicit `send()` and `markAllRead()` through `useBusy.run`. Leave the `markRead` `useEffect` and all query `loading` states untouched.

- [x] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add frontend/src/routes/messages.tsx frontend/src/routes/messages.test.tsx frontend/src/routes/notifications.tsx frontend/src/routes/notifications.test.tsx
git commit -m "feat(frontend): block message send and notification actions while saving"
```

---

### Task 5: Docs and full verification

**Files:**
- Modify: `docs/web-ui.md`
- Modify: `design.md`
- Modify: `docs/interaction-blocking-overlays-design.md` (status: implemented)
- Modify: `AGENTS.md` (component library entry for `BusyOverlay`)

**Interfaces:**
- Consumes: all tasks.
- Produces: docs matching shipped behavior.

- [x] **Step 1: Document the overlay pattern**

Add `BusyOverlay` to the web-ui component library and §5 interaction notes; document in `design.md` that indeterminate action overlays use the token surface + spinner while content regions keep skeletons. Mark the design status implemented.

- [x] **Step 2: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build all green.

- [x] **Step 3: Commit**

```bash
git add docs/web-ui.md design.md docs/interaction-blocking-overlays-design.md AGENTS.md
git commit -m "docs: document regional loading overlays"
```

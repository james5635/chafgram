# Profile Posts Feed Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render profile posts as a full-width single-column list like the home feed, instead of a grid inside the profile shell card.

**Architecture:** Move the existing posts region below the profile shell section, still inside `.page-wrap`, and replace its grid classes with the home feed's `space-y-6` spacing. The query, mapper, states and tabs are unchanged.

**Tech Stack:** React 19, TypeScript strict, Tailwind v4 tokens, Vitest + RTL.

**Spec:** `docs/profile-posts-feed-layout-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- Posts render in `<section className="mt-8 space-y-6">` below the profile shell `<section className="island-shell rise-in overflow-hidden rounded-xl">`, inside the same `.page-wrap` div.
- Single column: no `grid`, no `sm:grid-cols-*`, no `lg:grid-cols-*`.
- Keep `aria-busy={postsPending}` on the posts region and `role="alert"` on the error message.
- Keep all existing query logic, `toPostCardData`, `PostCard`, loading/empty/error text, header, tabs and `ProfilePanels` unchanged.
- Use existing tokens/classes; no new dependencies.
- `just verify` green.

## Review Focus

1. Posts must not be nested inside the profile shell card — pinned in Task 1 with the `closest('.island-shell')` assertion.
2. Posts must use one full-width column with the home feed's spacing — pinned in Task 1 with the `.space-y-6` ancestor assertion.
3. Loading, empty and error states still work after the move — existing profile tests cover empty/error; Task 1 keeps them passing.
4. `/profile/me` still resolves the real username — existing test covers it; no query changes allowed.
5. Skeleton loading state must also be single-column — Task 1 changes its wrapper classes.

---

### Task 1: Move the posts list below the profile card

**Files:**
- Modify: `frontend/src/routes/profile.$username.tsx`
- Test: `frontend/src/routes/profile.test.tsx`

**Interfaces:**
- Consumes: existing `posts`, `postsPending`, `postsError` from the component; no new interface.
- Produces: no cross-task interface.

- [ ] **Step 1: Write the failing test**

In `profile.test.tsx`, add:
```tsx
it('renders posts as a full-width list outside the profile card', async () => {
  renderScreen(<Profile />)
  const body = await screen.findByText('My profile post')
  const profileCard = screen.getByRole('heading', { name: 'Rojame' }).closest('.island-shell')
  expect(profileCard).not.toContainElement(body)
  expect(body.closest('.space-y-6')).not.toBeNull()
})
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && pnpm exec vitest run src/routes/profile.test.tsx`
Expected: FAIL — the post is inside the profile shell card and has no `.space-y-6` ancestor.

- [ ] **Step 3: Move the posts region**

Move the entire `<div className="mt-5" aria-busy={postsPending}>…</div>` block out of the profile shell section to immediately after that section, still inside `.page-wrap`. Wrap it in `<section className="mt-8 space-y-6">`, matching the home feed. Change the skeleton and post wrappers from `grid gap-4 sm:grid-cols-2 lg:grid-cols-3` to `space-y-6`; keep three skeleton rows at `island-shell h-48 animate-pulse rounded-xl`.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add 'frontend/src/routes/profile.$username.tsx' frontend/src/routes/profile.test.tsx
git commit -m "refactor(frontend): render profile posts like the home feed"
```

---

### Task 2: Docs and full verification

**Files:**
- Modify: `docs/web-ui.md` (§5.4 profile: Post grid row)
- Modify: `docs/profile-posts-feed-layout-design.md` (status: implemented)
- Add: `docs/profile-posts-feed-layout-plan.md` (currently untracked)

- [ ] **Step 1: Update docs**

In `docs/web-ui.md` §5.4, replace the grid description with a full-width single-column `space-y-6` list below the profile shell. Mark the design status implemented. Commit the plan doc as-is.

- [ ] **Step 2: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build green.

- [ ] **Step 3: Commit**

```bash
git add docs/web-ui.md docs/profile-posts-feed-layout-design.md docs/profile-posts-feed-layout-plan.md
git commit -m "docs: describe full-width profile post list"
```

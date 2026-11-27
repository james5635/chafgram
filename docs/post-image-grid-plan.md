# Post Image Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show every uploaded image in a post instead of only the first one.

**Architecture:** The upload path, backend, and mapper already carry all media ids and signed URLs; the loss is solely in `PostCard`, which renders `post.media[0]`. Replace that with a 2-column grid of all image thumbnails, each opening the lightbox at its own index. Video posts stay unchanged.

**Tech Stack:** React 19, TypeScript strict, Tailwind v4 tokens, Vitest + RTL.

**Spec:** `docs/post-image-grid-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- PHOTO posts with 2+ images render all of them in a `grid grid-cols-2` layout with no cap; a single image keeps its existing full-width `aspect-video` frame.
- Video posts keep the single inline `<video>`; no change to the video branch.
- Every image thumbnail is a labelled button `Preview <alt>` wrapping its `<img>` with `onError={() => void handleMediaError(originalIndex)}`.
- Clicking thumbnail at original media index `i` opens `ImageLightbox` at the matching entry in the IMAGE-only filtered list, never hard-coded to 0.
- Keep `refreshedUrls[i] ?? media.url`, the existing BusyOverlay/inert region, and the lightbox's `onImageError` mapping intact.
- No backend, Composer, GraphQL, or mapper changes.
- Tokens only; no new dependencies; `just verify` green.

## Review Focus

1. Every uploaded image appears — pinned in Task 1 (three-image post renders three distinct thumbnails).
2. Clicking a later thumbnail opens the lightbox on that image — pinned in Task 1 (second thumbnail → `2 / 3` and second alt).
3. 10 images are all rendered with no cap — pinned in Task 1 (ten-image post renders ten buttons).
4. Expired URLs refresh for any thumbnail — pinned in Task 1 (third thumbnail error → `refreshMediaUrl` with third media id).
5. Single-image and video posts do not regress — pinned by existing tests plus new explicit assertions.

---

### Task 1: Render all images in `PostCard`

**Files:**
- Modify: `frontend/src/components/PostCard.tsx`
- Test: `frontend/src/components/PostCard.test.tsx`

**Interfaces:**
- Consumes: existing `post.media`, `refreshedUrls`, `handleMediaError`, `lightboxEntries`, `ImageLightbox`.
- Produces: no new cross-task interface.

- [ ] **Step 1: Write failing tests**

In `PostCard.test.tsx` add:
  - `renders every image in a two-column grid`: post with three IMAGE media entries (ids `a`, `b`, `c`; alts `First photo`, `Second photo`, `Third photo`), assert three buttons named `Preview <alt>`, each wrapping an `<img>` with its own `src`, and the thumbnails' common ancestor has classes `grid` and `grid-cols-2`;
  - `opens the lightbox at the clicked image`: click `Preview Second photo`, assert dialog shows `2 / 3` and the second image;
  - `renders all ten images without a cap`: post with ten IMAGE entries, assert ten preview buttons;
  - `refreshes the URL for a later thumbnail`: post with three images, fire `error` on the third thumbnail, assert `refreshMediaUrl` called with `{ data: { mediaId: 'c' } }` and its src updates.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/PostCard.test.tsx`
Expected: FAIL — only one thumbnail exists.

- [ ] **Step 3: Render the grid**

Replace the single-image branch of the media block: keep the video branch for `kind === 'VIDEO'`; for images, if there is exactly one, keep the existing `aspect-video` frame and button; otherwise render all images in `<div className="grid grid-cols-2 gap-1">` with compact `aspect-square` tiles, each a labelled button with `onError={() => void handleMediaError(index)}` and `onClick={() => setLightboxIndex(lightboxEntries.findIndex(({ index: source }) => source === index))}`. Keep the existing `loading`/`decoding` attributes and `refreshedUrls[index]` lookup.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS (including the pre-existing single-image, video, refresh and lightbox tests). Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/PostCard.tsx frontend/src/components/PostCard.test.tsx
git commit -m "fix(frontend): render every uploaded image in a post"
```

---

### Task 2: Docs and full verification

**Files:**
- Modify: `docs/web-ui.md` (§5.1.1 media row)
- Modify: `docs/post-image-grid-design.md` (status: implemented)
- Add: `docs/post-image-grid-plan.md` (currently untracked)

- [ ] **Step 1: Update docs**

In `docs/web-ui.md` §5.1.1, describe the 2-column grid for multi-image PHOTO posts (single image keeps full-width frame; every thumbnail opens the lightbox at its own index). Mark the design status implemented. Commit the plan doc as-is.

- [ ] **Step 2: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build green.

- [ ] **Step 3: Commit**

```bash
git add docs/web-ui.md docs/post-image-grid-design.md docs/post-image-grid-plan.md
git commit -m "docs: describe multi-image post grid"
```

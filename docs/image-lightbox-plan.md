# Image Lightbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let viewers click a post image to preview it full-size and step through all of that post's images.

**Architecture:** A reusable `ImageLightbox` modal built on `@base-ui/react/dialog` owns the current image index. `PostCard` turns its thumbnail into a button that opens the dialog at the clicked image, passing the post's already-refreshed image URLs. Video posts are unchanged.

**Tech Stack:** React 19, TypeScript strict, Base UI dialog, Tailwind v4 tokens, Vitest + RTL.

**Spec:** `docs/image-lightbox-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- New component signature: `ImageLightbox({ images, initialIndex, open, onClose })` where `images: { url: string; alt: string }[]`, `initialIndex: number`, `open: boolean`, `onClose: () => void`.
- Use `Dialog.Root`, `Dialog.Portal`, `Dialog.Backdrop`, `Dialog.Popup`, `Dialog.Title`, `Dialog.Close` from `@base-ui/react/dialog` (already installed; no new dependency).
- Component owns current index; reset to `initialIndex` whenever it opens. Clamp navigation: previous disabled at 0, next disabled at `images.length - 1` (no wrap).
- Visible `n / N` counter with polite live region; image `alt` shown; previous/next buttons have accessible names.
- Escape and backdrop click close via `onClose`; Base UI handles focus trap/return.
- Tailwind tokens only, no new motion; no zoom/download/autoplay.
- `PostCard` includes only `kind === 'IMAGE'` media in the lightbox; video posts keep the inline player.
- Use `refreshedUrls[index] ?? post.media[index].url`; a lightbox image error calls the existing `handleMediaError(index)`.
- Colocated Vitest/RTL tests; `just verify` green.

## Review Focus

1. All images are reachable from one thumbnail — pinned in Task 2 (two-image post: open first, click Next, second image's alt visible).
2. Keyboard and bounds behavior — pinned in Task 1 (ArrowLeft/ArrowRight, disabled at ends, Escape closes).
3. Video posts must not show an image preview button — pinned in Task 2.
4. Expired signed URLs refresh for lightbox images too — pinned in Task 2 (error on image → `refreshMediaUrl` called → lightbox uses fresh URL).
5. Focus/accessibility: dialog title, alt text, labelled controls — pinned in Task 1.

---

### Task 1: `ImageLightbox` component

**Files:**
- Create: `frontend/src/components/ImageLightbox.tsx`
- Test: `frontend/src/components/ImageLightbox.test.tsx`

**Interfaces:**
- Produces: `ImageLightbox({ images, initialIndex, open, onClose })` consumed by Task 2.

- [ ] **Step 1: Write failing tests**

Tests (render with `open: true`, `onClose: vi.fn()`):
  - shows the image at `initialIndex` with its alt text and a `2 / 3` counter;
  - Next advances to the second image and Previous returns to the first;
  - ArrowRight/ArrowLeft change the image;
  - Previous is disabled at index 0 and Next at the last index;
  - clicking the backdrop calls `onClose`;
  - Escape calls `onClose`;
  - reopening with a different `initialIndex` starts at that index.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/ImageLightbox.test.tsx`
Expected: FAIL — module does not exist.

- [ ] **Step 3: Implement the component**

Implement the exact signature. Use `Dialog.Root open={open} onOpenChange={(next) => { if (!next) onClose() }}`; portal a backdrop and a centered popup; render `<img src={images[index].url} alt={images[index].alt}>`; labelled Previous/Next buttons and a counter; `useEffect` for ArrowLeft/ArrowRight while open; `useEffect` resetting index when `open` becomes true. Use tokens for surfaces, no raw hex.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/ImageLightbox.tsx frontend/src/components/ImageLightbox.test.tsx
git commit -m "feat(frontend): add reusable image lightbox dialog"
```

---

### Task 2: Open the lightbox from `PostCard`

**Files:**
- Modify: `frontend/src/components/PostCard.tsx`
- Test: `frontend/src/components/PostCard.test.tsx`

**Interfaces:**
- Consumes: `ImageLightbox({ images, initialIndex, open, onClose })` from Task 1.

- [ ] **Step 1: Write failing tests**

In `PostCard.test.tsx`:
  - `opens a lightbox with all images from the post thumbnail`: render a post with two IMAGE media entries, click the button labelled "Preview …" for the first image, assert the dialog shows the first image and `1 / 2`, click Next, assert the second image's alt is shown;
  - `does not render an image preview button for video posts`: reuse the existing video fixture and assert no "Preview" button;
  - `uses the refreshed URL in the lightbox`: reuse the existing refresh-media fixture, trigger the thumbnail's `onError`, await the refreshed `src`, then open the dialog and assert the dialog image uses the refreshed URL.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm exec vitest run src/components/PostCard.test.tsx`
Expected: FAIL — no preview button/lightbox exists.

- [ ] **Step 3: Wire the dialog into PostCard**

Add `const [lightboxIndex, setLightboxIndex] = useState<number | null>(null)`. Build `lightboxImages = (post.media ?? []).filter((m) => m.kind === 'IMAGE').map((m) => ({ url: refreshedUrls[index] ?? m.url, alt: m.alt }))` using the original index for URL lookup. Wrap the `<img>` thumbnail in a `<button type="button" aria-label={`Preview ${alt}`} onClick={() => setLightboxIndex(index)}>`; keep the media frame and `onError`. Render `<ImageLightbox images={lightboxImages} initialIndex={lightboxIndex ?? 0} open={lightboxIndex !== null} onClose={() => setLightboxIndex(null)} />`. Leave the video branch untouched.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/PostCard.tsx frontend/src/components/PostCard.test.tsx
git commit -m "feat(frontend): open post images in the lightbox"
```

---

### Task 3: Docs and full verification

**Files:**
- Modify: `docs/web-ui.md` (§5.1.1 media row and §7 component library index)
- Modify: `docs/image-lightbox-design.md` (status: implemented)
- Add: `docs/image-lightbox-plan.md` (currently untracked)

- [ ] **Step 1: Update docs**

In `docs/web-ui.md`, note that clicking a post image opens the `ImageLightbox` with previous/next through all images, and add an `ImageLightbox` row to the component library index. Mark the design status implemented. Commit the plan doc as-is.

- [ ] **Step 2: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build green.

- [ ] **Step 3: Commit**

```bash
git add docs/web-ui.md docs/image-lightbox-design.md docs/image-lightbox-plan.md
git commit -m "docs: document post image lightbox"
```

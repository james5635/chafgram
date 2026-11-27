# Image lightbox design

Date: 2026-11-27
Status: implemented

## Intent

Clicking an image in a post currently does nothing. Give viewers a proper
lightbox: a full-size preview that opens at the clicked image and lets them
step through every image in that post with previous/next controls.

## Goals

- New reusable `frontend/src/components/ImageLightbox.tsx` built on the
  existing `@base-ui/react/dialog` primitive (no new dependency).
- `PostCard` opens it when its image is clicked; all image media from the post
  are available, video posts keep their inline player.
- Previous/next buttons plus ←/→ keys; disabled at the first/last image;
  a visible `n / N` counter and the image's alt text.
- Escape and backdrop click close it; Base UI handles focus trapping and
  returning focus to the trigger.
- Use already-refreshed signed URLs; a failed lightbox image uses the same
  `refreshMediaUrl` fallback the card uses.
- Colocated Vitest/RTL tests, updated PostCard test, `just verify` green.

## Non-goals / anti-goals

- No zoom/pan, no download, no full-screen API, no slideshow autoplay.
- No changes to video playback, media uploads, or the backend.
- No global lightbox state; each card owns its own open index.
- No full-screen app freeze — only the dialog is modal.

## Constraints

- Interface: `ImageLightbox({ images, initialIndex, open, onClose })` where
  `images: { url: string; alt: string }[]`, `initialIndex: number`,
  `open: boolean`, `onClose: () => void`. The component owns the current
  index and resets it to `initialIndex` when opened.
- Dialog parts: `Dialog.Root/Portal/Backdrop/Popup/Title/Close` from
  `@base-ui/react/dialog`, already installed.
- Tailwind tokens only, no raw hex; respect `prefers-reduced-motion` by not
  adding new motion.
- The card's image becomes a real button with an accessible label; `alt` text
  stays on the `<img>`.
- Content is a modal dialog, so a `BusyOverlay` is not involved.

## Decisions

- Only `kind === 'IMAGE'` entries go into the lightbox. A post's mapper gives
  every item the same kind, so video posts simply never open it.
- Bounds are clamped: previous is disabled at index 0, next at `length - 1`
  (no wrap-around).
- `PostCard` keeps its single-image thumbnail; the lightbox is how the other
  attachments become reachable.

## Approaches considered

1. **Reusable `ImageLightbox` with Base UI Dialog (chosen)** — isolated modal
   behavior, no new dependency, testable without Apollo mocks.
2. **Inline overlay in `PostCard`** — fewer files but tangles media display
   with modal logic and makes tests heavier.
3. **Native `<dialog>` + `showModal()`** — Escape/focus for free but still
   requires hand-rolled navigation and imperative lifecycle code.

## Left to the builder

- Exact popup sizing/classes, provided tokens are used and the image fits the
  viewport on small screens.
- Whether to show the counter as text or `aria-live`; a visible `n / N` with
  a polite live region is recommended.

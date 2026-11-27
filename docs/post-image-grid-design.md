# Post image grid design

Date: 2026-11-27
Status: implemented

## Intent

Uploading several images produces a post with only one visible image. Tracing
the data shows the loss is only in `PostCard`: `Composer` collects every
uploaded asset id, the backend stores them all, `Views.post` returns every id
with a signed URL, and `toPostCardData` maps every item — but the card renders
`post.media[0]` only.

## Decision

Render **every** image of a PHOTO post in a compact **2-column grid**, with no
cap. A single image keeps its current full-width thumbnail. Video posts keep
their single inline player. Every grid thumbnail opens `ImageLightbox` at that
specific image.

## Goals / non-goals

- Goal: all uploaded images are visible in the post, and each opens the
  lightbox at the right position.
- Goal: expired signed URLs refresh independently for any thumbnail and any
  lightbox image.
- Non-goal: no backend, uploader, GraphQL, or mapper changes — they already
  carry all media.
- Non-goal: no cap / "+N more" tile — the human partner chose to always show
  every image, accepting taller cards for large uploads.
- Anti-goal: no change to video playback or to the lightbox component itself.

## Constraints

- Two columns for multi-image posts; exact gap/aspect classes are the
  builder's call as long as tokens are used and tiles stay compact.
- Each thumbnail is a labelled button (`Preview <alt>`) wrapping its `<img>`
  with `onError` wired to `handleMediaError(originalIndex)`.
- Clicking thumbnail `i` must open the lightbox at the corresponding entry in
  the IMAGE-only filtered list, not always at 0.
- Existing BusyOverlay/inert behavior stays unchanged.
- Colocated Vitest/RTL tests; `just verify` green.

## Approaches considered

1. **Render all images in a 2-column grid (chosen)** — every attachment
   visible and clickable; no arbitrary limit, card can grow tall.
2. **Cap at 4 thumbnails + "+N more" tile** — shorter cards but hides images
   until the lightbox opens; rejected by the human partner.
3. **Stack images full-width** — simple but very long even for 3–4 images;
   rejected in favor of the grid.

## Left to the builder

- Exact grid gap and tile aspect ratio, provided the grid is 2 columns and
  uses existing tokens.
- Whether to keep the `aspect-video` frame for the single-image case (recommended).

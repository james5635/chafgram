# Interaction blocking with loading overlays — design

Date: 2026-11-27
Status: implemented

The repo ignores `docs/superpowers/`, so this design is tracked under `docs/`.

## Intent and why

Async actions (login, posting, liking, commenting, sending a message, marking
notifications read) currently give only partial feedback: some buttons change
label and disable, others do nothing until the request returns. A fast double
click can fire duplicate mutations, and a slow request leaves the user unsure
whether the click registered.

Add a consistent **regional loading overlay**: while an action runs, the
component that owns it is visibly busy, cannot be interacted with, and exposes
that state to assistive technology.

## Goals

- One reusable overlay primitive and one reusable busy-state hook in
  `frontend/src/components/`.
- Apply to every user-initiated async action: login form, home composer,
  `/compose` page, post card (like, comment thread, comment submit), messages
  send, notifications mark-all-read, and any profile/admin action that mutates.
- Prevent duplicate submissions from rapid clicks or Enter keys.
- Announce busy state with `aria-busy` and an accessible status label.
- Respect `prefers-reduced-motion` and Tailwind tokens from `design.md`.

## Non-goals

- No full-screen/global overlay; other parts of the app stay usable.
- No changes to initial data loading — feed/list skeletons stay as they are.
- No changes to backend APIs or auth/BFF token flow.
- No blocking during silent refresh/bootstrap; that already gates route content.

## Anti-goals

- An overlay that traps focus or hides the reason for the wait.
- A spinner used for content regions (design.md §9: skeletons for content,
  spinners only for indeterminate actions).
- An overlay that looks like a modal but is dismissed by clicking elsewhere.

## Constraints

- React 19, TypeScript strict, Tailwind v4 CSS-first (`@tailwindcss/vite`).
- Use design tokens (`--surface-strong`, `--sea-ink`, `--brand`, `--lagoon`),
  never raw hex for new surfaces.
- Motion: ≤700ms, no springs, no bounce; respect `prefers-reduced-motion`.
- Vitest + React Testing Library, colocated `*.test.tsx`; tests assert visible
  state, disabled controls, `aria-busy`, and no duplicate calls.
- No new dependencies.
- Frontend intent skills: run `pnpm exec intent list` from `frontend/` before
  substantial work and load a matching local skill.

## Approaches considered

1. **Single global overlay driven by a shared mutation counter.** Simple to
   implement once, but freezes the whole app during every like or comment and
   makes quick interactions feel heavy. Rejected.
2. **Inline spinner + disabled button only.** Cheap, but there is no clear
   region-level boundary and several existing mutations have no pending state
   at all. Insufficient on its own.
3. **Regional overlay per busy component (chosen).** Each component owns a
   `busy` flag, renders a small overlay over itself, and disables its own
   controls. Other regions remain interactive. Fits the existing design rules
   and keeps state local and testable.

## Decisions

- New primitive `frontend/src/components/BusyOverlay.tsx`:
  `BusyOverlay({ label }: { label: string })`, rendered as an
  `absolute inset-0 z-10` layer inside a `relative` parent, with a translucent
  token surface, `backdrop-blur-[2px]`, a `animate-spin` ring, and the label in
  `text-sm font-semibold text-[var(--sea-ink)]`. It carries
  `role="status" aria-live="polite"`.
- New hook `frontend/src/hooks/useBusy.ts`:
  `useBusy(): { busy: boolean; run: <T>(task: () => Promise<T>) => Promise<T | undefined> }`.
  `run` guards against re-entry while busy, sets busy before awaiting, clears it
  in `finally`, and returns `undefined` when a call is skipped.
- Each busy region sets `aria-busy={busy}` and `disabled={busy}` on its own
  interactive controls; the overlay also intercepts pointer events.
- Existing local `pending`/`submitting` flags are replaced by `useBusy` so the
  behavior is uniform. Mutation hooks that currently ignore pending state
  (Apollo `useMutation` results) adopt it.
- Overlay labels are action-specific: `Signing in…`, `Posting…`, `Saving…`,
  `Sending…`, `Updating…`.
- On failure the overlay clears in `finally`; existing error text/alerts remain
  visible. No automatic retry.

## Settled during builder check (designer calls)

- **Comment thread**: exclude query loading. Opening the thread keeps its
  current `Loading comments…` text (a content region, per design.md §9); only
  the comment **submit** mutation gets an overlay.
- **Messages `markRead`**: exclude the effect-driven mutation. It is not a
  direct click and would otherwise block the thread on every conversation
  switch; only the explicit send action gets an overlay.
- **Keyboard blocking**: a busy region is truly inert. The region root carries
  `aria-busy`; its content subtree gets React 19's `inert` attribute while busy,
  and the `BusyOverlay` sits **outside** that inert subtree so `role="status"`
  is still announced. Controls are also `disabled` for clarity.

## Left to the builder

- Which exact wrapper element in each route carries `relative` (card, form, or
  panel) — choose the smallest region that contains all controls for the action.
- Whether the messages thread and conversation list get separate overlays or
  one shared panel overlay; document the choice.
- How to handle multiple independent actions in one region (e.g., like and
  comment on the same post card): one shared `busy` flag is acceptable if it
  blocks the whole card for the duration.

## Verification

- Unit/component tests for `BusyOverlay` and `useBusy` (re-entry guard, busy
  cleared on success and failure).
- Updated component tests for login, composer, PostCard, messages, and
  notifications asserting overlay presence, `aria-busy`, disabled controls,
  and exactly one call for rapid double clicks.
- `just verify` green; new UI checked in light/dark/auto with keyboard focus
  still visible.

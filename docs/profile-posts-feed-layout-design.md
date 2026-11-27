# Profile posts layout design

Date: 2026-11-27
Status: implemented

## Intent

The profile Posts tab currently renders `PostCard`s in a 2–3 column grid inside
the profile shell card. The human partner wants it to read like the home feed:
one full-width post per row.

## Decision

Move the posts list **outside** the profile shell card and render it in the
same full-width, single-column shape as the home feed (`.page-wrap` +
`space-y-6`). Keep the header, stats, tabs, `ProfilePanels`, the
`memberPosts(username, first: 20)` query, and all loading/empty/error states.

## Goals / non-goals

- Goal: profile posts use the same `PostCard` layout as `/`.
- Non-goal: no changes to the query, data mapping, tabs, or other screens.
- Anti-goal: no nested shell/borders around the post list; no grid columns.

## Constraints

- Reuse the home feed's `space-y-6` spacing rather than inventing new values.
- Keep `aria-busy` on the content region during loading and `role="alert"` on
  the error message.
- Update `docs/web-ui.md` §5.4 and the profile component test to assert the
  new structure; `just verify` green.

## Approaches considered

1. Single column inside the card — smaller diff, but cramped media/actions and
   nested shell borders. Rejected.
2. Full-width list outside the card (chosen) — consistent with the home feed
   and gives `PostCard` the room it needs.

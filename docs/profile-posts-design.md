# Profile posts design

Date: 2026-11-27
Status: implemented

The repo ignores `docs/superpowers/`, so this short design is tracked under `docs/`.

## Intent

Opening a profile and selecting the Posts tab shows no posts at all — including
your own. The tab row is static: `Profile` queries only `member`/`me`, and no
component below the tabs loads or renders posts. The backend already has
`PostService.listByAuthor` and `Views.posts`, so the missing pieces are a
GraphQL query and a post list in the profile UI.

## Goals

- Backend: `memberPosts(username: String!, first: Int = 20): [Post!]!` in
  `schema.graphqls`, resolved in `SocialQueryResolver`. `first` is clamped to
  1–100 like other list queries. The resolver uses the existing
  `findByUsername` + `requireVisible` checks (mutual-block hiding, BR-2.2),
  `PostService.listByAuthor`, and `Views.posts` so media URLs, saves and
  reactions match the feed.
- Frontend: add a `MemberPosts` operation and render the member's posts as
  `PostCard`s under the tabs, with loading, empty and error states.
- Resolve `/profile/me` to the real username from the `me` query; never send
  the literal `me` to the backend.
- Tests on both sides; `just verify` green.

## Non-goals / anti-goals

- Replies, Media and Likes tabs stay as they are (static buttons).
- No pagination / "load more" — `first` is a simple limit, default 20.
- No changes to post creation, feeds, auth flow, or the backend data model.
- No new dependencies.

## Constraints

- Resolver class is already `@PreAuthorize("isFullyAuthenticated()")`; the new
  query is authenticated by the same rule.
- Use existing DTOs/services; no duplicate post-to-view mapping.
- Frontend uses Apollo `useQuery`, the generated document from codegen, the
  existing `toPostCardData` mapper and `PostCard` component.
- Tailwind tokens only; loading state is a skeleton (content region), not a
  busy overlay.
- Colocated Vitest/RTL tests plus a backend GraphQL integration test.

## Approaches considered

1. **`memberPosts(username, first): [Post!]!` (chosen)** — reuses existing
   service and view code; matches `savedPosts`/`searchPosts`. Smallest change
   that shows all of a member's posts.
2. **`memberPosts(username, first, cursor): FeedPage!`** — supports
   pagination but adds cursor plumbing and UI state the profile spec doesn't
   require. Rejected.
3. **Filter `discoverFeed` in the browser** — rejected by the human partner:
   only sees the first feed page and misses posts reliably.

## Left to the builder

- Exact grid/skeleton classes, provided they use tokens and match §5.4 of
  `docs/web-ui.md`.
- Whether the backend test lives in `SocialFlowIntegrationTest` or a new
  focused GraphQL test class; the assertions must cover own posts visible,
  the `first` limit, and mutual-block hiding.

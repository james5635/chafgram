# Profile Posts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a member's posts (including your own) in the profile Posts tab via a new backend GraphQL query.

**Architecture:** A new `memberPosts(username, first)` query reuses `PostService.listByAuthor` and `Views.posts`, with the resolver's existing username lookup and mutual-block visibility check. The frontend adds a generated operation and renders `PostCard`s under the profile tabs using the existing mapper, with loading/empty/error states.

**Tech Stack:** Spring Boot 4 / GraphQL Java, JUnit + MockMvc integration tests, React 19, TanStack Router, Apollo Client 4, Vitest + RTL.

**Spec:** `docs/profile-posts-design.md`

The repo ignores `docs/superpowers/`, so this plan is tracked under `docs/`.

## Global Constraints

- Backend query signature: `memberPosts(username: String!, first: Int = 20): [Post!]!`.
- `first` is clamped to 1–100; default 20 when omitted/null. Newest posts first (existing repository order by id desc).
- Resolver: `current.require()`, `findByUsername(username)`, `requireVisible(viewer, target.getId(), "Posts")`, `posts.listByAuthor(target.getId(), limit)`, `views.posts(page, viewer)`. No new DTO or service method.
- Existing `SocialQueryResolver` class-level `@PreAuthorize("isFullyAuthenticated()")` applies; authorization failures remain HTTP 200 + `errors[]`.
- Frontend operation name exactly `MemberPosts`; variables `{ username: String!, first: Int = 20 }`.
- Frontend must query with the **resolved** username (`member?.username`), never the literal route param `me`.
- Select exactly the `Post` fields needed by `toPostCardData`: `id, type, body, editedAt, createdAt, commentCount, reactionCount, myReaction, isSaved, mediaIds, mediaUrls, author { id username displayName avatarMediaId }`.
- Render with `PostCard` + `toPostCardData`; loading is a skeleton (content region, not a busy overlay); empty state text "No posts yet."; error state visible.
- Replies/Media/Likes tabs remain unchanged; no pagination UI.
- Tailwind tokens only; no new dependencies; generated GraphQL files are never hand-edited.
- Tests on both sides; `just verify` green.

## Review Focus

1. `/profile/me` must send the real username, not `me` — pinned in Task 2 (mock `useParams` → `me`, `MeDocument` returns username `rojame`, assert `MemberPosts` mock variables `{ username: 'rojame', first: 20 }`).
2. `first` limit and default — pinned in Task 1 (query with `first: 1` returns exactly one newest post; omitting `first` returns both).
3. Mutual-block hiding (BR-2.2) — pinned in Task 1 (blocked author's `memberPosts` returns an error, no data).
4. Own posts appear in the Posts tab — pinned in Task 2 (frontend renders a post card with the mocked post body) and Task 1 (backend returns the author's own posts).
5. Loading, empty and error states — pinned in Task 2 (three frontend tests).

---

### Task 1: Backend `memberPosts` query

**Files:**
- Modify: `backend/api/src/main/resources/graphql/schema.graphqls` (add to `type Query`)
- Modify: `backend/api/src/main/java/io/github/james5635/chafgram/social/controller/SocialQueryResolver.java`
- Test: `backend/api/src/test/java/io/github/james5635/chafgram/social/GraphqlApiTest.java`

**Interfaces:**
- Consumes: `PostService.listByAuthor(Long authorId, int limit)`, `Views.posts(List<Post>, User)`, existing `findByUsername`/`requireVisible` helpers.
- Produces: GraphQL field `memberPosts(username: String!, first: Int = 20): [Post!]!` consumed by Task 2.

- [ ] **Step 1: Write failing tests**

In `GraphqlApiTest`, add:
  - `memberPostsReturnsTheAuthorsPostsAndHonoursFirst`: with `@WithMockUser(username = "alice", roles = "ADMIN")`, create two posts through `PostService` (bodies "older post" then "newer post"), then POST `/graphql` with `{"query":"{ memberPosts(username: \"alice\", first: 1) { body author { username } } }"}`. Assert `status().isOk()`, `$.data.memberPosts` has length 1, `$.data.memberPosts[0].body` is `"newer post"`, and author username is `"alice"`. Repeat the query without `first` and assert two posts.
  - `memberPostsHidesPostsFromBlockedAuthor`: with the same viewer, create a second member through the existing invite flow, create a post for that member via `PostService`, call `blocks.block(alice, other.getId())`, then query `memberPosts(username: other.username)`. Assert `$.errors[0]` exists and `$.data.memberPosts` does not exist.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && ./gradlew :api:test --tests '*GraphqlApiTest'`
Expected: FAIL — schema has no `memberPosts` field.

- [ ] **Step 3: Add the schema field and resolver**

Add the exact field from Global Constraints to `type Query` in `schema.graphqls`. Add a `@QueryMapping public List<PostDto> memberPosts(@Argument String username, @Argument Integer first)` in `SocialQueryResolver` following the Global Constraints order and clamp. Use imported types only (no FQCNs).

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Also run `./gradlew :api:test --tests '*SchemaDtoSyncTest'` — no new DTO is needed.

- [ ] **Step 5: Commit**

```bash
git add backend/api/src/main/resources/graphql/schema.graphqls backend/api/src/main/java/io/github/james5635/chafgram/social/controller/SocialQueryResolver.java backend/api/src/test/java/io/github/james5635/chafgram/social/GraphqlApiTest.java
git commit -m "feat(backend): expose member posts through GraphQL"
```

---

### Task 2: Render posts on the profile page

**Files:**
- Modify: `frontend/src/graphql/feed.graphql`
- Modify: `frontend/src/routes/profile.$username.tsx`
- Test: `frontend/src/routes/profile.test.tsx`

**Interfaces:**
- Consumes: GraphQL `memberPosts(username: String!, first: Int = 20): [Post!]!` from Task 1; `PostCard`, `toPostCardData`.
- Produces: no new cross-task interface.

- [ ] **Step 1: Write failing tests**

In `profile.test.tsx`, make the mocked `useParams` read a mutable hoisted `params` object so a test can set `username: 'me'`. Add a `MemberPostsDocument` mock returning a post with the exact Global Constraints field set, a body `"My profile post"`, author `rojame`, and zero counts. Tests:
  - `shows the signed-in members posts on the Posts tab`: with `username: 'rojame'`, assert the post body is visible after loading;
  - `queries by resolved username on the me route`: set `username: 'me'`, provide `MeDocument` data with `username: 'rojame'`, assert the `MemberPosts` mock is consumed (post body visible);
  - `shows an empty state when the member has no posts` with `memberPosts: []`;
  - `shows an error state when the query fails`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && pnpm run generate && pnpm exec vitest run src/routes/profile.test.tsx`
Expected: FAIL — `MemberPostsDocument` does not exist until the operation is added and codegen runs.

- [ ] **Step 3: Add the operation and render the list**

Add the `MemberPosts` operation to `feed.graphql` with the exact field selection from Global Constraints. In `Profile`, run `useQuery(MemberPostsDocument, { variables: { username: member?.username ?? '', first: 20 }, skip: !member?.username })` after the existing member/me queries. Below the tab row, render: skeleton while `loading`, error text when `error`, "No posts yet." when the list is empty, otherwise `posts.map((post) => <PostCard key={post.id} post={toPostCardData(post)} />)` in a responsive grid. Keep the header, tabs, and `ProfilePanels` unchanged.

- [ ] **Step 4: Run tests to verify they pass**

Run the Step 2 command. Expected: PASS. Then run `pnpm exec tsc --noEmit`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/graphql/feed.graphql frontend/src/routes/profile.$username.tsx frontend/src/routes/profile.test.tsx
git commit -m "feat(frontend): show profile posts in the Posts tab"
```

---

### Task 3: Docs and full verification

**Files:**
- Modify: `docs/web-ui.md` (§5.4 profile component table: add Post grid under tabs)
- Modify: `docs/profile-posts-design.md` (status: implemented)
- Add: `docs/profile-posts-plan.md` (currently untracked)

**Interfaces:**
- Consumes: all tasks.
- Produces: docs matching shipped behavior.

- [ ] **Step 1: Update docs**

In `docs/web-ui.md` §5.4, note that the Posts tab renders the member's posts from `memberPosts(username, first: 20)` as `PostCard`s with skeleton/empty/error states. Mark the design status implemented. Commit the plan doc as-is.

- [ ] **Step 2: Verify the full repo**

Run: `just verify`
Expected: PASS — backend tests/build and frontend tests/build green.

- [ ] **Step 3: Commit**

```bash
git add docs/web-ui.md docs/profile-posts-design.md docs/profile-posts-plan.md
git commit -m "docs: document profile posts query and list"
```

# AGENTS.md — Working in the Chafgram repo

Guidance for AI agents (and humans) contributing to this repository. Read this first, then
[`product.md`](product.md) for what Chafgram is and [`design.md`](design.md) for how it
looks and behaves.

## What this is

Chafgram is an **open-source (MIT), self-hosted private social platform for companies**
(short video, watch, posts, comments, follows, direct messages). See `product.md` — do not
assume public-social-network semantics anywhere in the codebase.

## Repository layout

```
backend/    Spring Boot 4 (Java 21) service — Maven wrapper, package io.github.james5635.chafgram
frontend/   TanStack Start (React 19, Vite, Tailwind v4) app — file-based routing
product.md  Product definition (what/who/why, goals, non-goals)
design.md   Design system (tokens, components, motion, a11y)
README.md   Human-facing overview and quick start
.agents/    Local agent tooling/skills — not part of the product, don't ship it
```

## Commands

### Backend (`backend/`)

```bash
./mvnw spring-boot:run     # run dev server (default port 8080)
./mvnw test                # run tests
./mvnw package             # build jar (target/)
```

- Requires JDK 21. MySQL + Flyway are the configured persistence stack; a datasource is
  **not yet configured** in `application.properties` — add one (or a profile) before
  running anything that touches the database. Never commit credentials.
- GraphQL client codegen (DGS) runs on `generate` against
  `src/main/resources/graphql-client/` → `target/generated-sources`, package
  `io.github.james5635.chafgram.codegen`. Generated code is not committed source you hand-edit.
- Migrations go in `src/main/resources/db/migration/` as `V<n>__snake_case.sql` (Flyway).
- Both `spring-boot-starter-webmvc` and `-webflux` are present; prefer the MVC/servlet path
  unless a reactive endpoint is explicitly needed.

### Frontend (`frontend/`)

```bash
npm install
npm run dev                # dev server on port 3000
npm run build              # production build (also regenerates the route tree)
npm run preview            # serve the production build
```

- Routes are files in `src/routes/`; `src/routeTree.gen.ts` is **generated — never edit
  it**. Add a route by adding a file; the tree regenerates on dev/build.
- Styling is Tailwind v4 (CSS-first, via `@tailwindcss/vite`) plus design tokens in
  `src/styles.css`. Use the tokens (`--lagoon`, `--sea-ink`, …), not raw hex; see
  `design.md`.
- Light/dark/auto theming: inline `THEME_INIT_SCRIPT` in `src/routes/__root.tsx` sets
  `data-theme` on `<html>` before paint; mode persists in `localStorage["theme"]`. Keep
  them consistent if you touch either.
- Devtools (TanStack Router/Devtools) render in the corner in dev; leave them out of any
  production-only code paths you author.
- `frontend/AGENTS.md` contains an `intent-skills` block: before substantial work here,
  run `npm exec --no -- intent list` and load a matching local skill if one exists.

## Conventions

- **Java:** 21, Lombok for boilerplate, constructor injection, records for DTOs where
  practical. Keep the base package `io.github.james5635.chafgram`.
- **API surface:** GraphQL for app data + REST/springdoc-openapi for ops/health or where
  OpenAPI consumers need it; WebAuthn/passkeys for strong auth (Spring Security). Choose
  deliberately per endpoint and document it in the PR/commit.
- **Frontend:** TypeScript strict; function components; colocate components in
  `src/components/`; routes thin, components dumb; server logic via TanStack Start server
  functions, not ad-hoc fetch wrappers.
- **Commits/PRs:** small, imperative subjects; describe *why*, not just *what*.
- **Docs-first for direction:** if a change contradicts `product.md`/`design.md`, update
  the doc in the same change or flag it — don't fork the truth in code.

## Guardrails

- Never commit secrets (`application.properties` with real credentials, `.env`, keys).
  Use env vars or local, git-ignored profiles.
- Don't commit build output: `backend/target/`, `frontend/.output/`, `dist/`,
  `node_modules/`.
- Don't vendor fonts/CDN assets silently; the app currently loads Google Fonts via
  `styles.css` `@import` — flag it if self-hosting is needed for offline deployments
  (it will be, for on-prem air-gapped installs — see product.md delivery model).
- No telemetry/analytics without explicit, opt-in configuration. Chafgram's promise is
  that company data stays with the company.
- Treat `HELP.md` (Spring Initializr reference) as boilerplate; keep or remove, don't
  extend it.
- Ask before deleting or rewriting anything you didn't create; the repo is young but
  every file is someone's decision.

## Definition of done for a change

1. Backend: `./mvnw test` green; migration (if any) is forward-only and reversible by a
   documented restore; OpenAPI/GraphQL schema updated where applicable.
2. Frontend: `npm run build` clean; new UI verified in light/dark/auto; keyboard focus
   visible; tokens used, not magic values.
3. Docs updated (`README.md`, `product.md`, `design.md`, this file) if behavior, scope, or
   decisions changed.
4. No secrets, no generated files, no `.agents/` content in the diff.

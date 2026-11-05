# Chafgram

**Chafgram is an open-source, self-hosted private social platform for companies.**

Admins deploy Chafgram on their own infrastructure; their people get the full modern
social experience — short video, watch, posts, comments, follows, and direct messages —
inside the company, with no data ever leaving their control. No public sign-up, no
algorithmic feed for strangers, no phone-home.

- **License:** [MIT](LICENSE)
- **Delivery:** self-hosted / on-premise (single-tenant per company)
- **Status:** early foundation — architecture and design system are in place; product
  features are being built

## Why

Companies want the engagement and culture density of public social media without the
exposure. Work chat carries messages, not culture; public networks can't hold internal
conversation. Chafgram is the internal social layer: familiar to use, private by default,
open source under MIT. See **[product.md](product.md)** for the full product definition.

## What it includes

- Short-video feed and watch
- Posts, comments, and reactions
- Follows and a personal, work-relevant feed
- Direct messages
- Admin workspace: member management, moderation, configuration

(Feature scope and open questions: [product.md](product.md).)

## Tech stack

| Layer    | Technology |
|----------|------------|
| Backend  | Spring Boot 4 (Java 21), Spring Security + WebAuthn (passkeys), Spring Data JPA + MySQL, Flyway migrations, Spring GraphQL (+ REST via springdoc-openapi), Actuator |
| Frontend | TanStack Start (React 19), TanStack Router (file-based), Vite, Tailwind CSS v4, TypeScript |
| Design   | "Sea/lagoon" system — Fraunces + Manrope, token-driven light/dark/auto themes — see [design.md](design.md) |

## Repository layout

```
backend/    Spring Boot service (Maven wrapper)
frontend/   TanStack Start web app
product.md  Product definition
design.md   Design system
AGENTS.md   Guide for AI agents and contributors
```

## Quick start

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Requires JDK 21. MySQL must be configured before database-backed features will run —
set the datasource via environment variables or a local profile (never commit
credentials). Migrations are applied by Flyway from `db/migration/`.

```bash
./mvnw test        # run backend tests
```

### Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

```bash
npm run build      # production build
npm run preview    # serve the production build
```

## Documentation

- **[product.md](product.md)** — what Chafgram is, who it's for, goals/non-goals, open
  questions
- **[design.md](design.md)** — design system: tokens, theming, components, motion,
  accessibility
- **[AGENTS.md](AGENTS.md)** — repo conventions, commands, and guardrails for contributors
  and AI agents

## Contributing

1. Read [AGENTS.md](AGENTS.md) and the relevant product/design docs.
2. Keep changes small and consistent with the existing structure and tokens.
3. `./mvnw test` and `npm run build` must pass; update docs when behavior or scope changes.

## License

[MIT](LICENSE) © Chafgram contributors.

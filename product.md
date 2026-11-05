# Chafgram — Product

> Status: foundational product document. Anything marked **(proposal)** is a recommendation
> made by the author, not a decision from the project owner, and should be confirmed or
> replaced as the product matures.

## Summary

Chafgram is an **open-source, self-hosted private social platform for companies**.

A company runs Chafgram on its own infrastructure. Admins set up a private workspace for
their people, and everyone inside that company gets the full modern social experience —
short video, posts, comments, follows, watch, and direct messages — without anything ever
leaving the organization's control or touching the public social web.

- **License:** MIT
- **Delivery:** self-hosted / on-premise, run by the company itself (not a hosted SaaS)
- **Model:** open source — any company can download, deploy, and run Chafgram
- **Audience:** companies and their employees (B2B, sold/adopted via the organization, not
  as a consumer app)

## The problem

Public social media (Instagram, TikTok, X, LinkedIn) is built for open, performative,
algorithmic distribution between strangers. Companies want the engagement, culture, and
communication density those products create — but none of the downsides:

- **Data residency / privacy:** internal conversations, faces, and culture must not live on
  a third party's servers or feed an advertising algorithm.
- **Context collapse:** employees don't want to share with the public, customers, or
  competitors; public platforms blend audiences that must stay separate.
- **Trust and safety:** internal spaces can enforce company norms without the content
  moderation problems of the public web.
- **Tool fragmentation:** work chat (Slack/Teams) carries messages; it does not carry
  culture. Announcements get read, but company life — the videos, the posts, the comments,
  the celebrations — has no home inside the firewall.

Chafgram closes that gap: the social layer a company would build for its own people, if it
were on the public internet — except it isn't, it's on the company's own hardware.

## Who it is for

| Role | What they do with Chafgram |
|------|----------------------------|
| **Company / organization** | The customer. Decides to adopt Chafgram, provides infrastructure, and owns the data. |
| **Admin** | Installs and operates the instance: creates the workspace, manages members and roles, configures branding, moderation, retention, and access. |
| **Member (employee)** | The everyday user: watches, posts, comments, follows colleagues, messages people directly. |

## What Chafgram does

The product is the whole social suite, not a stripped-down MVP:

- **Short video feed** — create, publish, and watch short-form video from within the company.
- **Watch** — browse and play video across the workspace (feed and direct viewing).
- **Posts** — text/image/link posts to personal or workspace feeds.
- **Comments** — threaded conversation on posts and videos.
- **Follows** — follow colleagues and topics; shape a personal, work-relevant feed.
- **Chat / direct messages** — private 1:1 (and, where supported, group) messaging.
- **Notifications** — activity that concerns you (new followers, comments, mentions, messages). *(proposal)*
- **Moderation & administration** — admin tooling to enforce company norms: member management,
  content actions, audit/visibility controls. *(proposal — scope to be defined)*

Capability areas not explicitly decided yet are listed as open questions at the bottom of
this document; nothing is silently in or out.

## Positioning

Chafgram is **not**:

- a public social network (there is no open sign-up, no cross-company graph, no public
  discovery)
- a work-chat replacement like Slack or Microsoft Teams (chat is a feature; the product is
  the social layer — feed, video, culture)
- an intranet CMS / document portal (it is for conversation and culture, not file storage)

Chafgram **is** the internal equivalent of the public social products people already know
how to use — deployed privately, owned by the company, open source.

**Value proposition in one line:** the social network your people actually enjoy, running
on your own servers, under MIT.

## Delivery & licensing

- **Self-hosted / on-premise.** The company installs and runs Chafgram on its own
  infrastructure (typically its cloud account or datacenter). The vendor/authors do not
  operate it for them.
- **Single-tenant per company.** One company, one deployment, one workspace boundary.
  Multi-workspace hosting is out of scope for the current design. *(proposal)*
- **Open source under MIT.** Anyone may use, modify, and redistribute Chafgram, including
  commercially, with no copyleft obligations and no license fee.
- **No data leaves the company.** Because the deployment is self-hosted, member data,
  videos, and messages live entirely on infrastructure the company controls.

## Goals

1. Give companies a genuinely engaging internal social experience — not a glorified
   announcement board.
2. Make self-hosting real: installable and operable by a competent admin team without
   heroics (documented setup, migrations, backups, upgrades).
3. Keep the codebase open, small enough to read, and pleasant to contribute to (MIT).
4. Preserve the member experience as the product scales: fast feed, fast video, reliable
   messaging.

## Non-goals

- Public or consumer-facing social networking.
- Competing with Slack/Teams on real-time work coordination (huddles, threads-for-tasks,
  integrations with ticketing/CI).
- Content monetization, ads, or creator payouts.
- Hosting-as-a-service operated by the Chafgram authors.
- Cross-company federation/discovery (at least for the foreseeable roadmap).

## Anti-goals (what would make this a failure)

- **Anything that exfiltrates data** — phone-home telemetry, external analytics, mandatory
  cloud dependencies, or "anonymous usage reporting" without explicit opt-in.
- **Feature-parity creep into generic admin tooling** — becoming a half-baked HR suite.
- **Open source in name only** — a crippled community edition with the real product behind
  a paywall. MIT means the whole thing.
- **A feed members can't find value in** — if it feels like mandatory fun, it has failed.

## Product principles

1. **Private by default, social by design.** Every decision favors the closed, internal
   context over openness.
2. **The company owns everything.** Data, uptime, moderation, backups — all theirs.
3. **Familiarity over novelty.** Members shouldn't need training; it should feel like the
   social apps they already use, minus the public.
4. **Admins are customers too.** Installation, upgrades, and moderation must be
   first-class, documented experiences.
5. **Boring technology, reliable product.** Prefer proven components over exciting ones;
   Chafgram is infrastructure for culture, and infrastructure must be dependable.

## Success measures (proposal)

Concrete targets to be set with the owner; initial measures worth tracking:

- Companies deployed (instances in production).
- Weekly active members per instance / adoption vs. headcount.
- Content created per active member per week (posts, videos, comments).
- D7/D30 retention of new members.
- Median time-to-first-post for a newly onboarded company (setup → first content).

## Roadmap shape (proposal)

Phasing to be decided by the owner; a sensible order:

1. **Foundation** — identity/workspace, member management, admin console, deploy story
   (Docker/Compose or Helm), Flyway migrations, observability via Actuator.
2. **Core social** — posts, comments, follows, feed, notifications.
3. **Video** — upload, transcoding, feed playback, watch.
4. **Messaging** — direct messages and presence.
5. **Polish & hardening** — moderation tooling, retention controls, backup/restore docs,
   upgrade path, accessibility passes.

## Open questions

These change what gets built and must be answered by the owner:

1. **Identity integration** — built-in accounts only, or SSO/LDAP/OIDC against the
   company's directory? (Backend already includes Spring Security + WebAuthn — passkeys —
   which suggests built-in strong auth; directory sync is still undecided.)
2. **Video pipeline** — self-hosted transcoding (e.g. ffmpeg workers) vs. external
   service; storage backend (local disk vs S3-compatible).
3. **Messaging transport** — WebSocket/STOMP vs. WebFlux/SSE; whether messaging is
   real-time only or also offline-tolerant.
4. **Moderation model** — admin-only roles, or trusted member roles; audit logging scope.
5. **Content retention** — configurable retention policies, legal hold, export/e-discovery.
6. **Data export** — member data export (GDPR-style) for the company's own obligations.
7. **Release & support model** — release cadence, upgrade guarantees, security disclosure
   process for an MIT project.
8. **Mobile** — responsive web only, PWA, or native companions?
9. **Naming of the unit of deployment** — "workspace", "instance", or "org" in UI copy and
   docs.
10. **The `.agents/` skills folder** currently in the repo is a local toolchain, not part
    of the product; keep it out of release artifacts.

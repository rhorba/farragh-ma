# DECISIONS — Farragh.ma



## 2026-07-04 — Stack pivot
DECISION: Replace Next.js/TS stack (original README) with Java 25 + Spring Boot 3 + Maven backend, Angular 21 frontend, PostgreSQL 16 + PostGIS retained.
Reason: user directive to standardize on Java/Angular LTS stack.
DECISION: Deploy via Docker only for now; Kubernetes deferred until a concrete scaling/orchestration need arises (YAGNI).
DECISION: Playwright video recording deferred until the final sprint of this project version, per explicit user instruction (deviates from default "every version completion" rule — logged as intentional override).

## 2026-07-04 — Scope decision
DECISION: Build COMPREHENSIVE scope — full README feature set (Household/SME, Recycler, Municipality bulk-subscribe, Admin), PostGIS geo zone matching, CMI mock payments, email notifications, FR+AR i18n.
Reason: user explicit choice over Simple/Balanced alternatives.

## 2026-07-04 — Architecture decisions (ADR-1..4)
ADR-1: Layered, package-by-feature (not hexagonal/DDD) — CRUD-heavy domain, low ceremony preferred.
ADR-2: PaymentGateway interface isolates CMI mock/real swap.
ADR-3: Stateless JWT auth with role claims + resource ownership checks.
ADR-4: PostGIS zone matching via repository query (ST_Contains/ST_DWithin), not in-app spatial logic.

## 2026-07-04 — Local toolchain blocker resolved
BLOCKER: Local machine has Java 21 + no Maven; target is Java 25 LTS.
DECISION: Docker-only builds. Backend build/test/run always via Docker (JDK 25 + Maven baked into the image) and CI. No local Java/Maven install required. Frontend (Angular) runs natively via Node/npm since that's already installed and gives fast reload; backend+DB run in Docker during local dev.

## DECISION — 2026-07-06 — Sprint 3 approach
Race protection (Story 3.3): conditional atomic UPDATE (SET status=ACCEPTED WHERE id=? AND status=POSTED), 0 rows affected -> 409. No schema migration; relies on Postgres row-level locking as DB-level optimistic concurrency guard.
Story 3.1 scope: full-stack (backend + frontend zone-declaration UI), not deferred like Story 2.0 was.
Zone matching (3.2): repository query using ST_Contains(area,...) OR ST_DWithin(center_point,...,radius_m) per ADR-4, filtered by recycler_materials join, status=POSTED. Re-check zone membership server-side in accept() too (not just feed filtering) per Test Strategy IDOR item.

## DECISION — 2026-07-06 — Adversarial checklist re-review outcome (Sprint 3)
Reviewed docs/test-strategy-farragh-marketplace.md checklist against the final diff:
- Household/Recycler IDOR: covered (existing tests).
- Race condition (two recyclers accept same request): covered (existing concurrency test).
- Self-intersecting zone geometry -> 400 not 500: covered (existing test).
- Payment double-submit: N/A, payment feature not built yet (correctly deferred to a later sprint).
- ST_DWithin exact boundary edge: intentionally skipped (already logged prior session) - flaky to construct a precise geodesic boundary point; ST_DWithin's inclusive (<=) behavior is documented as intentional.
- JWT signature tampering and Arabic/unicode address text: found untested (gap). User chose to add both now rather than defer.
Added: RequestsControllerTest.tamperedTokenSignatureIsRejected (flips last char of a valid JWT, expects 401) and .arabicAddressTextIsPersistedAndReturnedUnaltered (Arabic addressText/quantityDesc round-trips exactly). Backend suite now 32/32 green, coverage unchanged at 89% instruction / ~92% line.

## 2026-07-15 — Coverage-closure sprint depth
Chose Balanced approach: happy path + primary error/validation cases per component, matching the depth of Sprint 3's recycler-feed.component.spec.ts (TestBed + HttpTestingController pattern). Not doing full adversarial/unicode edge cases or re-baselining the CI gate to whole-project 80% this sprint.

## 2026-07-15 — Sprint 4 scope
Chose Comprehensive: backend (Stories 4.1 status state machine + 4.2 async email) + recycler "My Accepted Requests" UI (closes the UX-doc gap, same precedent as Sprint 2's missing auth story) + an admin preview pulled forward from Story 5.2 (search users/requests, read-only, no deactivate - that stays Story 5.3 in Sprint 5).

## 2026-07-15 — Sprint 5 scope
Story 5.2 (admin search) already satisfied by Sprint 4's admin-preview pull-forward - no new work needed, marking done. Chose: Story 5.1 (mock payment) backend + UI (Pay button on request-detail, closes UX-doc gap like Sprint 3/4). Story 5.3 (admin deactivate, lowest "Could" priority) deferred to a later sprint to keep this sprint focused on the Must-priority item.
No pricing model exists anywhere in the docs (PRD/database doc) - using a flat mock amount (5000 cents / 50 MAD) as a placeholder constant since Story 5.1 only requires a payment *record*, not a real pricing engine. Documented in code as a stub pending a real pricing model (out of scope, not requested).

## 2026-07-17 — Sprint 7 execution order
Chosen: strict sequential order, one story at a time with checkpoints (7.1 -> 7.2 -> 7.3 -> 7.4 -> 7.5). Simplest to review, lowest risk of tangled failures across this release-gate sprint. Rejected: merging 7.1+7.2 and 7.3+7.4 into batches (slightly faster but less granular checkpointing); comprehensive extra hardening beyond the documented checklist (scope creep, against YAGNI - checklist already exists in Test Strategy doc).

## 2026-07-18 — Story 7.3 E2E setup approach
Chosen (Balanced): new top-level `e2e/` folder, Playwright (Chromium only, no cross-browser requirement anywhere in the docs) targets the existing `docker-compose.yml` stack (db+backend+frontend+reverse-proxy) on localhost:80 - the same stack hand-verified via curl every sprint since browser extension connectivity has been broken all project. CI job brings the stack up with `--wait`, runs the suite, tears it down.
Reason: reuses the already-established manual-verification pattern, exercises the real prod-like nginx routing (not just `ng serve`), and sets up Story 7.5 (prod-profile docker-compose deploy) for free since it's the same compose file.
Rejected: Simple (`ng serve`-only webServer, backend/db assumed pre-running) - weaker CI story, doesn't exercise real routing. Comprehensive (multi-browser matrix + trace/sharding) - no stated cross-browser requirement, scope creep against YAGNI.

## 2026-07-19 — Story 7.5 TLS scope
Chosen: skip TLS on the reverse-proxy (stay HTTP-only on :80), noted with a comment in `docker-compose.yml`. Reason: no real host/domain exists to issue certs for (confirmed 2026-07-17: "production deploy" = local docker-compose standing in for prod, not a real remote host) - a self-signed cert would verify nothing real and adds complexity for no benefit at this stage. Revisit once an actual prod host/domain exists.
Also added `restart: unless-stopped` to all 4 services (was documented in docs/devops-farragh-marketplace.md's "production shape" and the monitoring table but missing from the actual compose file - real gap, now closed) and healthchecks for `frontend`/`reverse-proxy` (previously only `backend`/`db` had them).
Bug found and fixed while verifying: both new healthchecks initially used `http://localhost/...`, which failed with "connection refused" inside the frontend/reverse-proxy containers - the custom `nginx.conf` only binds IPv4 (`listen 80`, no `listen [::]:80`), but the container's `localhost` resolves IPv6 (`::1`) first, so the healthcheck hit a socket nothing was listening on. Fixed by pointing both healthchecks at `127.0.0.1` explicitly instead of `localhost`.

## 2026-07-20 — gitleaks false positive on E2E fixture passwords
Chosen: add `.gitleaks.toml` with a single `[allowlist]` table scoped by `paths` regex to just `e2e/tests/golden-path.*.spec.ts`. Reason: the flagged strings (`RecyclerPass123`, `HouseholdPass123`) are passwords for ephemeral test accounts created fresh every CI run via `uniqueEmail()`, not real secrets - gitleaks' `generic-api-key` rule fired purely on string entropy. Scoping the exception to the two specific files (rather than disabling the rule globally, or the whole `e2e/` path) keeps the scan meaningful for everything else. Verified locally against `zricethezav/gitleaks:latest` before pushing.
Rejected: disabling gitleaks/the rule entirely (defeats the point of the scan for real future secrets); rewriting the specs to pull passwords from an env var (over-engineering for synthetic, non-sensitive test fixtures that are never real credentials).

## 2026-07-20 — Story 5.3 scope
Chosen (via BRAINSTORM options presented to the user): "Full account-status management" - deactivate + reactivate endpoints, an AdminActionLog audit trail, and an admin UI toggle + history view. Reason: user's explicit pick over the two narrower options (backend-only minimal; backend + self-lockout guard + UI button without an audit trail).
No confirm-dialog modal added for the deactivate/reactivate buttons, staying consistent with every other destructive-ish action in the app (cancel-request, pay) which also fire immediately with no confirmation - introducing a new UI pattern (MatDialog) for just this one feature was rejected as inconsistent scope creep, not because confirmation wouldn't be reasonable in isolation.
Self-deactivation is blocked (409) but reactivating one's own account is not restricted - there's no lockout risk in the reactivate direction, so no guard was added there.

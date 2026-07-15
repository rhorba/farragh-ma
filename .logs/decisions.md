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

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

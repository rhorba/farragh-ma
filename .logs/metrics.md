# METRICS — Farragh.ma



## SPRINT_SNAPSHOT — Sprint 1 — 2026-07-04
Stories completed: 1.1 (scaffolding), 1.2 (Flyway migrations), 1.3 (CI pipeline), 1.4 (Auth register/login)
Tests: 8/8 passing (backend, Testcontainers-based, real Postgres+PostGIS)
Coverage: tracked via CI artifact (backend-coverage); 80% gate deferred to Sprint 7 release job per Test Strategy doc
CI: GREEN on main (security, backend, frontend, build jobs)
Commits pushed: e9a9abe..eb1ef3a (foundation docs + Sprint 1)
Corrections logged: Spring Boot 3.x -> 4.1 (Java 25 incompatibility), BouncyCastle 1.79 -> 1.84 (CVE-2025-14813)
Next: Sprint 2 (Household/SME pickup request core - Stories 2.1-2.3)

## SPRINT_SNAPSHOT — Sprint 2 — 2026-07-05
Stories completed: 2.0 (login/register UI, added retroactively), 2.1 (create request), 2.2 (view/list/detail), 2.3 (cancel)
Backend tests: 14/14 passing (Testcontainers, real Postgres+PostGIS)
Frontend tests: 8/8 passing (AuthService, authGuard, authInterceptor)
E2E: verified live via curl through the actual docker-compose stack (register -> create -> list -> detail -> cancel -> unauthenticated 401)
CI: GREEN on first push (main)
Corrections logged: missing frontend auth story added as 2.0; Spring Security 403->401 entry point fix; Nginx SPA fallback fix
Next: Sprint 3 (Recycler Zones & Matching Feed - Stories 3.1-3.3, highest risk per Test Strategy)

## COVERAGE — 2026-07-06 — Sprint 3
Backend (full suite, JaCoCo): 89% instruction coverage, ~91% line coverage (359 lines, 31 missed). 30/30 tests green (auth 5, recyclers 16, requests 6, app 1, migration 2).
Frontend: whole-project coverage is ~18% (17.37% lines) - pre-existing gap from Sprint 2 (login/register/request-list/request-detail/new-request/status-badge components have no specs). Decision (user-approved): scope Sprint 3's gate to its own new/touched code rather than closing the whole-project gap this session.
Frontend, Sprint 3 scope only (recyclers feature + auth guards, incl. new recycler-profile.component.spec.ts and recycler-feed.component.spec.ts added this session): 94.06% statements, 93.87% lines. 19/19 tests green, lint clean.
Backlog item: write specs for Sprint 2 components (login, register, request-list, request-detail, new-request, status-badge) to close the whole-project 80% gate - not done this session, flagged as a known risk.

## SPRINT_SNAPSHOT — 2026-07-06 — Sprint 3
Stories 3.1-3.3 (Recycler Zones & Matching Feed) complete. Backend 32/32 tests, 89% instruction / ~92% line coverage. Frontend 19/19 tests, lint clean, Sprint-3-scope coverage 94%; whole-project coverage ~18% (pre-existing Sprint 2 gap, logged as risk, not closed this sprint).

## 2026-07-15 — SPRINT_SNAPSHOT: Coverage-closure sprint
Whole-project frontend coverage (was ~18% before this sprint, only auth/* + app.spec.ts had specs):
  Statements: 89.65% (520/580)
  Branches:   93.54% (203/217)
  Functions:  86.58% (71/82)
  Lines:      93.85% (351/374)
Gate: >= 80% combined unit+integration — PASSED (all four metrics clear).
Test files: 13 (was 7), Tests: 48 (was 34 before this sprint, 19 before Sprint 3). Lint: clean.
Remaining known gap (pre-existing, not in this sprint's scope): recycler-feed.component.html template coverage still low (24.39%) - Sprint 3's own spec asserts on component signals but doesn't call fixture.detectChanges() post-flush, same pattern this sprint fixed elsewhere. Small follow-up if the whole-project number needs to climb further.

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

## 2026-07-15 — SPRINT_SNAPSHOT: Sprint 4 (Lifecycle & Notifications + admin preview)
Backend: 44/44 tests green. Jacoco: instruction 90.29%, line 92.25%, branch 68.48% (branch % not the gating metric, consistent with Sprint 3's convention - instruction/line clear the 80% combined gate).
Frontend: 63/63 tests green, lint clean. Coverage: 90.94% statements / 94.74% lines (whole-project, gate cleared).
Adversarial checklist (Auth + Recycler-ownership items, since this sprint touched both - Zone matching/Payment untouched, not re-reviewed):
  - IDOR: recycler cannot schedule/complete a request outside their own accepted-request ownership - covered (2 tests).
  - Role escalation: public self-registration as ADMIN rejected server-side - covered (1 test, new finding+fix this sprint).
  - Role escalation: non-admin cannot call admin endpoints - covered (2 tests, both directions).
  - Race/double-submit on schedule/complete: same atomic-UPDATE-with-WHERE-status-guard pattern as Sprint 3's accept() - structurally safe by construction, no new test added (YAGNI - identical mechanism already race-tested for accept()).
Release gate criteria (per Test Strategy §5) not fully applicable this sprint - full E2E/RTL gate is Sprint 7 scope; this snapshot covers what's in-scope now.

## 2026-07-15 — SPRINT_SNAPSHOT: Sprint 5 (mock CMI payment)
Backend: 48/48 tests green. Jacoco: instruction 91.56%, line 92.95%, branch 69.79% (branch % not the gating metric, same convention as prior sprints).
Frontend: 66/66 tests green, lint clean. Coverage: 91.03% statements / 94.94% lines.
Adversarial checklist (Payment, risk 10 per Test Strategy §2):
  - Double-submit: sequential double-submit covered by test (app-level check-then-act, 409 on second call).
  - Double-submit under true concurrency (two simultaneous requests racing the app-level check): not separately load-tested with a two-thread test like Sprint 3's accept() race test - mitigated by the DB's UNIQUE(pickup_request_id) constraint + saveAndFlush/catch as a second line of defense, but that fallback path itself isn't exercised by an automated test. Documented as a residual gap, same category as the Sprint 3 ST_DWithin-boundary skip.
  - IDOR: household cannot pay for another household's request - covered.
  - Ownership+state guard: payment before COMPLETED rejected - covered.

## 2026-07-16 — SPRINT_SNAPSHOT: Sprint 6 (Municipality & i18n/RTL)
Backend: 54/54 tests green (6 new for municipality bulk-subscribe). Jacoco: instruction 92%, line 93.6% (whole-project, gate cleared).
Frontend: 87/87 tests green (up from 66), lint clean. Coverage: 90.64% statements / 94.56% lines.
Adversarial checklist (Municipality endpoint, new this sprint):
  - Role check: non-MUNICIPALITY role forbidden on POST/GET /api/v1/municipality/subscriptions - covered (403 test).
  - Input validation: missing zone geometry rejected - covered (400 test, reuses the same ZoneGeometryValidator path already adversarially tested for recyclers).
  - IDOR: not applicable - listMySubscriptions is scoped to the authenticated municipality's own id server-side, no id parameter accepted from the client.
  - Overlap detection correctness: same-zone re-submission (radius-vs-radius) and cross-type (polygon-vs-radius) overlap both covered by tests.
RTL gate (Test Strategy §UI RTL strategy): CSS-audited all 13 component stylesheets - 2 real issues found and fixed (directional glyph, one text-align:left), rest already flexbox/logical-property safe. No literal "bottom nav" component exists in the codebase to test (wireframe-only, never built) - the new language-switcher header stood in for it. Full E2E dir=ltr/rtl visual verification deferred - claude-in-chrome still not connecting this session, only unit-spec + CSS-level verification done.

## 2026-07-17 — Sprint 7 Story 7.2: Coverage gate to >=80% combined
Backend: `mvn verify -Pcoverage-gate` (JaCoCo LINE COVEREDRATIO >= 0.80 at BUNDLE level) - PASSED. 56/56 tests green. 92.25% instruction / 93.58% line coverage.
Frontend: `npx ng test --no-watch --coverage` - 87/87 tests green, lint clean. 90.64% statements / 92.95% branch / 86.39% functions / 94.56% lines.
Both individually well clear of the 80% gate; combined project coverage is trivially >=80%. Release gate criterion met.
Known minor residual (not blocking, carried forward from Sprint 3/6): recycler-feed.component.html has lower template-branch coverage (67.39% lines) than the rest of the frontend - previously assessed as an optional follow-up, not re-opened as a blocking risk since whole-project gate clears comfortably.

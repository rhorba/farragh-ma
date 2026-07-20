# RISKS — Farragh.ma



## RISK — 2026-07-06 — Frontend whole-project test coverage below 80% gate
Only core/auth/* (guard, interceptor, service) and app.spec.ts had specs prior to this session; whole-project statement/line coverage measured at ~17-19%. Sprint 2 feature components (login, register, request-list, request-detail, new-request, status-badge) have zero unit tests. This predates Sprint 3 and was not caught in Sprint 2's verify phase (11/11 "green" referred to a small, non-representative test count, not coverage %).
Mitigation this session: Sprint 3's own new frontend code (recyclers profile + feed components) was brought to 94% coverage with 2 new spec files, and the guard already had a spec. The Sprint 2 gap itself was NOT closed - user explicitly chose to scope the gate to Sprint 3 rather than expand this session's work.
Action needed: schedule a dedicated task/sprint to write specs for the 6 untested Sprint 2 components before the whole-project 80% gate can be honestly claimed as met.

## RISK — 2026-07-06 — Frontend whole-project test coverage below 80% gate — CLOSED 2026-07-15
Closed: added specs for all 6 previously-untested Sprint 2 components (status-badge, login, register, new-request, request-list, request-detail) and tightened template-coverage on the new specs by triggering fixture.detectChanges() after signal updates. Whole-project coverage moved from ~18% to 89.65% statements / 93.85% lines - clear of the 80% gate. See .logs/metrics.md 2026-07-15 snapshot.
Residual minor gap (not blocking): recycler-feed.component.html (Sprint 3 code) still has low template-branch coverage (24.39%) because its existing spec doesn't call detectChanges() after HTTP flush. Small, optional follow-up - not re-opening this risk for it.

## RISK — 2026-07-06 (noted, never formally logged) — ST_DWithin boundary test skipped — CLOSED 2026-07-17
The Sprint 3 session log mentioned "ST_DWithin boundary test remains intentionally skipped (documented)" but no formal risk entry existed. Closed in Sprint 7 Story 7.1: root-caused as a genuine sub-millimeter forward/inverse geodesic-solver discrepancy in PostGIS (verified directly), not a business-logic gap. Two new tests added with a 1m margin on each side of the radius boundary. See .logs/activity.md 2026-07-17.

# RISKS — Farragh.ma



## RISK — 2026-07-06 — Frontend whole-project test coverage below 80% gate
Only core/auth/* (guard, interceptor, service) and app.spec.ts had specs prior to this session; whole-project statement/line coverage measured at ~17-19%. Sprint 2 feature components (login, register, request-list, request-detail, new-request, status-badge) have zero unit tests. This predates Sprint 3 and was not caught in Sprint 2's verify phase (11/11 "green" referred to a small, non-representative test count, not coverage %).
Mitigation this session: Sprint 3's own new frontend code (recyclers profile + feed components) was brought to 94% coverage with 2 new spec files, and the guard already had a spec. The Sprint 2 gap itself was NOT closed - user explicitly chose to scope the gate to Sprint 3 rather than expand this session's work.
Action needed: schedule a dedicated task/sprint to write specs for the 6 untested Sprint 2 components before the whole-project 80% gate can be honestly claimed as met.

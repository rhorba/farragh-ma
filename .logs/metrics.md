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

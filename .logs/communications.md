# COMMUNICATIONS — Farragh.ma



## 2026-07-04 — UNDERSTAND phase
User request: pivot stack to Java + Angular (latest LTS), full sprint backlog, strict skill adherence, push every sprint, no video recording until final sprint, Docker-only deploy (K8s only if needed).
Clarified via questions:
- Backend: Spring Boot 3.x + Maven, Java 25 LTS
- Frontend: Angular 21 LTS
- DB: PostgreSQL 16 + PostGIS (kept from original README — needed for coverage-zone geo queries)
- Git remote: github.com/rhorba/farragh-ma (existing repo, now wired as origin)
- Doc-first: full expert doc chain this session per CLAUDE.md rule 13

## 2026-07-04 — Env vars collected
User: fill .env.example with placeholders now; CMI runs in CMI_MODE=mock (no real gateway calls) until real merchant credentials are available.

## 2026-07-15
User chose: coverage-closure sprint (write specs for 6 untested Sprint 2 frontend components) over Sprint 4 feature work. No new env vars needed for this task.

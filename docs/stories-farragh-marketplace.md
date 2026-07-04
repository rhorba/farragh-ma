# Stories: Farragh.ma — Full Sprint Backlog
**PRD**: docs/prd-farragh-marketplace.md
**Architecture**: docs/architecture-farragh-marketplace.md
**Test Strategy**: docs/test-strategy-farragh-marketplace.md

## Epic 1: Foundation & Infrastructure
Scaffolding, CI/CD, and auth so every later epic has something to build on.

### Story 1.1: Repo scaffolding (Spring Boot + Angular + Docker Compose)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev + DevOps
As a dev team, I want a working skeleton for backend, frontend, and local Docker Compose, so that all later work has a foundation.
- Given a fresh clone, when `docker compose up` runs, then backend `/actuator/health` and frontend root both respond 200.
**Technical Notes**: Maven project per Architecture §3 module layout; Angular workspace with Angular Material installed (UI doc). Uses docker-compose.yml from DevOps doc.

### Story 1.2: DB migrations baseline (Flyway + PostGIS)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
As a dev team, I want Flyway migrations V1-V8 applied automatically on startup, so that schema is reproducible.
- Given a fresh DB container, when the backend starts, then all tables from Database doc exist with PostGIS extension enabled.
**Technical Notes**: Database doc §3/§5.

### Story 1.3: CI pipeline green on first push
**Priority**: Must | **Size**: S | **Specialist**: DevOps
As a dev team, I want the GitHub Actions pipeline (lint, test+coverage, security scan, build) running on every push, so that CI enforces quality from day one.
- Given a push to any branch, when CI runs, then backend+frontend+security jobs all complete and report status.
**Technical Notes**: DevOps doc §2. Per CLAUDE.md rule 11, must be watched to green after every push, not just set up.

### Story 1.4: Auth — register & login (all 4 roles)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Security
As a user of any role, I want to register and log in, so that I get a role-scoped JWT.
- Given valid registration data, when I register, then a user row is created with argon2id-hashed password and correct role.
- Given valid credentials, when I log in, then I receive an access token (≤15min) and refresh token (≤7d) with my role claim.
- Given 5 failed login attempts, when I try again, then I'm rate-limited (Security doc STRIDE — DoS row).
**Technical Notes**: Architecture ADR-3, Security doc §3/§4.

**Sprint 1 total**: Stories 1.1-1.4

---

## Epic 2: Pickup Request Core (Household/SME)
### Story 2.1: Create pickup request
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
As a Household/SME, I want to post a pickup request with material, quantity, address+pin, so that recyclers can find it.
- Given required fields filled, when I submit, then a request is created with status=POSTED and a `geography(Point)` location.
- Given the address/pin is missing, when I submit, then I see an inline validation error (no server round-trip needed for empty check).
**Technical Notes**: UX Flow 1, DB `pickup_requests` table.

### Story 2.2: View my requests + detail
**Priority**: Must | **Size**: S | **Specialist**: Frontend Dev
As a Household/SME, I want to see my requests list and detail, so that I can track status.
- Given I have requests, when I open "My Requests", then I see them with status badges (UI doc component inventory).
- Given I have none, when I open the list, then I see the empty state from UX doc §5.

### Story 2.3: Cancel a request
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev
As a Household/SME, I want to cancel a request before it's completed, so that I'm not stuck with a stale request.
- Given status is POSTED or ACCEPTED, when I cancel, then status becomes CANCELLED and the recycler (if any) is notified.
- Given status is COMPLETED, when I try to cancel, then the API rejects it (state machine, per Test Strategy scenario).

**Sprint 2 total**: Stories 2.1-2.3

---

## Epic 3: Recycler Zones & Matching Feed
### Story 3.1: Recycler declares coverage zone + materials
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
As a Recycler, I want to declare a coverage zone (polygon or radius) and accepted materials, so that I only see relevant requests.
- Given valid zone geometry, when I save it, then it's stored in `coverage_zones` and linked to my recycler ID.
- Given invalid/self-intersecting geometry, when I submit, then I get a clear validation error, not a 500 (Test Strategy adversarial checklist).

### Story 3.2: Matched request feed (PostGIS)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev
As a Recycler, I want to see only requests inside my zone and matching my materials, so that my feed is relevant.
- Given a request inside my zone with a material I accept, when I open my feed, then I see it (Test Strategy Scenario 1).
- Given a request outside my zone, when I open my feed, then I do NOT see it, AND a direct API call to accept it is rejected server-side (Scenario 2 — IDOR-class check, not client-side filtering).
**Technical Notes**: Architecture ADR-4, DB `idx_requests_location` GiST index. This is a risk-13 component — adversarial review required before merge (Test Strategy §4).

### Story 3.3: Accept a request (with race protection)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
As a Recycler, I want to accept a request, so that it moves to my accepted list.
- Given a POSTED request I'm eligible for, when I accept, then status→ACCEPTED and `accepted_by_recycler_id` is set.
- Given two recyclers accept simultaneously, when both requests race, then exactly one succeeds and the other gets a clean conflict response (Test Strategy adversarial checklist — DB-level unique/optimistic-lock constraint required).

**Sprint 3 total**: Stories 3.1-3.3 (highest-risk sprint — allocate extra review time)

---

## Epic 4: Request Lifecycle & Notifications
### Story 4.1: Status transitions (Accepted → Scheduled → Completed)
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
As a Recycler, I want to progress a request's status, so that the requester knows what's happening.
- Given status=ACCEPTED, when I set SCHEDULED then COMPLETED in order, then each transition succeeds.
- Given status=POSTED, when someone attempts to jump directly to COMPLETED, then it's rejected (Test Strategy Scenario 4 state machine).

### Story 4.2: Email notifications on key transitions
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev
As a Household/SME, I want an email when my request is accepted/completed, so that I don't have to keep checking the app.
- Given a request transitions to ACCEPTED or COMPLETED, when the transition commits, then an async email is sent (System Design: `@Async`, no queue).

**Sprint 4 total**: Stories 4.1-4.2

---

## Epic 5: Payments & Admin
### Story 5.1: Mock CMI payment on completed request
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev
As a Household/SME, I want to pay for a completed pickup, so that the transaction is recorded.
- Given status=COMPLETED and no existing payment, when I trigger payment, then a payment record is created with mode=MOCK, status=SUCCEEDED (Test Strategy Scenario 3).
- Given a payment already exists for that request, when I trigger payment again, then the API rejects the duplicate (double-submit protection, Test Strategy adversarial checklist).
**Technical Notes**: Architecture ADR-2 `PaymentGateway` interface — implement `MockCmiGateway` only this sprint.

### Story 5.2: Admin — view/search users & requests
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
As an Admin, I want to search users and requests, so that I can support users.
- Given I'm logged in as Admin, when I search by email or status, then I get filtered, paginated results.
- Given I'm logged in as a non-Admin role, when I call an admin endpoint directly, then I get 403 (role-escalation adversarial check).

### Story 5.3: Admin — deactivate a user
**Priority**: Could | **Size**: S | **Specialist**: Backend Dev
As an Admin, I want to deactivate a problematic account, so that they can no longer log in.
- Given an active user, when Admin deactivates them, then `is_active=false` and subsequent login attempts fail.

**Sprint 5 total**: Stories 5.1-5.3

---

## Epic 6: Municipality & Localization
### Story 6.1: Municipality bulk-subscribe a zone
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev
As a Municipality, I want to bulk-subscribe a neighborhood zone, so that residents get default coverage.
- Given a valid zone, when I create a subscription, then it's stored and marked active.
- Given a zone overlapping an existing subscription, when I submit, then I see a warning but can still confirm (UX Flow 3).

### Story 6.2: FR/AR language switch + RTL
**Priority**: Must | **Size**: L | **Specialist**: Frontend Dev + UI Designer
As any user, I want to use the app in French or Arabic, so that I'm comfortable in my language.
- Given I select Arabic, when the UI re-renders, then `dir="rtl"` is applied and Angular Material components mirror correctly (UI doc RTL strategy).
- Given each custom component (status badge, request card, zone tool, bottom nav), when tested in both directions, then layout and text are correct (Test Strategy gate item).

**Sprint 6 total**: Stories 6.1-6.2

---

## Epic 7: Hardening & Release
### Story 7.1: Adversarial fixes (Auth, Zone matching, Payment)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Security Engineer
Run the full Test Strategy §4 adversarial checklist against Auth, Zone matching, and Payment; fix every finding.
- Given the checklist, when each item is executed, then no critical/high finding remains open.

### Story 7.2: Coverage gate to ≥80% combined
**Priority**: Must | **Size**: M | **Specialist**: Tester
Close any coverage gaps found by CI's JaCoCo/Angular coverage reports until the combined gate passes.
- Given the CI coverage report, when combined unit+integration is measured, then it is ≥80% (CLAUDE.md rule 6).

### Story 7.3: E2E Playwright suite — critical paths
**Priority**: Must | **Size**: M | **Specialist**: Tester
Automate the E2E happy path: post request → recycler accepts → status progression → mock payment, in both FR and AR.
- Given the suite runs in CI, when it completes, then all critical-path scenarios pass.

### Story 7.4: Video recording of E2E flows (final sprint only)
**Priority**: Must | **Size**: S | **Specialist**: Tester
Per explicit user instruction, record Playwright video only at this final sprint, not every sprint.
- Given the E2E suite runs with video enabled, when it completes, then `.recordings/v1.0-[date].webm` is saved and logged.

### Story 7.5: Production deploy via Docker Compose
**Priority**: Must | **Size**: M | **Specialist**: Deployment + DevOps
Deploy the full stack (backend, frontend, db, reverse-proxy) to the production host via Docker Compose.
- Given a green CI run on `main`, when `docker compose up -d` runs on the prod host, then health checks pass for all containers.

**Sprint 7 total**: Stories 7.1-7.5

---

## Sprint Allocation Summary
| Sprint | Epic Focus | Stories | Risk Note |
|---|---|---|---|
| Sprint 1 | Foundation & Infra | 1.1, 1.2, 1.3, 1.4 | Auth is risk-10 — don't rush |
| Sprint 2 | Household Request Core | 2.1, 2.2, 2.3 | Standard |
| Sprint 3 | Recycler Zones & Matching | 3.1, 3.2, 3.3 | **Highest risk (13)** — PostGIS + race conditions |
| Sprint 4 | Lifecycle & Notifications | 4.1, 4.2 | Standard |
| Sprint 5 | Payments & Admin | 5.1, 5.2, 5.3 | Payment is risk-13 |
| Sprint 6 | Municipality & i18n | 6.1, 6.2 | RTL is risk-9, don't retrofit |
| Sprint 7 | Hardening & Release | 7.1, 7.2, 7.3, 7.4, 7.5 | Release gate — nothing ships from here without it passing |

Every sprint ends with: tests passing, coverage checked (full gate enforced by Sprint 7, but each sprint should not regress it), `git push origin main`, and a log entry — per CLAUDE.md rules 6/7. Video recording is explicitly **not** required until Sprint 7 (user override of the default per-version rule).

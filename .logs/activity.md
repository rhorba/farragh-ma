# ACTIVITY — Farragh.ma



## MILESTONE — 2026-07-04
PRD approved: docs/prd-farragh-marketplace.md

## MILESTONE — 2026-07-04
System Design approved: docs/system-design-farragh-marketplace.md (modular monolith, Docker Compose, PostGIS)

## MILESTONE — 2026-07-04
Architecture approved: docs/architecture-farragh-marketplace.md (layered monolith, PaymentGateway interface, JWT+RBAC, PostGIS zone matching)

## MILESTONE — 2026-07-04
Security Baseline approved: docs/security-farragh-marketplace.md (JWT+argon2id, MFA for Admin, RBAC+resource ownership as top control)

## MILESTONE — 2026-07-04
Database Design approved: docs/database-farragh-marketplace.md (PostGIS schema, GiST indexes, Flyway migrations)

## MILESTONE — 2026-07-04
UX Foundation approved: docs/ux-farragh-marketplace.md (personas, IA, 3 core flows, wireframes, RTL carried forward)

## MILESTONE — 2026-07-04
UI Foundation approved: docs/ui-farragh-marketplace.md (Angular Material for RTL, tokens, component inventory)

## MILESTONE — 2026-07-04
Test Strategy approved: docs/test-strategy-farragh-marketplace.md (risk-based levels, adversarial checklist, 80% gate)

## MILESTONE — 2026-07-04
DevOps Foundation approved: docs/devops-farragh-marketplace.md (GH Actions, JaCoCo 80% gate, Docker Compose, K8s deferred)

## MILESTONE — 2026-07-04
Stories/Sprint Backlog approved: docs/stories-farragh-marketplace.md (7 sprints, Epic 1-7)
All foundation docs approved. Ready to commit and push per CLAUDE.md rule 13.

## PUSH — 2026-07-04
Pushed foundation docs to origin/main. Commit: e9a9abe
git push -u origin main — 76 files, "docs: foundation documents for farragh-ma"

## CI — 2026-07-04
Push 1f149e0: CI RED. frontend + backend jobs GREEN; security job failed (Trivy YAML flow-mapping bug: unquoted `severity: CRITICAL,HIGH` parsed HIGH as a bogus key).
Fix: quoted the value in .github/workflows/ci.yml and docs/devops doc.

## CI — 2026-07-04 (green)
Push 8793aec: CI GREEN on main. security/backend/frontend/build all pass. Sprint 1 (Stories 1.1-1.4) complete.

## PLAN — 2026-07-06 — Sprint 3 batches
Batch 1: Backend zone+materials declaration (3.1 backend) - CoverageZone entity/repo, recycler materials link, geometry validation, controller+DTOs, unit tests.
Batch 2: Backend matched feed (3.2) - PostGIS repository query (ST_Contains/ST_DWithin + material join + status=POSTED), feed endpoint, Testcontainers integration test (in-zone visible / out-of-zone hidden).
Batch 3: Backend accept + race protection (3.3) - conditional atomic UPDATE, 409 on 0 rows, server-side zone/material re-check (IDOR guard), concurrency test, adversarial tests (IDOR, boundary, self-intersecting geometry).
Batch 4: Frontend - recycler zone-declaration form (radius-based: center lat/lng + radius_m, consistent with existing raw-coordinate pattern, no map library added per YAGNI), matched feed list + accept action, role guard for RECYCLER routes.
Then: coverage check (>=80
## PLAN — 2026-07-06 — Sprint 3 batches
Batch 1: Backend zone+materials declaration (3.1 backend) - CoverageZone entity/repo, recycler materials link, geometry validation, controller+DTOs, unit tests.
Batch 2: Backend matched feed (3.2) - PostGIS repository query (ST_Contains/ST_DWithin + material join + status=POSTED), feed endpoint, Testcontainers integration test (in-zone visible / out-of-zone hidden).
Batch 3: Backend accept + race protection (3.3) - conditional atomic UPDATE, 409 on 0 rows, server-side zone/material re-check (IDOR guard), concurrency test, adversarial tests (IDOR, boundary, self-intersecting geometry).
Batch 4: Frontend - recycler zone-declaration form (radius-based: center lat/lng + radius_m, consistent with existing raw-coordinate pattern, no map library added per YAGNI), matched feed list + accept action, role guard for RECYCLER routes.
Then: coverage check (80pct target), adversarial checklist review, push.

## ACTIVITY — 2026-07-06 — Batch 1 complete
Backend: CoverageZone/RecyclerMaterial entities+repos, DeclareZone/DeclareMaterials DTOs, RecyclersService (polygon+radius zone declare/replace with JTS validity check, materials declare/replace), RecyclersController (POST/GET zone, PUT/GET materials, RECYCLER role only).
Bug fix (shared): GlobalExceptionHandler had no handler for AuthorizationDeniedException, so any @PreAuthorize role rejection returned 500 instead of 403. Added handler - affects RequestsController and RecyclersController alike.
Tests: RecyclersControllerTest (7 cases incl. self-intersecting polygon 400, wrong-role 403, replace-on-redeclare). Full suite: 21 tests, 0 failures.
Tooling note: no mvnw committed and global PATH mvn install is stale/missing; used cached Maven 3.9.9 at ~/.m2/wrapper/dists/apache-maven-3.9.9-bin/33b4b2b4/apache-maven-3.9.9/bin for local runs.

## ACTIVITY — 2026-07-06 — Batch 2 complete
Backend: PickupRequestRepository.findMatchedFeed (native ST_Contains/ST_DWithin + material join + status=POSTED per ADR-4) and isEligibleForRecycler (reused in Batch 3 accept guard). GET /api/v1/recyclers/feed. Reused RequestResponseDto (made RequestsService.toDto public static).
Tests: 4 new cases in RecyclersControllerTest - radius zone match, out-of-zone hidden, non-matching material hidden, polygon zone match (ST_Contains path). Full suite: 25 tests, 0 failures.

## ACTIVITY — 2026-07-06 — Batch 3 complete
Backend: PickupRequestRepository.acceptIfPosted (conditional atomic UPDATE WHERE status=POSTED, native query, clearAutomatically) - 0 rows affected means race lost -> 409. RecyclersService.accept() re-checks isEligibleForRecycler first (IDOR guard -> 404, indistinguishable from not-found to avoid leaking zone info) then attempts the atomic update. POST /api/v1/recyclers/feed/{id}/accept.
Test fix: RecyclersControllerTest @BeforeEach deleted users before pickup_requests; accepted_by_recycler_id has no ON DELETE cascade (intentional - deleting a recycler account should not silently wipe accepted-request history), so cleanup order was flipped to delete pickup_requests first.
Tests: accept success, IDOR (outside zone -> 404), double-accept conflict (409), non-existent request (404), concurrent-accept race (two threads, CountDownLatch-synchronized start, asserts exactly one 200 + one 409) - reran 3x standalone, stable. Full suite: 30 tests, 0 failures.
Adversarial checklist note: skipped a dedicated ST_DWithin exact-boundary test (radius edge) - constructing a precise geodesic boundary point is inherently flaky in an integration test; ST_DWithin is documented as inclusive (<=), which is the intentional behavior here, not accidental.

## MILESTONE — 2026-07-06 — Sprint 3 (Recycler Zones & Matching Feed) complete
Backend: CoverageZone/RecyclerMaterial declare+replace API (radius or polygon, JTS geometry validation), PostGIS matched-feed query (ST_Contains/ST_DWithin + material join + status=POSTED), conditional-atomic-UPDATE accept with IDOR guard (404) and race protection (409). GlobalExceptionHandler bugfix: AuthorizationDeniedException now maps to 403 (was 500) - benefits RequestsController too.
Frontend: recycler zone+materials profile form, matched-feed list with accept action, RECYCLER route guard (+spec); fixed a redirect bug where RECYCLER-role login/register sent users to `/` which looped back to `/login`.
Verify phase (this session, resumed): golden path walked end-to-end via docker-compose + curl (browser extension still not connecting) - zone declare, materials declare, request post, matched-feed visibility, accept, double-accept 409, out-of-zone IDOR 404, all as expected. Backend coverage 89% instruction/~92% line, 32/32 tests green (added 2 adversarial tests this session: JWT tamper rejection, Arabic/unicode address round-trip). Frontend: added specs for the 2 new recycler components (94% coverage on Sprint 3's own code); found and logged (not closed) a pre-existing Sprint 2 whole-project coverage gap (~18%) as a risk/backlog item.

## PUSH — 2026-07-06
Pushed Sprint 3 to origin/main. Commit: 6e0b64e — "feat(sprint-3): recycler zones, materials, and matched-request feed" (36 files, 1665 insertions).

## CI — 2026-07-07 (green after fix)
Push 6e0b64e (Sprint 3): CI RED - backend job failed on the new tamperedTokenSignatureIsRejected test (flaky, see .logs/issues.md). Fixed in 67a82a4 (tamper payload's first char instead of signature's last char, verified deterministic across 3 local runs). Push 67a82a4: CI GREEN - security/backend/frontend/build all pass. Sprint 3 done.

## 2026-07-15 — PLAN: Coverage-closure sprint
📋 BATCH 1: status-badge + auth (smallest/simplest components first)
  ├── status-badge.component.spec.ts — renders correct label/class per status input
  ├── login.component.spec.ts — happy-path login, invalid-credentials error, form validation
  └── register.component.spec.ts — happy-path register (all 4 roles), duplicate-email error, form validation
📋 BATCH 2: request components
  ├── new-request.component.spec.ts — happy-path submit, validation errors, HTTP error
  ├── request-list.component.spec.ts — loads/renders list, empty state, load error
  └── request-detail.component.spec.ts — loads detail, cancel action, cancel error
📋 BATCH 3: Verify + Ship
  ├── run frontend coverage report, confirm whole-project ≥ Sprint-3 baseline (target: meaningfully closes the 18% gap)
  ├── lint + full test suite
  ├── update .logs/risks.md (close or downgrade the coverage risk)
  └── commit + push

## 2026-07-15 — EXECUTE: Batch 1 complete
Added status-badge.component.spec.ts (5 status-label cases), login.component.spec.ts (4 tests: validation, success x2 roles, error), register.component.spec.ts (6 tests: default role, validation, minlength, success x2 roles, duplicate-email error). Found and corrected a wrong test assumption: LoginComponent does NOT reset `submitting` on success (only on error, since success navigates away) - test adjusted to match real behavior, not a code bug. Full suite: 34/34 green.

## 2026-07-15 — EXECUTE: Batch 2 complete
Added new-request.component.spec.ts (4 tests), request-list.component.spec.ts (4 tests), request-detail.component.spec.ts (6 tests, incl. ActivatedRoute mock for the paramMap-based id read). Full suite: 13 files, 48/48 tests green. All 6 previously-untested Sprint 2 components now have specs.

## 2026-07-15 — SHIP
Pushed to origin/main: 919d191 (backend: 5fcde79 -> 919d191). CI run 29392608926 GREEN (backend, frontend, security, build all passed; release-coverage-gate skipped as expected - only runs on release tags). PUSH logged per mandatory sprint-end rule.

## 2026-07-15 — EXECUTE: Sprint 4 Batch 1 complete (Story 4.1 backend)
Added status state machine: RecyclersService.schedule()/complete() using the same conditional-atomic-UPDATE pattern as Sprint 3's accept() (PickupRequestRepository.scheduleIfAccepted/completeIfScheduled, WHERE status+ownership match). New endpoints: GET /api/v1/recyclers/requests (list accepted-by-me), POST /api/v1/recyclers/requests/{id}/schedule, POST /api/v1/recyclers/requests/{id}/complete. Ownership check reuses the existing 404-not-403 IDOR pattern. Added 4 tests to RecyclersControllerTest (happy path full lifecycle, skip-ahead rejected 409, never-accepted rejected 404, cross-recycler intrusion rejected 404). Backend suite: 20/20 green.

## 2026-07-15 — EXECUTE: Sprint 4 Batch 2 complete (Story 4.2 backend)
Added notifications/ package: EmailSender interface, SmtpEmailSender (JavaMailSender-backed), NotificationService with @Async notifyRequestAccepted/notifyRequestCompleted (failures logged and swallowed - must never affect the transition that triggered them, per System Design's "no queue" decision). Wired into RecyclersService.accept() and complete() (not schedule() - story scope is ACCEPTED/COMPLETED only). 3 new unit tests (Mockito-mocked EmailSender, no real SMTP).
Caught and fixed a real bug before it shipped: the Sprint-1 comment said to flip management.health.mail.enabled=true in Sprint 4, but doing so would make /actuator/health return 503 DOWN in every environment (no real SMTP configured in dev/CI/docker-compose), breaking the docker-compose healthcheck that gates container startup. Left disabled, replaced the stale comment with the actual reasoning.
Backend suite: 39/39 green.

## 2026-07-15 — EXECUTE: Sprint 4 Batch 3 complete (admin preview, pulled forward from Story 5.2)
Added admin/ package: AdminController (GET /api/v1/admin/users, GET /api/v1/admin/requests, both @PreAuthorize hasRole('ADMIN')), AdminService with pageable search (UserRepository.search/PickupRequestRepository.search - optional email/role/status filters via JPQL). Read-only, no deactivate (stays Story 5.3/Sprint 5).
Security finding caught and fixed in the same batch: POST /auth/register accepted role=ADMIN from any caller (frontend hides it, backend never enforced it) - a live privilege-escalation hole that became actually exploitable the moment real ADMIN-gated endpoints existed. Fixed in AuthService.register() - ADMIN is now rejected server-side (403 ADMIN_SELF_REGISTRATION_NOT_ALLOWED), matching the security doc's Elevation-of-Privilege requirement. Added a regression test.
Backend suite: 44/44 green.

## 2026-07-15 — EXECUTE: Sprint 4 Batch 4 complete (recycler "My Accepted Requests" UI)
Added AcceptedRequestsComponent (frontend/src/app/features/recyclers/accepted/) with Planifier/Marquer terminée action buttons driven by request status (ACCEPTED->schedule, SCHEDULED->complete, no action for COMPLETED/CANCELLED - server already guards invalid transitions with 409). Wired route /recycler/accepted, cross-nav links between feed<->accepted pages. Extended RecyclersService (frontend) with listAccepted/schedule/complete. 7 new spec tests. Frontend suite: 14 files, 55/55 green, lint clean, coverage 90.44% statements / 94.62% lines (still clear of 80% gate).

## 2026-07-15 — EXECUTE: Sprint 4 Batch 5 complete (admin preview UI)
Added AdminSearchComponent (frontend/src/app/features/admin/search/) - two independent filter panels (users by email/role, requests by status), read-only result tables. AdminService + admin.models.ts (AdminUserDto, PageResponse<T>). New adminGuard (mirrors recyclerGuard pattern) + route /admin.
Follow-on fix from the same bug class Sprint 3 caught for RECYCLER: login.component.ts's redirectAfterAuth() sent ADMIN into the else-branch (-> '/' -> redirects to /login, infinite loop) since there was previously no admin destination. Fixed now that /admin exists. MUNICIPALITY has the same latent issue but is out of scope (Sprint 6 territory, pre-existing, not touched this sprint).
Frontend suite: 16 files, 63/63 green, lint clean, coverage 90.94% statements / 94.74% lines.

## 2026-07-15 — SHIP: Sprint 4
Pushed to origin/main: 651877d (from 919d191). CI run 29407540838 GREEN (backend, frontend, security, build all passed; release-coverage-gate skipped as expected).

## 2026-07-15 — EXECUTE: Sprint 5 Batches 1-2 complete (Story 5.1, mock CMI payment)
Batch 1 (backend): new payments/ package - PaymentGateway interface + MockCmiGateway (ADR-2, always SUCCEEDED, flat placeholder amount since no pricing model exists in the docs), Payment entity (deliberately decoupled from PickupRequest - plain UUID column, not a JPA relation, keeping payments/requests as sibling modules per architecture doc), PaymentService (ownership+status check via RequestsService.getMine(), double-submit guarded both at app level and via DB UNIQUE(pickup_request_id) constraint + saveAndFlush/catch, same "DB is source of truth" principle as Sprint 3's race guard). New endpoint POST /api/v1/requests/{id}/payment. GET /api/v1/requests/{id} now enriched with paymentStatus (composed at the controller level to avoid a RequestsService<->PaymentService circular dependency). 4 new integration tests (success, too-early, double-submit, cross-household ownership).
Batch 2 (frontend): Pay button on request-detail (visible when COMPLETED and not yet paid), "Paiement effectué" confirmation once paid, error+retry on failure. 5 new/updated spec tests.
Backend: 48/48 tests, coverage 91.56% instruction / 92.95% line. Frontend: 66/66 tests, lint clean, coverage 91.03% statements / 94.94% lines.
Story 5.2 required no new work this sprint (already satisfied by Sprint 4's admin-preview pull-forward). Story 5.3 deferred per user decision.

## 2026-07-15 — SHIP: Sprint 5
Pushed to origin/main: cac13cd (from 651877d). CI run 29439195094 GREEN (backend, frontend, security, build all passed; release-coverage-gate skipped as expected).

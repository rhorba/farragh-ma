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

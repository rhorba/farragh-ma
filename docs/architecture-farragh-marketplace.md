# Architecture: Farragh.ma
**PRD Reference**: docs/prd-farragh-marketplace.md
**System Design Reference**: docs/system-design-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Tech Lead / Software Architect

## 1. Overview
A layered, package-by-feature modular monolith in Spring Boot 4.1 (Java 25), backing an Angular 21 SPA. Each business capability (auth, requests, recyclers, municipality, payments, notifications, admin) is a self-contained package with its own controller/service/repository/entity, communicating with other modules only through public service interfaces — never direct cross-module repository access.

## 2. Architecture Decision Records

### ADR-1: Layered architecture, package-by-feature (not Clean/Hexagonal)
**Status**: Accepted
**Context**: CRUD-heavy marketplace domain with moderate business rules (zone matching, status lifecycle, payment reconciliation) — not complex enough to need full hexagonal port/adapter isolation.
**Decision**: Standard Spring layered architecture (Controller → Service → Repository → Entity), packages organized by feature, not by layer.
**Consequences**:
  + Fast to build, easy to navigate, low ceremony
  - Payments module isolation relies on discipline (interface + adapter), not enforced by a full hexagonal boundary
**Re-evaluate when**: A module's business logic grows complex enough that framework coupling becomes a testing/change burden.

### ADR-2: Payments isolated behind a `PaymentGateway` interface
**Status**: Accepted
**Context**: CMI runs in mock mode now; real credentials come later. Must not require rewriting call sites when switching.
**Decision**: Define `PaymentGateway` interface in the payments module; `MockCmiGateway` implements it now, `CmiGateway` (real) implements it later. Selected via Spring profile/`CMI_MODE` env var.
**Consequences**:
  + Zero call-site changes when going live
  - Mock behavior must faithfully mirror real gateway's success/failure/callback shape to avoid surprises at cutover
**Re-evaluate when**: Real CMI credentials are available — swap implementation, keep interface.

### ADR-3: JWT stateless auth with role claims
**Status**: Accepted
**Context**: 4 distinct roles (Household/SME, Recycler, Municipality, Admin) need clear, enforceable authorization.
**Decision**: Spring Security + JWT (role claim in token), method-level `@PreAuthorize` per role, resource-level ownership checks in service layer (e.g., a Recycler can only update requests they accepted).
**Consequences**:
  + Stateless, scales horizontally without session store
  - Token revocation requires short expiry + refresh flow (no server-side session to invalidate)
**Re-evaluate when**: Need for immediate token revocation (e.g., compromised account) becomes a hard requirement — would need a token blocklist.

### ADR-4: PostGIS zone matching via repository query, not in-app spatial logic
**Status**: Accepted
**Context**: Matching requests to recyclers by declared coverage zone (FR-3/FR-4).
**Decision**: Store recycler coverage as PostGIS `geography(Polygon)` or radius (`geography(Point)` + `radius_m`); matching done via `ST_Contains`/`ST_DWithin` in the repository query (Spring Data JPA + Hibernate Spatial), not by pulling all rows into app memory.
**Consequences**:
  + Leverages GiST index, correct at any realistic pilot data volume
  - Requires Hibernate Spatial + PostGIS-aware migrations (Flyway with PostGIS extension enabled)
**Re-evaluate when**: Query patterns need geo features PostGIS doesn't support well (unlikely at this scope).

## 3. System Structure
```
[Angular 21 SPA] → HTTPS/JSON → [Spring Boot 4 API]
                                      │
        ┌────────────┬───────────────┼───────────────┬──────────────┐
        │            │               │               │              │
   [auth/]      [requests/]     [recyclers/]   [municipality/]  [admin/]
        │            │               │               │              │
        └─────┬──────┴───────┬───────┴───────┬───────┘              │
              │               │               │                      │
        [payments/]    [notifications/]  [shared/ (config, security, exceptions)]
              │               │
        [PaymentGateway]  [EmailSender]
        (Mock/Real CMI)   (SMTP)
                    │
              [PostgreSQL 16 + PostGIS]
```

## 4. Data Model (high level — full schema owned by DBA)
```
User ──1:1──> Role (HOUSEHOLD_SME | RECYCLER | MUNICIPALITY | ADMIN)
User(HOUSEHOLD_SME) ──1:N──> PickupRequest
PickupRequest ──N:1──> MaterialType
PickupRequest ──0:1──> Recycler (accepted_by)
PickupRequest ──0:1──> Payment
Recycler ──1:N──> CoverageZone (geography)
Recycler ──N:N──> MaterialType (via recycler_materials)
Municipality ──1:N──> BulkSubscription ──1:1──> CoverageZone
Admin ── (no owned entities — cross-cutting read/manage access)
```

## 5. API Design (representative — full list in stories)
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Register (role-specific payload) | Public |
| POST | /api/v1/auth/login | Login, issue JWT | Public |
| POST | /api/v1/requests | Household/SME creates pickup request | HOUSEHOLD_SME |
| GET | /api/v1/requests/feed | Recycler's matched request feed (zone+material) | RECYCLER |
| POST | /api/v1/requests/{id}/accept | Recycler accepts request | RECYCLER (owns zone match) |
| PATCH | /api/v1/requests/{id}/status | Update lifecycle status | RECYCLER (owner) / ADMIN |
| POST | /api/v1/requests/{id}/payment | Trigger mock CMI payment | HOUSEHOLD_SME (owner) |
| POST | /api/v1/recyclers/zones | Recycler declares coverage zone | RECYCLER |
| POST | /api/v1/municipality/subscriptions | Bulk-subscribe a zone | MUNICIPALITY |
| GET | /api/v1/admin/users | List/search users | ADMIN |
| GET | /api/v1/admin/requests | List/search all requests | ADMIN |

## 6. Security Considerations (summary — full detail in Security Baseline doc)
- Authentication: JWT, role claim, short expiry + refresh token
- Authorization: RBAC + resource ownership checks in service layer
- Data protection: HTTPS enforced, PII (address/phone/email) — encryption-at-rest evaluated in Security doc
- Key risks: cross-tenant data leakage (recycler seeing requests outside their zone — must be enforced server-side, never trust client-filtered results)

## 7. Infrastructure
- Hosting: Docker Compose on a single host (per System Design SDR-2)
- Database: PostgreSQL 16 + PostGIS, Docker volume for persistence, daily backup cron
- CI/CD: GitHub Actions (lint → test+coverage gate → security scan → build → deploy)
- Monitoring: Spring Boot Actuator + Micrometer, container stdout logs

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| Hibernate Spatial + PostGIS setup friction | Spike early in Sprint 1, validate one zone-match query end-to-end before building on top | Backend Dev |
| Mock CMI diverging from real CMI shape at cutover | Keep `PaymentGateway` interface narrow, document expected real callback contract now | Backend Dev / Security |
| RTL (Arabic) layout retrofit cost | UI doc must define RTL approach before component build starts | UI Designer / Frontend Dev |

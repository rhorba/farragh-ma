# PRD: Farragh.ma — Recycling Pickup Marketplace
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: PM | **Status**: Draft

## 1. Problem Statement
Morocco's Green Law 28-00 requires certified waste treatment, but households and SMEs have no way to find which certified recycler accepts what materials, and certified recyclers have no digital lead-generation channel. Municipalities have no tool to organize neighborhood-level recycling coverage.

## 2. Goals & Success Metrics
| Goal | Metric | Target |
|---|---|---|
| Households/SMEs can get materials picked up | Requests posted → accepted within 48h | ≥ 70% |
| Recyclers get a working lead channel | Active recyclers accepting ≥ 1 request/week | ≥ 50% of onboarded recyclers |
| Municipalities can organize coverage | Municipality accounts with ≥ 1 active zone | ≥ 1 pilot municipality by launch |
| Platform is trustworthy | Payment success rate (mock mode validates flow) | 100% of mock transactions reconcile |

## 3. User Stories
As a **Household/SME**, I want to post a pickup request by material type and location, so that a certified recycler can collect it.
As a **Household/SME**, I want to pay for a pickup (where applicable) through a trusted gateway, so that I don't have to handle cash.
As a **Certified Recycler**, I want to see pickup requests within my declared coverage zone and material specialties, so that I only see relevant leads.
As a **Certified Recycler**, I want to accept a request and update its status, so that the requester knows what's happening.
As a **Municipality**, I want to bulk-subscribe a neighborhood for recurring coverage, so that residents in that zone get service without individual sign-up.
As an **Admin**, I want to see all users, requests, and transactions, so that I can support users and monitor platform health.
As any **user**, I want the app in French or Arabic, so that I can use it in my preferred language.

- [ ] Story: Household/SME registers, posts a pickup request (material, quantity, address/geo-point, photo optional)
- [ ] Story: Recycler registers with certification info, declares coverage zone(s) and accepted material types
- [ ] Story: Recycler sees a feed of requests matching their zone + materials, accepts one
- [ ] Story: Request lifecycle: Posted → Accepted → Scheduled → Completed → (Cancelled)
- [ ] Story: Payment (CMI, mock mode) tied to a completed pickup
- [ ] Story: Municipality bulk-subscribes a zone; residents in that zone get default recycler coverage
- [ ] Story: Admin views/manages users, requests, transactions
- [ ] Story: Email notification on key status changes (request accepted, completed)
- [ ] Story: FR/AR language switch

## 4. Scope
### In Scope
- 4 roles: Household/SME, Certified Recycler, Municipality, Admin
- Pickup request lifecycle with geo (PostGIS) zone matching
- CMI payment integration in **mock mode** (no live merchant credentials yet)
- Municipality bulk-subscribe / neighborhood coverage
- Basic admin panel (view/manage users, requests, transactions — no deep analytics)
- Email notifications on status changes
- FR + AR localization (Angular i18n)
- Docker Compose deployment (single-host); Kubernetes only introduced if a real scaling need appears

### Out of Scope (this version)
- Native mobile apps (web-responsive only)
- Real CMI production credentials / live payment processing
- Admin analytics dashboards beyond basic lists
- Multi-region / multi-tenant infrastructure
- SMS notifications (email only for now)

## 5. Requirements
### Functional
- FR-1: Users register/authenticate per role (Household/SME, Recycler, Municipality, Admin)
- FR-2: Household/SME can create, view, and cancel pickup requests
- FR-3: Recycler can declare coverage zones (PostGIS polygons or radius) and accepted materials
- FR-4: System matches requests to eligible recyclers by zone + material type
- FR-5: Recycler can accept a request and progress its status
- FR-6: Payment step runs against a CMI mock adapter, produces a reconciled mock transaction
- FR-7: Municipality can bulk-subscribe a geographic zone for default coverage
- FR-8: Admin can view/search users, requests, transactions and deactivate accounts
- FR-9: Email sent on request accepted / completed
- FR-10: UI available in French and Arabic (including RTL layout for Arabic)

### Non-Functional
- NFR-1: Performance — API p95 < 500ms under expected pilot load (single municipality scale)
- NFR-2: Security — JWT auth, RBAC per role, HTTPS enforced, no secrets in code/logs
- NFR-3: Accessibility — WCAG AA on primary flows
- NFR-4: Coverage — ≥ 80% combined unit + integration test coverage before any ship

## 6. Constraints & Assumptions
- Stack fixed: Java 25 (LTS) + Spring Boot 3.x + Maven backend; Angular 21 (LTS) frontend; PostgreSQL 16 + PostGIS
- CMI payment runs in mock mode until real merchant credentials are available (assumption: mock is acceptable for this version's "done")
- Deployment: Docker only; Kubernetes deferred (YAGNI) until a concrete scaling trigger
- No video recording of E2E flows until the final sprint of this version (explicit user instruction, overrides default per-version-completion rule)
- Assumption: single-region deployment is sufficient for pilot scale

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| PostGIS zone-matching complexity underestimated | M | M | Spike in System Design/Architecture phase before committing story estimates |
| CMI mock-to-real switch surprises later | M | M | Adapter pattern isolates CMI integration behind an interface from day one |
| FR/AR RTL support adds frontend rework if bolted on late | M | M | UX/UI docs must address RTL from the start, not retrofitted |
| Full doc-chain-before-code slows first release | H | L | Accepted tradeoff — user explicitly chose full doc-first compliance |

## 8. Timeline
| Milestone | Target |
|---|---|
| PRD Approved | This session |
| Full doc chain (System Design → Stories) Approved | This session |
| Docs committed & pushed | End of this session |
| Sprint 1 Execution Start | Next session |
| Sprint count / MVP date | Determined in Stories doc (Scrum Master), based on backlog size |

# System Design: Farragh.ma
**PRD Reference**: docs/prd-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | 99% (single instance + restart) | Pilot scale, single municipality — not worth active-passive yet |
| Latency (p99) | < 500ms API | Matches PRD NFR-1 |
| Throughput | ~20-50 RPS peak | Pilot: one region, hundreds of households/SMEs, tens of recyclers |
| Data Volume | < 1 GB/day | Text + metadata + optional photos (assume object storage later if photos grow) |
| Retention | Indefinite for transactional data (legal/audit); logs 90 days | |
| Recovery (RTO) | 60 min | Restore from latest DB backup + redeploy container |
| Recovery (RPO) | 24 hr | Daily automated Postgres backup (pilot scale — tighten later if needed) |

## 2. Component Topology
```
[Web Browser — Angular 21 SPA]
        ↓ HTTPS
[Reverse proxy / Nginx — TLS termination, static Angular build]
        ↓
[Spring Boot 3 API (modular monolith, single container)]
  ├── Auth module (JWT, RBAC)
  ├── Requests module (pickup lifecycle)
  ├── Recyclers module (zones, materials)
  ├── Municipality module (bulk-subscribe)
  ├── Payments module → CMI adapter (mock now, real later)
  ├── Notifications module → SMTP (email)
  └── Admin module
        ↓
[PostgreSQL 16 + PostGIS] — single instance, Docker volume
        ↓
[External: CMI gateway (mocked), SMTP provider]
        ↓
[Observability: container logs → stdout, aggregated by Docker logging driver]
```

**YAGNI call**: one Spring Boot service, not microservices — team size and load don't justify service boundaries yet. No message queue: email notifications sent via `@Async` in-process; revisit only if email volume or reliability becomes a measured problem. No Redis/cache: Postgres handles pilot read volume directly; add if a specific query proves slow under real load.

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| CMI Payment | REST (mocked adapter, same interface as real gateway) | Isolates mock-to-real swap to one adapter class |
| SMTP Email | Async in-process call (`@Async`) | Volume too low to justify a queue |
| Angular ↔ API | REST/JSON over HTTPS | Standard, no need for GraphQL/gRPC at this scale |

## 4. Scalability Strategy
- Scaling approach: vertical (bigger container) until a measured bottleneck appears
- Cache strategy: none initially
- Queue strategy: none initially — `@Async` covers notification fan-out at this volume

## 5. System Design Decision Records

### SDR-1: Monolith vs microservices
**NFR Driver**: Throughput (~20-50 RPS), small team
**Options**:
  🟢 Simple: modular monolith (single Spring Boot app, clear module packages)
  🟡 Balanced: monolith + separate payments service
  🔴 Custom: full microservices (auth, requests, payments, notifications as separate services)
**Decision**: 🟢 Modular monolith. Package-level module boundaries (auth/, requests/, recyclers/, municipality/, payments/, notifications/, admin/) keep a future extraction possible without paying microservices operational cost now.
**Trade-offs**: Single deploy unit — one bug can affect the whole app; acceptable at pilot scale.
**Re-evaluate when**: Any one module's load or team ownership outgrows the shared deploy (e.g., payments needs independent scaling/compliance isolation).

### SDR-2: Deployment orchestration
**NFR Driver**: Availability 99%, team/ops capacity
**Options**:
  🟢 Simple: Docker Compose (API + DB + Nginx containers on one host)
  🟡 Balanced: Docker Swarm
  🔴 Custom: Kubernetes
**Decision**: 🟢 Docker Compose, per explicit user instruction (Docker only; K8s only if needed).
**Trade-offs**: No auto-healing/auto-scaling across hosts; acceptable for pilot single-municipality launch.
**Re-evaluate when**: Multi-region requirement, need for zero-downtime rolling deploys at scale, or horizontal scaling across multiple hosts becomes necessary.

### SDR-3: Geo zone matching
**NFR Driver**: FR-3/FR-4 (recycler coverage zones, request matching)
**Options**:
  🟢 Simple: PostGIS `ST_Contains`/`ST_DWithin` queries against recycler zone polygons/radius
  🔴 Custom: External geo-indexing service (Elasticsearch geo, custom spatial index)
**Decision**: 🟢 PostGIS — already the chosen DB, native spatial indexing (GiST) is sufficient at pilot data volume.
**Re-evaluate when**: Zone-matching query latency measured > NFR target under real data volume.

## 6. Observability Baseline (minimum viable)
- Logs: structured JSON to stdout, captured by Docker logging driver, no external aggregator yet (add Loki/Datadog only if the team needs cross-container search)
- Metrics: Spring Boot Actuator + Micrometer exposed for basic health/latency; no dashboard tool mandated yet
- Alerts: none automated at pilot scale — manual monitoring is acceptable until real users are on the platform

# Database Design: Farragh.ma
**Architecture Reference**: docs/architecture-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: DBA

## 1. Database Selection
- **Engine**: PostgreSQL 16 + PostGIS extension
- **Rationale**: Relational data with real transactional needs (request lifecycle, payments) plus native geo support (recycler coverage zones) — PostGIS is the standard fit, already locked in via PRD/System Design.
- **Hosting**: Self-hosted in Docker (per System Design SDR-2), single instance, Docker volume for data + daily `pg_dump` backup

## 2. Entity-Relationship Model
```
users ──1:1──> roles (enum-like: HOUSEHOLD_SME, RECYCLER, MUNICIPALITY, ADMIN)
users(HOUSEHOLD_SME) ──1:N──> pickup_requests
pickup_requests ──N:1──> material_types
pickup_requests ──0:1──> users(RECYCLER) (accepted_by_recycler_id)
pickup_requests ──0:1──> payments
users(RECYCLER) ──1:N──> coverage_zones
users(RECYCLER) ──N:N──> material_types (via recycler_materials)
users(MUNICIPALITY) ──1:N──> bulk_subscriptions ──1:1──> coverage_zones
```

## 3. Schema Design
```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

CREATE TABLE users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email           VARCHAR(255) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  role            VARCHAR(20) NOT NULL CHECK (role IN ('HOUSEHOLD_SME','RECYCLER','MUNICIPALITY','ADMIN')),
  full_name       VARCHAR(255) NOT NULL,
  phone           VARCHAR(30),
  preferred_lang  VARCHAR(2) NOT NULL DEFAULT 'fr' CHECK (preferred_lang IN ('fr','ar')),
  is_active       BOOLEAN NOT NULL DEFAULT true,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE material_types (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code        VARCHAR(50) NOT NULL UNIQUE,   -- e.g. PLASTIC, METAL, ELECTRONIC, ORGANIC
  label_fr    VARCHAR(100) NOT NULL,
  label_ar    VARCHAR(100) NOT NULL
);

CREATE TABLE coverage_zones (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, -- recycler or municipality
  area         GEOGRAPHY(POLYGON, 4326),   -- nullable if radius-based
  center_point GEOGRAPHY(POINT, 4326),     -- nullable if polygon-based
  radius_m     INTEGER,                    -- used only with center_point
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (area IS NOT NULL OR (center_point IS NOT NULL AND radius_m IS NOT NULL))
);

CREATE TABLE recycler_materials (
  recycler_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  material_type_id UUID NOT NULL REFERENCES material_types(id) ON DELETE CASCADE,
  PRIMARY KEY (recycler_id, material_type_id)
);

CREATE TABLE pickup_requests (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  material_type_id      UUID NOT NULL REFERENCES material_types(id),
  quantity_desc         VARCHAR(255),
  address_text          VARCHAR(500) NOT NULL,
  location              GEOGRAPHY(POINT, 4326) NOT NULL,
  status                VARCHAR(20) NOT NULL DEFAULT 'POSTED'
                          CHECK (status IN ('POSTED','ACCEPTED','SCHEDULED','COMPLETED','CANCELLED')),
  accepted_by_recycler_id UUID REFERENCES users(id),
  photo_url             VARCHAR(500),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pickup_request_id UUID NOT NULL UNIQUE REFERENCES pickup_requests(id) ON DELETE CASCADE,
  amount_cents      INTEGER NOT NULL,
  currency          VARCHAR(3) NOT NULL DEFAULT 'MAD',
  provider          VARCHAR(20) NOT NULL DEFAULT 'CMI',
  mode              VARCHAR(10) NOT NULL DEFAULT 'MOCK' CHECK (mode IN ('MOCK','LIVE')),
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCEEDED','FAILED')),
  provider_ref      VARCHAR(255),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bulk_subscriptions (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  municipality_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  coverage_zone_id  UUID NOT NULL REFERENCES coverage_zones(id) ON DELETE CASCADE,
  is_active         BOOLEAN NOT NULL DEFAULT true,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| users | idx_users_email | email | login lookup (also enforced by UNIQUE) |
| users | idx_users_role | role | admin filtering by role |
| pickup_requests | idx_requests_requester | requester_id | household/SME "my requests" |
| pickup_requests | idx_requests_status | status | feed filtering (WHERE status = 'POSTED') |
| pickup_requests | idx_requests_location | location (GiST) | recycler zone-match (`ST_DWithin`/`ST_Contains`) |
| pickup_requests | idx_requests_recycler | accepted_by_recycler_id | recycler "my accepted requests" |
| coverage_zones | idx_zones_owner | owner_id | recycler/municipality zone lookup |
| coverage_zones | idx_zones_area (GiST) | area | polygon containment queries |
| coverage_zones | idx_zones_center (GiST) | center_point | radius containment queries |
| recycler_materials | idx_recycler_materials_material | material_type_id | "which recyclers accept X material" |
| payments | idx_payments_request | pickup_request_id | already UNIQUE, covers lookup |

## 5. Migration Plan (Flyway, matches Spring Boot convention)
| Migration File | Description | Reversible |
|---|---|---|
| V1__enable_extensions.sql | postgis, pgcrypto | Yes (drop extension) |
| V2__create_users.sql | users table + role check | Yes |
| V3__create_material_types.sql | material_types + seed FR/AR labels | Yes |
| V4__create_coverage_zones.sql | coverage_zones (PostGIS columns) | Yes |
| V5__create_recycler_materials.sql | join table | Yes |
| V6__create_pickup_requests.sql | pickup_requests + indexes | Yes |
| V7__create_payments.sql | payments table | Yes |
| V8__create_bulk_subscriptions.sql | bulk_subscriptions | Yes |

Flyway runs forward-only in production; each `V*` file paired with a documented manual rollback script kept in `backend/src/main/resources/db/rollback/` (not auto-applied — DBA/Tech Lead runs manually if ever needed).

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| Recycler's matched feed | `WHERE status='POSTED' AND ST_DWithin(location, zone, radius)` (or `ST_Contains`) AND material_type_id IN (recycler's materials) | idx_requests_status, idx_requests_location (GiST), idx_recycler_materials_material |
| Household's own requests | `WHERE requester_id = ?` | idx_requests_requester |
| Admin search | filtered list, paginated | idx_users_role, idx_requests_status |

## 7. Sensitive Data
- Columns requiring extra care: `users.phone`, `users.full_name`, `pickup_requests.address_text`, `pickup_requests.location` — protected via disk-level encryption (Security doc), not column-level (YAGNI at pilot scale)
- Row-level security: not enabled at DB level — authorization enforced in the service layer per Security doc (RLS would be redundant given single-app access pattern; revisit only if multiple services query the DB directly)

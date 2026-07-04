# DevOps Foundation: Farragh.ma
**Architecture**: docs/architecture-farragh-marketplace.md
**Security**: docs/security-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development (Docker Compose, hot-reload) | Manual (`docker compose up`) |
| staging | QA / preview, same Compose stack on a staging host | Auto on merge to `main` |
| production | Live pilot (single municipality) | Manual tag / approved release, `docker compose up -d` |

## 2. CI Pipeline (GitHub Actions)
```yaml
name: ci
on: [push, pull_request]
permissions:
  contents: read
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '25' }
      - name: Build & test with coverage (JaCoCo) — Testcontainers spins up its own Postgres+PostGIS
        working-directory: backend
        run: mvn -B verify

  # 80% coverage gate (CLAUDE.md rule 6) only enforced at release time via the "coverage-gate"
  # Maven profile — see backend/pom.xml. Not run on every routine push (would block early sprints
  # before enough tests exist); enforced from Sprint 7 hardening onward via a version tag push.
  release-coverage-gate:
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '25' }
      - working-directory: backend
        run: mvn -B verify -Pcoverage-gate

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22' }
      - working-directory: frontend
        run: npm ci
      - working-directory: frontend
        run: npm run lint
      - working-directory: frontend
        run: npm test -- --code-coverage --watch=false

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: returntocorp/semgrep-action@v1
        with: { config: p/owasp-top-ten }
      - uses: aquasecurity/trivy-action@master
        with: { scan-type: fs, severity: CRITICAL,HIGH, exit-code: 1 }
      - uses: gitleaks/gitleaks-action@v2

  build:
    needs: [backend, frontend, security]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build backend image
        run: docker build -t farragh-backend:${{ github.sha }} ./backend
      - name: Build frontend image
        run: docker build -t farragh-frontend:${{ github.sha }} ./frontend
```
Per CLAUDE.md rule 11: after every push, watch this pipeline to completion (`gh run watch`). Red CI blocks all further work until fixed and green.

## 3. Infrastructure
- **Hosting**: Single Docker host (VPS or on-prem) — matches System Design SDR-2 (Docker Compose, no K8s yet)
- **Compute**: Containers — `backend` (Spring Boot), `frontend` (Nginx serving Angular build), `db` (Postgres+PostGIS), `reverse-proxy` (Nginx, TLS termination)
- **Database**: Self-hosted Postgres+PostGIS in a Docker volume; daily `pg_dump` cron to off-container storage
- **Secrets**: `.env` file on the host (gitignored), populated from `.env.example`; no external secrets manager yet (YAGNI at single-host scale)
- **Monitoring**: Spring Boot Actuator + Micrometer endpoints; container logs via Docker's `json-file` driver, rotated

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities (OWASP Top 10 ruleset) | Critical findings |
| Trivy | SCA — dependency CVEs (Maven + npm) | Critical CVEs |
| Gitleaks | Secrets detection | Any secrets found |

## 5. Docker Setup

### `docker-compose.yml` (production/staging shape)
```yaml
services:
  backend:
    build: ./backend
    env_file: .env
    depends_on:
      db: { condition: service_healthy }
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s

  frontend:
    build: ./frontend
    depends_on: [backend]

  reverse-proxy:
    image: nginx:1.27-alpine
    ports: ["80:80", "443:443"]
    volumes: ["./nginx.conf:/etc/nginx/nginx.conf:ro", "./certs:/etc/nginx/certs:ro"]
    depends_on: [backend, frontend]

  db:
    image: postgis/postgis:16-3.4
    env_file: .env
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 5s

volumes:
  pgdata:
```

### `backend/Dockerfile` (multi-stage, non-root)
```dockerfile
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### `frontend/Dockerfile` (multi-stage, Nginx serving static build)
```dockerfile
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
COPY --from=builder /app/dist/farragh-frontend /usr/share/nginx/html
EXPOSE 80
```

**Kubernetes**: intentionally not set up. Trigger to revisit: horizontal scaling need across multiple hosts, zero-downtime rolling deploys required, or multi-region expansion — none present at pilot scale (matches System Design SDR-2).

## 6. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Logs | Docker `json-file` driver, manual `docker logs` review at pilot scale | Error rate spike — manual monitoring for now |
| Metrics | Spring Actuator `/actuator/health`, `/actuator/metrics` | Health check failure → container auto-restart (Compose `restart: unless-stopped`) |
| Uptime | Manual/health-check based | No formal SLO yet — informal target 99% (per System Design NFR) |

Formal dashboards (Grafana/Prometheus) explicitly deferred — YAGNI at pilot scale; revisit once there's a real on-call rotation or multiple hosts to correlate.

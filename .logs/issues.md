# ISSUES — Farragh.ma



## 2026-07-04 — Resolved
ISSUE: Local Java 21 / no Maven vs target Java 25 LTS.
RESOLUTION: Docker-only build strategy (see decisions.md).

## 2026-07-04 — CI: Trivy 429 from Maven Central
ISSUE: Trivy fs scan hit "429 Too Many Requests" resolving spring-batch-bom from Maven Central (shared GH Actions IP rate-limited).
FIX: Added a `mvn dependency:go-offline` warm-up step before Trivy in the security job, so Trivy reads resolved deps from the local .m2 cache instead of hitting the network.

## 2026-07-04 — CI: real CVE found (Trivy working correctly)
ISSUE: Trivy (after cache fix) found CVE-2025-14813 (CRITICAL) in org.bouncycastle:bcprov-jdk18on 1.79 (GOSTCTR cipher block-count bug).
FIX: Bumped bouncycastle.version to 1.84 (patched) in backend/pom.xml. Verified full test suite still passes (8/8) after the bump.

## 2026-07-05 — Sprint 2 issues found & fixed
ISSUE: Spring Security's default entry point (Http403ForbiddenEntryPoint) returned 403 for requests with no JWT at all, not 401. Fixed by disabling anonymous authentication and configuring an explicit HttpStatusEntryPoint(401) in SecurityConfig.
ISSUE: Frontend Nginx (bare nginx:1.27-alpine) had no SPA fallback — direct navigation to client-side routes like /login returned 404. Fixed by adding frontend/nginx.conf with `try_files $uri $uri/ /index.html` and copying it into the Dockerfile.
Both found and fixed during live E2E verification (curl through the real docker-compose stack), not just unit/integration tests.

## ISSUE — 2026-07-07 — Flaky adversarial test caught by CI, fixed
tamperedTokenSignatureIsRejected (added 2026-07-06) flipped the last base64url character of a JWT signature to simulate tampering. That character only encodes 4 real bits (32-byte HMAC-SHA256 signature doesn't divide evenly into 6-bit base64 units) with 2 padding bits; whether flipping it changes the decoded bytes depends on the specific original/replacement character pair and the JWT's random per-run secret, so it passed or failed non-deterministically. First CI push after this session's changes went RED because of it (run 28827712668); a coincidental second green run on a log-only commit masked it rather than fixing it.
Fix: tamper the first character of the payload segment instead (a full 6-bit-aligned unit, no padding ambiguity) - deterministically changes decoded bytes and breaks the signature every time. Verified with 3 consecutive local runs, all pass.

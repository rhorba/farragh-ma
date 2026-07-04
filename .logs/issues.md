# ISSUES — Farragh.ma



## 2026-07-04 — Resolved
ISSUE: Local Java 21 / no Maven vs target Java 25 LTS.
RESOLUTION: Docker-only build strategy (see decisions.md).

## 2026-07-04 — CI: Trivy 429 from Maven Central
ISSUE: Trivy fs scan hit "429 Too Many Requests" resolving spring-batch-bom from Maven Central (shared GH Actions IP rate-limited).
FIX: Added a `mvn dependency:go-offline` warm-up step before Trivy in the security job, so Trivy reads resolved deps from the local .m2 cache instead of hitting the network.

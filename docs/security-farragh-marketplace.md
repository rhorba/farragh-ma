# Security Baseline: Farragh.ma
**Architecture Reference**: docs/architecture-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Security Engineer

## 1. Threat Model (5-Minute)
- **What are we building?** A recycling pickup marketplace handling PII (names, addresses, phone/email) and payment flow (mocked for now) across 4 role types.
- **Who would attack it?** Mostly opportunistic/script-based attackers (credential stuffing, scraping user data, IDOR probing); a competitor scraping recycler/pricing data is plausible; low likelihood of nation-state interest at this stage.
- **Worst outcome?** PII leak (addresses tied to households — physical safety concern, not just privacy), a recycler/household viewing or acting on another user's data (IDOR), or payment-status manipulation once CMI goes live.

## 2. STRIDE Analysis (top risks)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | Auth (JWT) | Strong password hashing (argon2id), JWT signed RS256, short access-token expiry | Required in Sprint 1 |
| Tampering | Request status updates | Server-side state machine validation (no client-trusted status jumps, e.g., Posted→Completed skipping Accepted) | Required |
| Repudiation | Payment / status changes | Audit trail: `created_at`/`updated_at`/`changed_by` on requests and payments | Required |
| Info Disclosure | Recycler feed, admin endpoints | Resource-level authorization — recycler only sees requests in their zone; household only sees their own requests | Required — this is the #1 risk (IDOR via zone bypass) |
| DoS | Public register/login endpoints | Rate limiting on auth endpoints (Spring `Bucket4j` or gateway-level) | Should-have, not launch-blocking at pilot scale |
| Elevation of Privilege | Role claims in JWT | Roles set server-side only at registration/admin action, never client-supplied on subsequent requests | Required |

## 3. Authentication Strategy
- **Type**: JWT (access + refresh), per Architecture ADR-3
- **MFA**: Not required for pilot (Household/SME, Recycler); **required for Admin** role given its cross-tenant access (TOTP)
- **Password policy**: argon2id hashing, min 10 chars, reject top-10k breached passwords (e.g., via a static breached-password list check)
- **Session management**: access token ≤ 15 min, refresh token ≤ 7 days, refresh rotation on use

## 4. Authorization Model
- **Pattern**: RBAC + resource-level ownership checks (per Architecture ADR-3)
- **Roles defined**: HOUSEHOLD_SME, RECYCLER, MUNICIPALITY, ADMIN
- **Resource-level checks**: Yes, mandatory —
  - Household/SME can only read/cancel their own requests
  - Recycler can only see/accept requests matching their declared zone + materials (enforced in the query, not filtered client-side)
  - Recycler can only update status on requests they accepted
  - Municipality can only manage their own bulk subscriptions
  - Admin bypasses ownership checks but every admin action is audit-logged

## 5. Data Protection
- **PII fields**: household/SME name, phone, email, pickup address (free text + geo-point)
- **Encryption at rest**: Database-level encryption via managed Postgres disk encryption (or LUKS on the Docker host volume) — full column-level encryption not justified at pilot scale (YAGNI), revisit if compliance requires it
- **Encryption in transit**: HTTPS enforced everywhere, HSTS enabled at the reverse proxy
- **Secrets management**: env vars only (`.env`, gitignored), never committed — matches `.env.example` already in place; no secrets manager needed until multi-environment/team-scale operations

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side (Bean Validation `@Valid` on all DTOs)
- [ ] Output encoded for context — Angular's default HTML sanitization relied on, no raw `innerHTML` binding of user content
- [ ] No secrets in code, logs, or error messages — error responses return generic messages, details logged server-side only
- [ ] HTTPS only, HSTS + standard security headers (CSP, X-Content-Type-Options, X-Frame-Options) via Nginx reverse proxy
- [ ] Dependencies scanned in CI (SCA) — see DevOps doc for Trivy/Gitleaks/Semgrep gates
- [ ] Every new endpoint gets an explicit `@PreAuthorize` — deny-by-default, not allow-by-default

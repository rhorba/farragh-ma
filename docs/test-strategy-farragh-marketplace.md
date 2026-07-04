# Test Strategy: Farragh.ma
**Architecture Reference**: docs/architecture-farragh-marketplace.md
**Security Reference**: docs/security-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: Test Architect
**Note**: Per-story Gherkin scenarios are finalized jointly with Scrum Master in `docs/stories-farragh-marketplace.md`. This document sets the strategy those scenarios must satisfy.

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Risk | Test Level |
|---|---|---|---|---|---|
| Auth (JWT, roles) | Critical (5) | Low (2) | Medium (3) | 10 | Maximum |
| Zone matching (PostGIS) | Critical (5) | Medium (3) | High (5) | 13 | Maximum |
| Payment (CMI mock) | Critical (5) | Medium (3) | High (5) | 13 | Maximum |
| Request lifecycle/status | High (4) | Medium (3) | Medium (3) | 10 | High |
| Municipality bulk-subscribe | Medium (3) | Low (2) | Medium (3) | 8 | Standard |
| Admin panel | Medium (3) | Low (2) | Low (1) | 6 | Standard |
| FR/AR i18n + RTL | High (4) | Low (2) | Medium (3) | 9 | High |
| Email notifications | Low (2) | Low (2) | Low (1) | 5 | Standard |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit | ≥ 60% of business logic | JUnit 5 + Mockito (backend), Jasmine/Karma or Jest (Angular) |
| Integration | ≥ 40% of API + DB layer | Spring Boot Test + Testcontainers (real Postgres+PostGIS, not mocked) |
| E2E | Critical happy paths only | Playwright |
| **Combined gate** | **≥ 80%** — non-negotiable | CI blocks merge if below (see DevOps doc) |

**Auth, Zone matching, Payment** (risk 13/13/10): also get adversarial review before any sprint touching them ships.

## 3. ATDD Acceptance Scenarios (critical paths — representative; full set lives in Stories)
```gherkin
Feature: Post and match a pickup request

  Scenario: Household posts a request and a matching recycler sees it
    Given a Recycler has declared a coverage zone containing the request's location
    And the Recycler accepts the request's material type
    When a Household posts a pickup request with that material and location
    Then the request appears in the Recycler's feed
    And the request does NOT appear in the feed of a Recycler outside that zone

  Scenario: Recycler cannot accept a request outside their zone
    Given a Recycler's coverage zone does not contain a request's location
    When the Recycler attempts to accept that request via the API
    Then the request is rejected with 403/404 (not just hidden client-side)

  Scenario: Mock payment succeeds after request completion
    Given a request has status COMPLETED
    When the Household triggers payment
    Then a payment record is created with mode=MOCK and status=SUCCEEDED
    And the request cannot be paid for twice

  Scenario: Status cannot skip states
    Given a request has status POSTED
    When an API call attempts to set status directly to COMPLETED
    Then the transition is rejected — only POSTED→ACCEPTED→SCHEDULED→COMPLETED (or →CANCELLED) is valid
```

## 4. Adversarial Checklist (applies to Auth, Zone matching, Payment — risk ≥ 10)
- [ ] IDOR: Household A cannot read/cancel Household B's request via ID guessing
- [ ] IDOR: Recycler cannot accept/update a request outside their declared zone by crafting the request directly (bypass client filtering)
- [ ] Role escalation: JWT role claim tampering rejected (signature validation)
- [ ] Race condition: two recyclers accepting the same request simultaneously — only one wins, other gets a clean conflict error
- [ ] Payment double-submit: triggering payment twice on the same request does not create two payment records
- [ ] Input abuse: zone polygon with self-intersecting/invalid geometry rejected gracefully, not a 500
- [ ] Input abuse: RTL/unicode text (Arabic) in address/name fields doesn't break validation, storage, or rendering
- [ ] Boundary: request exactly on a zone's radius edge (`ST_DWithin` boundary) — verify inclusive/exclusive behavior is intentional, not accidental

## 5. Release Gate Criteria
- [ ] All ATDD acceptance scenarios pass
- [ ] Combined unit + integration coverage ≥ 80% (per CLAUDE.md rule 6)
- [ ] No critical/high security findings open (Security Engineer sign-off)
- [ ] Adversarial checklist above cleared for Auth, Zone matching, Payment
- [ ] E2E happy path passes for: post request → recycler accepts → status progression → mock payment
- [ ] Every screen in UX doc verified in both `dir="ltr"` and `dir="rtl"` (per UI doc RTL strategy)
- [ ] Video recording of E2E flows: **deferred until the final sprint of this version** per explicit user instruction (not required at every sprint's gate)

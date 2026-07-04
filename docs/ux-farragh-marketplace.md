# UX Foundation: Farragh.ma
**PRD Reference**: docs/prd-farragh-marketplace.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: UX Designer

## 1. User Personas
## Persona: Amina (Household)
**Who**: Urban homeowner, mobile-first, moderate tech savviness
**Goal**: Get her sorted plastic/electronics picked up without driving to a depot
**Frustration**: Doesn't know which recycler takes what, distrusts informal collectors
**Context**: Uses phone during evenings, expects the process in French or Arabic
**Quote**: "I just want to know someone certified is coming, and when."

## Persona: Youssef (Certified Recycler)
**Who**: Runs a small certified recycling operation, uses a phone/tablet in the field
**Goal**: Fill his truck route with relevant, in-zone pickup jobs without cold-calling
**Frustration**: No digital channel today — relies on word of mouth
**Context**: Checks the feed a few times a day between routes

## 2. Information Architecture / Site Map
```
[App Root]
├── Public
│   ├── Landing / How it works
│   ├── Login
│   └── Register (role selector: Household/SME, Recycler, Municipality)
├── Household/SME (post-login)
│   ├── My Requests (list — statuses)
│   ├── New Request (material, quantity, address/pin, photo optional)
│   ├── Request Detail (status, recycler info once accepted, payment)
│   └── Profile
├── Recycler (post-login)
│   ├── Request Feed (matched by zone + materials)
│   ├── My Accepted Requests (status update actions)
│   ├── My Coverage Zones (declare/edit)
│   └── Profile / Certification info
├── Municipality (post-login)
│   ├── My Subscriptions (bulk zones)
│   ├── New Subscription (draw/select zone)
│   └── Profile
└── Admin (post-login)
    ├── Users (search, deactivate)
    ├── Requests (search, override status)
    └── Transactions (mock payment log)
```

## 3. Core User Flows

### Flow 1: Household posts and tracks a pickup request
```
Feature: Post pickup request
Actor: Amina (Household)

Happy Path:
(Login) → [My Requests] → [New Request: pick material, describe qty, set address/pin] → [Submit]
        → [Request Detail: status=POSTED] → <Recycler accepts?>
        → [Status=ACCEPTED, recycler contact shown] → [Status=SCHEDULED] → [Status=COMPLETED]
        → [Payment step (CMI mock)] → (Success)

Error Paths:
[New Request] → <Address/pin missing?> → [Inline validation error] → [New Request retry]
[Payment step] → <Mock payment fails?> → [Error message + retry action]

Edge Cases:
- No recycler accepts within a reasonable window → [status stays POSTED, show "still looking" state, allow cancel]
- User cancels after ACCEPTED → <Confirm cancellation> → [Status=CANCELLED, recycler notified]
```

### Flow 2: Recycler reviews feed and accepts a request
```
Feature: Accept pickup request
Actor: Youssef (Recycler)

Happy Path:
(Login) → [Request Feed: filtered by my zone + materials] → [Open request detail]
        → [Accept] → [Moves to My Accepted Requests] → [Update status: Scheduled → Completed]

Error Paths:
[Accept] → <Another recycler already accepted (race)?> → [Error: "no longer available", refresh feed]

Edge Cases:
- Feed is empty (no matching requests) → [Empty state: "No requests in your zone right now"]
- Recycler hasn't declared a zone yet → [Prompt: "Declare a coverage zone to start seeing requests"]
```

### Flow 3: Municipality bulk-subscribes a zone
```
Feature: Bulk subscribe
Actor: Municipality admin

Happy Path:
(Login) → [My Subscriptions] → [New Subscription: select/draw zone] → [Submit] → [Zone active, residents covered]

Error Paths:
[New Subscription] → <Zone overlaps existing subscription?> → [Warning, allow confirm or edit]

Edge Cases:
- Zone drawing tool unavailable (no geo data source) → [Fallback: enter zone by neighborhood name/postal code]
```

## 4. Key Screen Wireframes (text-based)

### Screen: New Request (Household)
```
┌─────────────────────────────────────┐
│ [← Back]   Nouvelle demande         │
├─────────────────────────────────────┤
│ Matériau: [Dropdown: Plastique ▾]   │
│ Quantité (approx.): [____________]  │
│ Adresse: [____________] [📍 Pin]    │
│ Photo (optionnel): [Upload]         │
│                                     │
│        [ Publier la demande ]       │
├─────────────────────────────────────┤
│ (bottom nav: Mes demandes | Profil) │
└─────────────────────────────────────┘
```

### Screen: Recycler Feed
```
┌─────────────────────────────────────┐
│ Demandes dans ma zone               │
├─────────────────────────────────────┤
│ [Plastique — 2km — Voir]            │
│ [Métal — 4km — Voir]                │
│ (empty state: "Aucune demande       │
│  actuellement dans votre zone")     │
├─────────────────────────────────────┤
│ (bottom nav: Feed | Mes acceptées |  │
│  Mes zones | Profil)                │
└─────────────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| My Requests | "Vous n'avez pas encore de demande" + CTA | Skeleton list | "Impossible de charger" + retry | List with status badges |
| Request Feed | "Aucune demande dans votre zone" | Skeleton list | "Impossible de charger" + retry | List sorted by distance |
| Request Detail | n/a | Skeleton | "Demande introuvable" | Full detail + status timeline |
| New Request | n/a | Submit spinner | Inline field errors / "Erreur serveur" | Redirect to detail, confirmation toast |

## RTL/i18n Note
Every screen above must be validated in both LTR (French) and RTL (Arabic) layouts — this is a UI Designer requirement carried forward from here, not an afterthought (see Architecture risk on RTL retrofit cost).

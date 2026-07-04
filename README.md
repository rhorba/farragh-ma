# Farragh.ma — Recycling Pickup Marketplace

Morocco's Green Law 28-00 requires certified waste treatment. No consumer-facing recycling pickup platform exists.

## Problem
Households and SMEs don't know which certified recyclers accept what materials. Recyclers have no digital lead-gen channel.

## Solution
Marketplace + scheduling: post a pickup request by material type → certified recycler accepts → pickup confirmed. Municipalities can bulk-subscribe for neighborhood coverage.

## Stack
Java 25 (LTS) + Spring Boot 4.1 + Maven, Angular 21 (LTS) + Angular Material, PostgreSQL 16 + PostGIS (coverage zones), CMI payments (mock mode), Docker Compose deployment. See `docs/` for the full foundation doc chain (PRD, system design, architecture, security, database, UX/UI, test strategy, DevOps, sprint backlog).

## Key Roles
Household / SME | Certified Recycler | Municipality | Admin

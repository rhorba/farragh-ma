# CORRECTIONS — Farragh.ma



## 2026-07-04 — Doc correction: Spring Boot version
CORRECTION: Approved docs (PRD, Architecture, System Design, README) said "Spring Boot 3.x" for the Java 25 backend. This is factually wrong — Spring Boot 3.x cannot run on Java 25 (its bytecode plugin doesn't recognize class file version 69). Java 25 first-class support requires Spring Boot 4.0+; picked 4.1 (latest stable, supports through Java 26).
Discovered during Sprint 1 Story 1.1 scaffolding when the Docker build failed with "Unsupported class file major version 69".
User approved the fix (upgrade path, not reverting Java 25) via AskUserQuestion. All docs updated to say Spring Boot 4.1.

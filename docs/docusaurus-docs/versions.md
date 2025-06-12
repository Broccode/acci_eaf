---
sidebar_position: 99
---

# Version Matrix (Canonical)

This page lists **single sources of truth** for key dependency versions used across ACCI EAF. Update
here first, then reference everywhere else (docs, code examples, README).

| Category           | Artifact / Tool                         | **Pinned MVP Version**  | Notes                           |
| ------------------ | --------------------------------------- | ----------------------- | ------------------------------- |
| JDK                | Temurin JDK                             | 21                      | Minimum compatible version      |
| Kotlin             | `org.jetbrains.kotlin.jvm`              | **1.9.23**              | Used in `build.gradle.kts` root |
| Spring Boot        | `org.springframework.boot`              | **3.2.5**               | Align all modules + examples    |
| Spring Framework   | (Pulled by Boot)                        | 6.1.x                   | Do not override manually        |
| Testcontainers BOM | `org.testcontainers:testcontainers-bom` | **1.19.7**              | Import once in root Gradle      |
| NATS Java Client   | `io.nats:jnats`                         | **2.17.0**              | Works with NATS ≥ 2.11          |
| Vaadin (Hilla)     | `@vaadin` / `com.vaadin.hilla`          | 24.x / 2.5.x            | Pending final UI choice         |
| Node.js            |                                         | 18.x LTS                | For docs/UI kit                 |
| Nx CLI             |                                         | Latest (currently 21.1) | Managed via `package.json`      |
| Docker Desktop     |                                         | 4.x (23.x engine)       | Ensure Compose V2 support       |

_If you bump a version, add a row in the Change Log below._

## Change Log

| Date       | Change                 | Author   |
| ---------- | ---------------------- | -------- |
| 2025-06-11 | Initial matrix created | AI Agent |

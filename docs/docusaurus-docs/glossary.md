---
sidebar_position: 98
---

# Glossary

A quick reference for recurring terms and acronyms in ACCI EAF.

| Term / Acronym | Definition |
| -------------- | ---------- |
| **ACCI EAF** | Axians Competence Center Infrastructure — Enterprise Application Framework. |
| **CQRS** | Command-Query Responsibility Segregation — separates reads from writes. |
| **ES / Event Sourcing** | Persist state changes as an immutable sequence of events. |
| **EDA** | Event-Driven Architecture. Components communicate via events (NATS). |
| **NATS** | High-performance messaging system; JetStream adds persistence. |
| **JetStream** | NATS extension providing streams, consumers, at-least-once delivery. |
| **Aggregate** | DDD root object that enforces invariants for a consistency boundary. |
| **Domain Event** | Immutable record of something that happened in the domain. |
| **Port** | Hexagonal interface defining interaction with the outside world. |
| **Adapter** | Implementation of a port (e.g., REST controller, JPA repository). |
| **Tenant** | Isolated customer space; primary multi-tenant discriminator. |
| **Projector** | Component that builds or updates a read model by consuming events. |
| **Read Model** | Optimised data structure for queries; updated asynchronously. |
| **Transactional Outbox** | Pattern to atomically store events with DB write before publishing. |
| **Testcontainers** | Java/Kotlin library for on-demand Dockerised dependencies in tests. |
| **ADR** | Architecture Decision Record; structured log of significant decisions. |
| **Launchpad** | Docusaurus-based developer portal for EAF docs. |

_See anything missing?  Feel free to submit a PR to expand the glossary._

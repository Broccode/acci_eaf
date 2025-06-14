# Architectural Principles

The ACCI EAF is built on a foundation of proven architectural patterns and development practices
that ensure scalability, maintainability, and developer productivity.

## Core Principles

ACCI EAF mandates the use of four key architectural principles:

### 🏗️ [Domain-Driven Design (DDD)](/architecture/domain-driven-design)

Model your business domain with precision using aggregates, entities, value objects, and domain
events.

### 🔷 [Hexagonal Architecture](/architecture/hexagonal-architecture)

Isolate your application core from infrastructure concerns using ports and adapters.

### ⚡ [CQRS/Event Sourcing](/architecture/cqrs-event-sourcing)

Separate read and write models while using events as the source of truth.

### 🧪 [Test-Driven Development (TDD)](/architecture/test-driven-development)

Write tests first to ensure robust, well-designed code from the start.

## Why These Principles?

These architectural patterns work together to create applications that are:

- **Maintainable**: Clear separation of concerns and well-defined boundaries
- **Testable**: Every component can be tested in isolation
- **Scalable**: Event-driven architecture supports horizontal scaling
- **Evolvable**: Business logic remains independent of technical implementation details
- **Resilient**: Event sourcing provides a complete audit trail and recovery capabilities

## Getting Started

Begin with the [Domain-Driven Design guide](/architecture/domain-driven-design) to understand how to
model your business domain, then explore how
[Hexagonal Architecture](/architecture/hexagonal-architecture) helps organize your code structure.

_This documentation provides guidance for implementing these principles within the ACCI EAF
context._

Welcome to the architectural hub for the ACCI Enterprise Application Framework (EAF). This section
provides in-depth guidance on the core principles that shape EAF services.

### How to Read This Section

This section is designed for both learning and reference. If you are new to these concepts, we
recommend reading them in the following order:

1. **Start with the EAF Overview** to get a high-level picture of the framework's goals and design
   philosophy.
2. Begin with the [Domain-Driven Design guide](/architecture/domain-driven-design) to understand how
   to model your business domain.
3. [Hexagonal Architecture](/architecture/hexagonal-architecture) helps organize your code
   structure.
4. Finally, dive into **CQRS/ES** and **TDD** for advanced patterns and practices.

# Architectural Principles

The ACCI EAF is built on a foundation of proven architectural patterns and development practices that ensure scalability, maintainability, and developer productivity.

## Core Principles

ACCI EAF mandates the use of four key architectural principles:

### 🏗️ [Domain-Driven Design (DDD)](/docs/architectural-principles/ddd)

Model your business domain with precision using aggregates, entities, value objects, and domain events.

### 🔷 [Hexagonal Architecture](/docs/architectural-principles/hexagonal)

Isolate your application core from infrastructure concerns using ports and adapters.

### ⚡ [CQRS/Event Sourcing](/docs/architectural-principles/cqrs-es)

Separate read and write models while using events as the source of truth.

### 🧪 [Test-Driven Development (TDD)](/docs/architectural-principles/tdd)

Write tests first to ensure robust, well-designed code from the start.

## Why These Principles?

These architectural patterns work together to create applications that are:

- **Maintainable**: Clear separation of concerns and well-defined boundaries
- **Testable**: Every component can be tested in isolation
- **Scalable**: Event-driven architecture supports horizontal scaling
- **Evolvable**: Business logic remains independent of technical implementation details
- **Resilient**: Event sourcing provides a complete audit trail and recovery capabilities

## Getting Started

Begin with the [Domain-Driven Design guide](/docs/architectural-principles/ddd) to understand how to model your business domain, then explore how [Hexagonal Architecture](/docs/architectural-principles/hexagonal) helps organize your code structure.

*This documentation provides guidance for implementing these principles within the ACCI EAF context.*

# Architecture Decision Record: Hilla KotlinNullabilityPlugin Workaround

Date: 2024-08-15

## Status

Accepted

## Context

A `java.lang.ClassCastException` occurs in Hilla v24.8.0.alpha5 during OpenAPI generation when
processing a Kotlin collection (e.g., `Set<String>`) that has an aliased Jackson `@JsonProperty`
(e.g., `@JsonProperty("email_addresses")`).

This is caused by a structural variant in the Abstract Syntax Tree (`CompositeTypeSignatureNode`)
that the downstream `KotlinNullabilityPlugin` is not designed to handle, leading to an unsafe cast.
This issue is tracked in [Hilla GitHub issue #3443](https://github.com/vaadin/hilla/issues/3443) and
blocks development of Hilla endpoints that consume DTOs with such aliased collection properties.

## Decision

We will implement a temporary workaround by creating dedicated, "clean" Data Transfer Objects (DTOs)
for Hilla endpoints. These Hilla-specific DTOs will not contain `@JsonProperty` aliases on
collection fields, thus avoiding the AST structure that triggers the bug.

Mapping between the external-facing DTOs (with aliases) and the internal Hilla DTOs will be handled
in the service layer or via extension functions. This isolates the workaround and allows for easy
removal once the upstream bug is fixed.

## Consequences

### Positive

- Unblocks development on affected Hilla endpoints.
- Maintains a clean separation between the external API contract and the frontend DTO contract.
- The workaround is isolated and can be easily tracked and reverted.

### Negative / Trade-offs

- Introduces boilerplate code (DTO classes and mapping logic).
- Requires developers to remember to apply the pattern for affected endpoints.
- Adds a temporary layer of complexity to the data flow.

## Alternatives Considered

1. **Match JsonProperty to Field Names**: Not always feasible as we may not control the external API
   contract.
2. **Use Simple Types (e.g., Comma-separated String)**: Introduces unnecessary parsing/serialization
   logic and clutters the domain.
3. **Disable Affected Endpoints**: Halts development and delivery of required features.

---

_This ADR documents the accepted temporary solution to the Hilla `KotlinNullabilityPlugin` issue._

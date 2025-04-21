# ADR-001: Selection of CASL as RBAC Implementation Library

* **Status:** Accepted
* **Date:** 2025-04-25
* **Authors:** [System Architect]

## Context

The ACCI EAF requires a robust Role-Based Access Control (RBAC) system with the following key requirements:

1. Support for defining and enforcing role-based permissions
2. Ability to work with attribute-based rules (particularly ownership checking)
3. Tenant isolation in permission checks
4. Integration with NestJS
5. Typesafe API with good TypeScript support
6. Maintainable and extensible permission definitions
7. Good performance characteristics for permission checking
8. Active maintenance and community support

Several options were considered:

* **CASL**: A versatile isomorphic authorization library
* **AccessControl**: A role and attribute-based access control library
* **NestJS RBAC**: NestJS-specific RBAC implementation
* **Custom implementation**: Building our own RBAC system from scratch

## Decision

We will use **CASL** as the RBAC implementation library for the ACCI EAF.

## Rationale

CASL was selected for the following reasons:

1. **Expressiveness**: CASL provides a flexible and intuitive API for defining permissions that goes beyond simple role-based checks, supporting attribute-based conditions like ownership.

2. **TypeScript Support**: CASL offers excellent TypeScript integration with strong typing, which aligns with our development practices.

3. **Framework Agnostic**: While being easily integrable with NestJS, CASL is framework-agnostic, which aligns with our hexagonal architecture principle of separating core logic from infrastructure.

4. **Active Maintenance**: CASL is actively maintained with regular updates and has a responsive community.

5. **Performance**: CASL has good performance characteristics, especially with its ability to compile permission rules for faster runtime checks.

6. **Multi-tenancy Support**: CASL can be configured to incorporate tenant context in permission rules, which is essential for our multi-tenant architecture.

7. **Documentation Quality**: CASL has comprehensive documentation with examples that cover our use cases.

8. **Community Adoption**: CASL is widely used in production applications, indicating its reliability and effectiveness.

9. **Integration Examples**: There are several examples and articles demonstrating CASL integration with NestJS, which will accelerate our implementation.

The other options were rejected for the following reasons:

* **AccessControl**: While capable, it has fewer features for attribute-based conditions and less active development.
* **NestJS RBAC**: Too tightly coupled to NestJS, which conflicts with our hexagonal architecture approach.
* **Custom implementation**: Would require significant development effort and introduce unnecessary risk without providing substantial advantages over existing solutions.

## Consequences

### Positive

* Faster development of the RBAC system with a proven library
* Better maintainability through the use of a well-documented standard library
* Strong typing support for permission rules
* Ability to implement both RBAC and basic ABAC (ownership) requirements

### Negative

* Team needs to learn CASL's specific API and concepts
* Potential performance overhead compared to a highly optimized custom solution
* Dependency on a third-party library for a critical security component

### Mitigations

* Provide training and examples for the team on CASL usage
* Create abstraction layers where necessary to isolate core business logic from CASL-specific code
* Implement comprehensive testing of permission rules
* Monitor CASL performance in our specific use cases and optimize as needed

## Implementation Notes

* CASL will be integrated primarily in the `libs/rbac` package
* We'll create NestJS guards that use CASL for permission checking
* Tenant context will be incorporated into CASL's ability context
* Permission definitions will be centralized and organized by domain area
* We'll use CASL's ability caching features to optimize performance

## Alternatives Considered

### AccessControl

AccessControl provides a simpler, more opinionated RBAC implementation. While it offers good role-based permission control, it's less flexible for attribute-based conditions and has fewer active contributors.

### NestJS RBAC

NestJS has its own RBAC implementation, but it's tightly coupled to the framework and lacks some of the flexibility we need, particularly for attribute-based conditions and tenant isolation.

### Custom Implementation

Building our own solution would give maximum flexibility but would require significant development time and introduce potential security risks. It would also require ongoing maintenance effort.

## References

* [CASL Documentation](https://casl.js.org/)
* [CASL GitHub Repository](https://github.com/stalniy/casl)
* [NestJS with CASL Integration Example](https://github.com/nestjsx/nest-casl)
* [AccessControl Documentation](https://onury.io/accesscontrol/)
* [NestJS Access Control Documentation](https://docs.nestjs.com/security/authorization)

# ADR-003: License Validation Mechanism

* **Status:** Accepted
* **Date:** 2025-04-25
* **Authors:** [Product Architect]

## Context

The ACCI EAF is designed to be used as the foundation for enterprise applications that will be licensed to customers. A robust license validation mechanism is required to enforce licensing terms, protect intellectual property, and support the business model. This mechanism needs to balance security, reliability, and user experience.

Key requirements for the license validation system:

1. Validate that the application is being used according to the license terms (e.g., number of users, expiration date, licensed features)
2. Prevent simple circumvention of licensing controls
3. Support for both online and offline validation
4. Graceful handling of validation failures
5. Support for license lifecycle management (trial, expiration, renewal)
6. Compatibility with multi-tenant architecture
7. Minimal performance impact
8. Audit capability for license usage

Several approaches for license validation were considered:

* **Online validation with a license server**
* **Offline validation with cryptographically signed license files**
* **Hybrid approach combining both methods**
* **Hardware-based validation (e.g., dongles)** - immediately ruled out as impractical for cloud-based software

## Decision

We will implement a **hybrid license validation mechanism** that combines cryptographically signed license files with optional online validation.

The core of the system will be:

1. JSON license files containing license metadata, signed with asymmetric cryptography (RSA)
2. License validation on application startup and periodic runtime checks
3. Optional connectivity to an Axians license server for enhanced validations and telemetry
4. Graceful degradation with configurable grace periods when validation fails
5. Secure storage of metrics for license compliance reporting

## Rationale

The hybrid approach was selected for the following reasons:

1. **Flexibility**: Supports both online and offline environments, meeting various customer deployment scenarios.

2. **Security**: Asymmetric cryptography provides strong protection against license tampering while being impossible to reverse engineer from the application code.

3. **User Experience**: Allows for graceful degradation and configurable grace periods, preventing sudden interruptions of business operations.

4. **Business Model Support**: Enables enforcement of various licensing models (per-user, per-core, feature-based, time-based).

5. **Technical Feasibility**: Can be implemented without external dependencies and integrated into the EAF architecture.

6. **Multi-Tenant Compatibility**: License validation can be tied to specific tenants, supporting multi-tenant deployments.

7. **Auditing Capability**: Secure metrics collection provides data for license compliance and usage analysis.

8. **Low Performance Impact**: Validation occurs primarily at startup and periodically thereafter, with minimal runtime impact.

Other approaches were rejected for the following reasons:

* **Online-only validation**: Too restrictive for customers in air-gapped or restricted network environments.
* **Offline-only validation**: Limits ability to enforce changing license terms and gather usage metrics.
* **Hardware-based validation**: Impractical for cloud/virtualized environments and increases deployment complexity.

## Consequences

### Positive

* Supports various licensing models (user-based, core-based, feature-based, time-based)
* Works in both online and offline environments
* Provides secure license enforcement
* Enables license analytics and usage monitoring
* Supports graceful handling of validation failures

### Negative

* Increased complexity compared to simpler approaches
* Requires key management infrastructure for signing licenses
* Needs proper documentation for license administration
* May face resistance from customers regarding telemetry features

### Mitigations

* Develop clear documentation for license administration
* Create tools to simplify license generation and management
* Make telemetry collection transparent and configurable
* Implement strict security controls around cryptographic keys
* Thoroughly test across various scenarios

## Implementation Notes

### License File Format

The license file will be a JSON document containing:

* License ID
* Customer information
* Issuance and expiration dates
* Licensed capabilities/features
* Constraints (users, cores, etc.)
* Cryptographic signature

### Validation Process

1. **Startup Validation**:
   * Load and validate license file signature
   * Check basic constraints (expiration, customer ID)
   * Initialize license state and enforcement

2. **Runtime Validation**:
   * Periodic re-validation (configurable interval)
   * Constraint checks based on actual usage
   * Optional connection to license server (if online)

3. **Enforcement**:
   * Feature-level enforcement (disable features not in license)
   * Constraint enforcement (user limits, etc.)
   * Grace period handling when validation fails
   * Degraded operation modes for expired licenses

### Security Measures

* Asymmetric cryptography (RSA) for license signing
* Secure storage of license state
* Obfuscation of validation code
* Anti-debugging measures for critical validation paths

### Integration Points

* Core EAF initialization process
* Feature activation/configuration system
* Tenant management system
* Application metrics and monitoring

## Alternatives Considered

### Online-Only Validation with License Server

Benefits:

* Stronger enforcement of license terms
* Real-time license updates
* Better usage analytics

Drawbacks:

* Requires continuous internet access
* Single point of failure
* Customer resistance to "calling home"
* Requires building and maintaining a license server infrastructure

### Offline-Only with License Files

Benefits:

* Works in any environment
* Simpler implementation
* No external dependencies

Drawbacks:

* Harder to update license terms
* Limited usage analytics
* No ability to remotely disable licenses

### Commercial Licensing Solutions

Benefits:

* Ready-to-use solution
* Professionally maintained

Drawbacks:

* Additional cost
* Less control over implementation details
* Potential integration challenges with our architecture

## References

* [JSON Web Tokens](https://jwt.io/)
* [RSA Cryptography](https://en.wikipedia.org/wiki/RSA_(cryptosystem))
* [Software License Management Best Practices](https://www.flexera.com/blog/software-monetization/software-license-management-best-practices/)
* [License Management in Multi-Tenant Applications](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7516098/)

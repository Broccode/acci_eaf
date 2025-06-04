## Operational Guidelines

### Error Handling Strategy

- **General Approach:** Utilize exceptions for error signaling in Kotlin/Spring backend services. A
  clear hierarchy of custom EAF exceptions (e.g., `EafValidationException`,
  `EafAuthorizationException`, `EafResourceNotFoundException`) extending base Spring/Java exceptions
  will be established. APIs will return standardized error responses (e.g., JSON problem details).
- **Logging (Mandatory):**
  - Library/Method: SLF4J with Logback, configured for structured JSON logging.
  - Format: JSON, including timestamp, severity, service name, thread ID, correlation ID (see
    below), logger name, message, stack trace (for errors), and custom contextual fields (e.g.,
    `tenant_id`, `user_id` where appropriate and safe).
  - Levels: Standard DEBUG, INFO, WARN, ERROR. Consistent usage guidelines will be provided.
  - Context & Correlation IDs: All log messages related to a single request/operation across
    services must include a unique Correlation ID (e.g., propagated via HTTP headers or NATS message
    headers) for distributed tracing.
- **Specific Handling Patterns:**
  - **External API Calls (e.g., to `corenode` or federated IdPs):** Implement resilient client
    patterns:
    - Timeouts (connect and read).
    - Retries with exponential backoff and jitter for transient errors (using libraries like Spring
      Retry or Resilience4j).
    - Circuit Breaker pattern for services prone to unresponsiveness to prevent cascading failures.
    - Clear mapping of external API errors to internal EAF exceptions.
  - **Internal Errors / Business Logic Exceptions:** Custom exceptions will be caught by global
    exception handlers in API layers (e.g., Spring `@ControllerAdvice`) and translated into standard
    API error responses. Sensitive details will not be exposed to clients.
  - **NATS/JetStream Interactions (Event Publishing/Consumption):**
    - Publishing: At-least-once delivery with `PublishAck` handling. Retries for failed publishes.
      Consider Transactional Outbox for critical events if simple retries are insufficient.
    - Consumption: Idempotent consumers are mandatory. Explicit `ack()`, `nak()`, `term()` for
      messages. DLQ strategy for \"poison pill\" events. Robust error handling within listeners as
      per `NATS Multi-Tenancy JetStream Research` (section 2.4).
  - **Event Store Operations (Optimistic Concurrency):** The EAF Event Store SDK will handle
    optimistic concurrency exceptions during event appends and propagate them clearly for command
    handlers to manage (e.g., by retrying the command with re-read state or failing the operation).
  - **Transaction Management:**
    - Commands in CQRS should typically be processed within a single transaction that appends
      events.
    - Eventual consistency is accepted between the command side (event store) and read models.
    - Projectors updating read models should also manage their updates transactionally where
      appropriate and handle idempotency.

### Coding Standards

These standards are mandatory for all EAF core code and are strongly recommended for product
applications built on EAF. Linting and formatting tools will enforce many of these.

- **Primary Language:** Kotlin (for backend).
- **Style Guide & Linter/Formatter:**
  - Kotlin: Adhere to official Kotlin Coding Conventions.
  - Tooling: Spotless with Ktlint or Kfmt for automated formatting and linting, integrated into CI.
- **Naming Conventions (Kotlin):**
  - Packages: lowercase, dot-separated (e.g., `com.axians.eaf.iam.application.service`).
  - Classes/Interfaces/Objects/Enums/Annotations: PascalCase (e.g., `TenantProvisioningService`).
  - Functions/Methods/Properties (val/var): camelCase (e.g., `provisionNewTenant`).
  - Constants (compile-time `const val`, top-level or object `val` for true constants):
    SCREAMING_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT_MS`).
  - Test Methods: backticks for descriptive names (e.g.,
    ``fun `should provision tenant when valid data is provided`() {}``).
- **File Structure:** Adhere to standard Gradle project layout (`src/main/kotlin`,
  `src/test/kotlin`). Within these, organize by feature or architectural layer (e.g.,
  `domain/model`, `application/port/in`, `infrastructure/adapter/out/persistence`) consistent with
  Hexagonal Architecture.
- **Unit Test File Organization:** Co-located with source files or in a parallel structure within
  `src/test/kotlin` (e.g., `MyService.kt` in `main` has `MyServiceTest.kt` in `test` in the same
  package). Test file names end with `Test`.
- **Asynchronous Operations (Kotlin):** Primarily use Kotlin Coroutines for asynchronous
  programming. Adhere to structured concurrency principles. Avoid blocking threads unnecessarily.
- **Type Safety & Nullability (Kotlin):** Leverage Kotlin's null safety. Avoid platform types where
  possible. Use nullable types (`?`) explicitly. Minimize use of not-null assertion operator (`!!`).
- **Immutability:** Prefer immutable data structures (e.g., `val`, `List`, `Map`, Kotlin data
  classes with `val` properties) where practical, especially for DTOs, Value Objects, and Events.
- **Comments & Documentation:**
  - KDoc for all public APIs (classes, methods, properties).
  - Explain _why_, not _what_, for complex logic. Avoid redundant comments.
  - Module-level READMEs for significant libraries or services.
- **Dependency Management:** Use Gradle Version Catalogs (`libs.versions.toml`) for managing
  dependency versions. Avoid hardcoding versions in `build.gradle.kts` files.
- **Error Handling:** Use exceptions for error states. Define and use specific EAF exceptions. Avoid
  swallowing exceptions or catching generic `Exception` without specific handling or re-throwing.
- **Logging:** Use SLF4J for logging. Log meaningful messages with appropriate context (correlation
  IDs, tenant IDs where applicable and safe). Avoid logging sensitive information.
- **Spring Framework Usage:**
  - Prefer constructor injection for dependencies.
  - Use Spring Boot auto-configuration where possible.
  - Adhere to Spring best practices for defining beans, configurations, and profiles.

#### Detailed Kotlin Specifics

- **Functional Constructs:** Utilize Kotlin's functional programming features (lambdas, higher-order
  functions, collection operations like `map`, `filter`, `fold`) where they enhance readability and
  conciseness without sacrificing clarity.
- **Data Classes:** Use extensively for DTOs, Events, Commands, and simple Value Objects.
- **Sealed Classes/Interfaces:** Use for representing restricted hierarchies (e.g., different types
  of Commands for an Aggregate, or states in a state machine).
- **Extension Functions:** Use judiciously to enhance existing classes with utility functions,
  keeping them well-scoped and discoverable.
- **Coroutines:** Follow best practices for launching coroutines (e.g., within `CoroutineScope` tied
  to component lifecycle), error handling (e.g., `CoroutineExceptionHandler`), and context switching
  (`withContext`).

### Overall Testing Strategy

Testing is a cornerstone of ACCI EAF, with TDD being mandatory.

- **Tools (Backend):** JUnit 5, MockK (for Kotlin mocking), Spring Boot Test utilities, AssertJ (for
  fluent assertions), ArchUnit (for architecture rule validation), Testcontainers (for integration
  tests with PostgreSQL, NATS, etc.).
- **Unit Tests:**
  - **Scope:** Test individual Kotlin classes, functions, and small modules in isolation. Focus on
    business logic within Aggregates, Domain Services, Application Services, and utility classes.
  - **Location:** Typically co-located or in a parallel package structure in `src/test/kotlin`.
  - **Mocking/Stubbing:** Use MockK for creating test doubles. Mock all external dependencies (other
    services, database interactions via repository ports, NATS interactions via eventing ports).
  - **Target Coverage:** >80% line/branch coverage for core EAF logic.
- **Integration Tests:**
  - **Scope:** Test interactions between EAF components/services or between services and
    infrastructure (e.g., service layer to Event Store SDK to PostgreSQL; service layer to Eventing
    SDK to NATS). Test Hexagonal adapter integrations.
  - **Location:** Separate source set or directory (e.g., `src/integrationTest/kotlin`).
  - **Environment:** Use Testcontainers to spin up real instances of PostgreSQL and NATS/JetStream
    for reliable integration testing. Spring Boot test slices (e.g., `@DataJpaTest`,
    `@SpringBootTest` with specific components) will be used.
- **Architecture Tests (ArchUnit):**
  - **Scope:** Enforce architectural rules like layer dependencies (Hexagonal), DDD constraints,
    naming conventions, and domain purity (no infrastructure dependencies in domain code).
  - **Location:** Can be part of the main test suite or a dedicated test module.
- **End-to-End (E2E) Tests:**
  - **Scope:** Validate complete user flows or critical paths for applications like the ACCI EAF
    Control Plane MVP and the pilot application.
  - **Tools:** Playwright is mentioned for Hilla. This or similar for frontend E2E. For backend EAF
    service E2E tests, this might involve API-level tests orchestrating multiple service calls.
- **Performance Tests:**
  - **Scope:** Benchmark critical EAF operations and services against defined NFRs (response times,
    throughput).
  - **Tools:** JMeter, k6, or similar.
  - **Process:** Establish baseline benchmarks and run tests regularly (especially in CI) to detect
    performance regressions.
- **Test Data Management:** Strategies for generating and managing test data for various test levels
  will be defined (e.g., test data builders, setup scripts for integration tests).

### Security Best Practices

Security is paramount for ACCI EAF.

- **Authentication & Authorization (IAM Service):**
  - Strong authentication for all users and services (local users per tenant, federation via
    SAML/OIDC/LDAP, optional built-in MFA).
  - Robust RBAC and ABAC mechanisms for fine-grained access control, enforced by the IAM service and
    EAF SDKs.
  - Secure token management (e.g., JWTs) with appropriate lifetimes, signing, and validation.
- **Input Sanitization/Validation:** All external inputs (API requests, event payloads,
  user-provided data) must be rigorously validated at service boundaries (e.g., using Bean
  Validation in Spring, custom validation logic).
- **Output Encoding:** Contextual output encoding for data rendered in UIs (handled by Vaadin/Hilla
  components) and in API responses to prevent XSS.
- **Secrets Management:**
  - No hardcoded secrets. Use environment variables, Docker secrets, or a dedicated secrets
    management solution (e.g., HashiCorp Vault if feasible in target environments) for API keys,
    database credentials, signing keys.
  - EAF services will securely access secrets via a configuration mechanism.
- **Dependency Security:**
  - Regularly scan dependencies (both backend JVM and frontend JS/TS) for known vulnerabilities
    using tools like OWASP Dependency-Check (for scanning) and OWASP Dependency-Track (for ongoing
    monitoring of SBOMs), Snyk, or GitHub Dependabot.
  - Establish a process for timely patching of vulnerable dependencies.
- **Secure Inter-Service Communication:** All communication between EAF services (e.g., via NATS or
  internal APIs) must be over secure channels (e.g., TLS for NATS if configured, HTTPS for REST
  APIs). Authentication between services where necessary (e.g., service-to-service tokens).
- **Data Protection:**
  - Encryption of sensitive data at rest (e.g., PostgreSQL Transparent Data Encryption if
    available/applicable, or application-level encryption for specific fields).
  - Encryption of data in transit (TLS everywhere).
- **Principle of Least Privilege:** Services, users, and API clients should operate with the minimum
  necessary permissions.
- **API Security:**
  - Enforce HTTPS for all APIs.
  - Implement rate limiting and throttling to prevent abuse.
  - Use standard HTTP security headers (CSP, HSTS, X-Frame-Options, etc.) for web applications like
    the Control Plane.
- **Error Handling & Information Disclosure:** Error messages must not leak sensitive internal
  information. Detailed errors logged server-side, generic messages/error IDs to clients.
- **Auditing:** Comprehensive and secure audit trails for all critical administrative, IAM, and
  security-relevant events. Audit logs stored separately and securely.
- **Compliance Enablement:** Design with ISO27001, SOC2, and FIPS requirements in mind, facilitating
  product certification.
- **Regular Security Reviews & Testing:** Incorporate security testing (SAST, DAST, potentially
  penetration testing for mature EAF versions) into the development lifecycle.

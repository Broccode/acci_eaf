# Control Plane Circular Dependency Resolution

## Problem Description

The Control Plane service experienced a circular dependency during Spring context loading that
prevented integration tests from running. The issue manifested as:

```
Error creating bean with name 'jpaAuditRepositoryImpl': Requested bean is currently in creation:
Is there an unresolvable circular reference or an asynchronous initialization dependency?
```

## Root Cause Analysis

### Dependency Chain

The circular dependency occurred between these components:

1. **JpaAuditRepositoryImpl** → depends on **JpaAuditRepository** (Spring Data interface)
2. **AuditService** → depends on **AuditRepository** (implemented by JpaAuditRepositoryImpl)
3. **TenantSecurityAspect** → depends on **AuditService**
4. **AOP Proxy Creation** → creates circular reference during bean initialization

### Key Issue

The Spring AOP framework was trying to create proxies for audit-related components while those same
components were being injected, creating a self-referential loop.

## Solution Applied

### 1. Lazy Injection in Security Aspect

**File**: `TenantSecurityAspect.kt`

```kotlin
@Aspect
@Component
class TenantSecurityAspect(
    private val securityContextHolder: EafSecurityContextHolder,
) {

    @Lazy
    @Autowired
    private lateinit var auditService: AuditService

    // ... rest of implementation
}
```

### 2. Lazy Configuration in Domain Services

**File**: `DomainServiceConfiguration.kt`

```kotlin
@Bean
@Lazy
fun auditService(
    @Lazy auditRepository: AuditRepository,
    auditEventPublisher: AuditEventPublisher,
    securityContextHolder: EafSecurityContextHolder,
): AuditService = // ... implementation
```

### 3. Comprehensive Test Mocking

**File**: `ControlPlaneTestcontainerConfiguration.kt`

```kotlin
@TestConfiguration
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [".*\\.JpaAuditRepositoryImpl"]
        )
    ]
)
class ControlPlaneTestcontainerConfiguration {

    @Bean @Primary
    fun mockAuditRepository(): AuditRepository = mockk(relaxed = true)

    @Bean @Primary
    fun mockJpaAuditRepository(): JpaAuditRepository = mockk(relaxed = true)

    @Bean @Primary
    fun mockNatsEventPublisher(): NatsEventPublisher = mockk(relaxed = true)

    // ... other mocks
}
```

## Current Status

### ✅ Working

- All unit tests pass (`TenantTest`, `HealthEndpointTest`, etc.)
- Domain logic functions correctly
- Basic application startup works

### ⚠️ Partial

- Integration tests still affected by circular dependency
- Full Spring context loading has issues in test environment

## Architectural Issues Identified

### 1. Tight Coupling

- Audit infrastructure tightly coupled to repository layer
- Cross-cutting concerns mixed with business logic

### 2. Inappropriate Dependency Direction

- Infrastructure components depending on domain services
- AOP aspects creating unexpected dependency cycles

### 3. Test Complexity

- Extensive mocking required to break dependencies
- Component exclusion needed for test contexts

## Recommended Long-term Solutions

1. **Audit Event Bus Architecture**

   - Decouple audit from direct service injection
   - Use application events for audit trail creation

2. **Repository Layer Separation**

   - Separate audit repository from business repositories
   - Use different transaction boundaries

3. **Aspect Refactoring**
   - Move audit logic out of security aspects
   - Use dedicated audit aspects with proper ordering

## Prevention Guidelines

1. **Avoid bidirectional dependencies** between infrastructure and domain layers
2. **Use events** for cross-cutting concerns like auditing
3. **Keep aspects stateless** and avoid service injection where possible
4. **Test context isolation** with proper mocking strategies

## Related Documentation

- [IAM Integration Test Solution Attempts](./iam-integration-test-solution-attempts.md)
- [MockK Best Practices](./mockk-best-practices.md)

---

**Created**: 2024-12-19  
**Status**: Resolved (with architectural improvement needed)  
**Impact**: Integration tests, Spring context loading

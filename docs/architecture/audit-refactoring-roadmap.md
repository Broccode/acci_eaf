---
sidebar_position: 9
title: Audit Architecture Refactoring Roadmap
---

# Audit Architecture Refactoring Roadmap

## Executive Summary

This roadmap outlines a phased approach to transform the current tightly-coupled audit architecture
into a resilient, event-driven system. The journey addresses immediate circular dependency issues
while establishing a foundation for enterprise-scale audit capabilities.

**Duration**: 12-16 weeks  
**Team Size**: 2-3 senior engineers  
**Risk Level**: Low (incremental approach with rollback capability)

---

## Phase 0: Foundation & Preparation (Week 1-2)

### Goals

- Establish project infrastructure
- Create comprehensive test baseline
- Document current state architecture

### Deliverables

#### 0.1 Project Setup

- [ ] Create feature branch: `feature/audit-architecture-refactoring`
- [ ] Set up feature flags for gradual rollout
- [ ] Configure monitoring dashboards for audit metrics
- [ ] Establish performance baseline metrics

#### 0.2 Test Coverage Baseline

```kotlin
// Create integration test suite for current audit behavior
@SpringBootTest
@Tag("audit-baseline")
class AuditBaselineIntegrationTest {
    // Document current synchronous audit behavior
    // These tests will ensure backward compatibility
}
```

#### 0.3 Documentation

- [ ] Document current audit flow with sequence diagrams
- [ ] Create ADR for event-driven audit decision
- [ ] Update team wiki with refactoring approach

### Success Criteria

- ✅ All existing audit tests passing
- ✅ Performance metrics documented
- ✅ Team aligned on approach

---

## Phase 1: Break Circular Dependencies (Week 3-4)

### Goals

- Resolve immediate `BeanCurrentlyInCreationException`
- Stabilize Spring context initialization
- Enable all integration tests to pass

### Deliverables

#### 1.1 Apply Immediate Fixes

```kotlin
// Update TenantSecurityAspect with proper lazy initialization
@Aspect
@Component
class TenantSecurityAspect(
    private val securityContextHolder: EafSecurityContextHolder,
) {
    @Lazy
    @Autowired
    private lateinit var auditService: AuditService
}
```

#### 1.2 Fix Additional Repository Cycles

- [ ] Apply same pattern to `JpaRoleRepositoryImpl`
- [ ] Update `JpaUserRepositoryImpl` if affected
- [ ] Ensure all repository implementations follow consistent pattern

#### 1.3 Update Test Configuration

```kotlin
@TestConfiguration
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [".*\\.Jpa.*RepositoryImpl"]
        )
    ]
)
class ControlPlaneTestcontainerConfiguration {
    // Provide mocks for problematic beans
}
```

### Success Criteria

- ✅ `nx run-many --target=test` passes without circular dependency errors
- ✅ Application starts successfully in all environments
- ✅ No regression in existing functionality

---

## Phase 2: Introduce Event Infrastructure (Week 5-6)

### Goals

- Establish event-driven foundation
- Define audit event contracts
- Create port/adapter structure for events

### Deliverables

#### 2.1 Define Audit Domain Events

```kotlin
// Core domain events
package com.axians.eaf.controlplane.domain.event

sealed class AuditDomainEvent {
    abstract val eventId: String
    abstract val tenantId: TenantId
    abstract val timestamp: Instant
    abstract val correlationId: String?

    data class SecurityActionPerformed(
        override val eventId: String = UUID.randomUUID().toString(),
        override val tenantId: TenantId,
        val userId: UserId,
        val action: String,
        val resource: String,
        val result: SecurityResult,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String? = null,
    ) : AuditDomainEvent()
}
```

#### 2.2 Create Event Publisher Port

```kotlin
// Application port
interface DomainEventPublisher {
    suspend fun publish(event: AuditDomainEvent)
    suspend fun publishAll(events: List<AuditDomainEvent>)
}

// Infrastructure adapter
@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : DomainEventPublisher {
    override suspend fun publish(event: AuditDomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
```

#### 2.3 Implement Event Listeners

```kotlin
@Component
class AuditEventHandler(
    private val auditRepository: AuditRepository,
    private val tenantContextManager: TenantContextManager
) {
    @EventListener // Start with synchronous
    fun handle(event: AuditDomainEvent.SecurityActionPerformed) {
        // Process audit event
    }
}
```

### Success Criteria

- ✅ Event infrastructure deployed but not yet active
- ✅ All event contracts defined and reviewed
- ✅ Unit tests for event publishers and handlers

---

## Phase 3: Parallel Run & Validation (Week 7-8)

### Goals

- Run both synchronous and event-driven audit in parallel
- Validate event-driven audit produces identical results
- Build confidence before cutover

### Deliverables

#### 3.1 Dual-Write Implementation

```kotlin
@Aspect
@Component
class TenantSecurityAspect(
    private val securityContextHolder: EafSecurityContextHolder,
    private val eventPublisher: DomainEventPublisher,
    @Value("\${audit.dual-write.enabled:true}")
    private val dualWriteEnabled: Boolean
) {
    @Lazy
    @Autowired
    private lateinit var auditService: AuditService

    @AfterReturning("@annotation(requiresTenantAccess)")
    fun auditAccess(joinPoint: JoinPoint, requiresTenantAccess: RequiresTenantAccess) {
        // Legacy synchronous audit
        if (dualWriteEnabled) {
            auditService.logAccess(...)
        }

        // New event-driven audit
        val event = createAuditEvent(joinPoint)
        runBlocking { eventPublisher.publish(event) }
    }
}
```

#### 3.2 Audit Comparison Tool

```kotlin
@Component
class AuditConsistencyValidator {
    fun validateAuditConsistency(
        timeRange: ClosedRange<Instant>
    ): ConsistencyReport {
        val legacyAudits = legacyAuditRepo.findByTimeRange(timeRange)
        val eventAudits = eventAuditRepo.findByTimeRange(timeRange)

        return ConsistencyReport(
            matching = findMatching(legacyAudits, eventAudits),
            missingInNew = findMissing(legacyAudits, eventAudits),
            extraInNew = findExtra(legacyAudits, eventAudits)
        )
    }
}
```

### Success Criteria

- ✅ 100% audit consistency between old and new systems
- ✅ No performance degradation in dual-write mode
- ✅ Monitoring shows both audit paths functioning

---

## Phase 4: Async Migration (Week 9-10)

### Goals

- Enable asynchronous event processing
- Implement transactional guarantees
- Optimize performance

### Deliverables

#### 4.1 Enable Async Processing

```kotlin
@Configuration
@EnableAsync
class AsyncAuditConfiguration {
    @Bean("auditExecutor")
    fun auditExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 20
            queueCapacity = 500
            setThreadNamePrefix("audit-async-")
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            initialize()
        }
    }
}

// Update listener
@Component
class AuditEventHandler {
    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: AuditDomainEvent) {
        // Now processes asynchronously
    }
}
```

#### 4.2 Implement Transactional Outbox

```kotlin
// Add Spring Modulith dependency
dependencies {
    implementation("org.springframework.modulith:spring-modulith-events-api")
    implementation("org.springframework.modulith:spring-modulith-events-jdbc")
}

// Configure event publication registry
@Configuration
@EnableModulithEvents
class EventPublicationConfiguration {
    // Automatic transactional outbox pattern
}
```

#### 4.3 Multi-Tenant Context Propagation

```kotlin
@Component
class TenantAwareAuditEventHandler {
    @TransactionalEventListener
    @Async("auditExecutor")
    fun handle(event: AuditDomainEvent) {
        try {
            // Critical: Set tenant context from event
            tenantContextManager.setTenant(event.tenantId)

            // Process audit
            auditRepository.save(event.toAuditEntry())
        } finally {
            // Critical: Clear context
            tenantContextManager.clear()
        }
    }
}
```

### Success Criteria

- ✅ Async processing enabled with monitoring
- ✅ Zero audit events lost (verified by reconciliation)
- ✅ Request latency improved by >50%

---

## Phase 5: Legacy Cleanup & Optimization (Week 11-12)

### Goals

- Remove legacy synchronous audit code
- Optimize event processing pipeline
- Enhance monitoring and alerting

### Deliverables

#### 5.1 Remove Legacy Code

- [ ] Remove `@Lazy` annotations from aspects
- [ ] Delete old `AuditService` synchronous interface
- [ ] Clean up dual-write configuration
- [ ] Update all tests to use event-driven approach

#### 5.2 Performance Optimization

```kotlin
@Configuration
class AuditOptimizationConfig {
    @Bean
    fun auditEventBatcher(): EventBatcher<AuditDomainEvent> {
        return EventBatcher(
            batchSize = 100,
            maxWaitTime = Duration.ofMillis(500)
        )
    }
}
```

#### 5.3 Enhanced Monitoring

- [ ] Audit event processing rate dashboard
- [ ] Tenant-specific audit metrics
- [ ] Alert on processing delays > 5 seconds
- [ ] Dead letter queue monitoring

### Success Criteria

- ✅ All legacy code removed
- ✅ Performance meets or exceeds targets
- ✅ Comprehensive monitoring in place

---

## Phase 6: Advanced Capabilities (Week 13-16)

### Goals

- Implement advanced audit features
- Prepare for future CQRS evolution
- Establish audit analytics foundation

### Deliverables

#### 6.1 Audit Event Streaming

```kotlin
@Component
class AuditEventStreamer(
    private val natsTemplate: NatsTemplate
) {
    @EventListener
    fun streamToNats(event: AuditDomainEvent) {
        natsTemplate.publish(
            "audit.events.${event.tenantId.value}",
            event
        )
    }
}
```

#### 6.2 Read Model Projections

```kotlin
@Component
class AuditSummaryProjection {
    @EventHandler
    fun on(event: AuditDomainEvent) {
        // Update materialized views for fast queries
        auditSummaryRepo.updateSummary(event)
    }
}
```

#### 6.3 Audit Analytics API

```kotlin
@RestController
@RequestMapping("/api/audit/analytics")
class AuditAnalyticsController(
    private val queryService: AuditQueryService
) {
    @GetMapping("/activity-heatmap")
    suspend fun getActivityHeatmap(
        @RequestParam tenantId: String,
        @RequestParam period: String
    ): HeatmapData {
        return queryService.generateHeatmap(tenantId, period)
    }
}
```

### Success Criteria

- ✅ Real-time audit streaming operational
- ✅ Analytics queries < 100ms response time
- ✅ Foundation ready for full CQRS if needed

---

## Key Milestones & Checkpoints

### Milestone 1: System Stability (End of Phase 1)

- **Checkpoint**: All tests passing, no circular dependencies
- **Go/No-Go**: Team approval required to proceed

### Milestone 2: Event Foundation (End of Phase 3)

- **Checkpoint**: Parallel audit validation shows 100% consistency
- **Go/No-Go**: Architecture review and performance validation

### Milestone 3: Production Ready (End of Phase 5)

- **Checkpoint**: Async audit in production, legacy code removed
- **Go/No-Go**: Production metrics review, stakeholder sign-off

### Milestone 4: Enhanced Platform (End of Phase 6)

- **Checkpoint**: Advanced features operational
- **Go/No-Go**: Business value assessment

---

## Risk Mitigation

### Technical Risks

1. **Event Loss**: Mitigated by transactional outbox pattern
2. **Performance Degradation**: Mitigated by parallel run validation
3. **Tenant Context Leaks**: Mitigated by explicit context management

### Rollback Strategy

- Phase 1-2: Simple code revert
- Phase 3-4: Feature flag to disable event path
- Phase 5+: Requires migration back to synchronous (unlikely)

---

## Success Metrics

### Performance KPIs

- **Request Latency**: -60% reduction (150ms → 60ms)
- **Throughput**: +500% increase (60 RPS → 300 RPS)
- **Audit Lag**: <1 second for 99th percentile

### Architecture KPIs

- **Coupling Score**: High → Low (measured by dependency analysis)
- **Test Execution Time**: -40% for integration tests
- **Startup Time**: Consistent <5 seconds

### Business KPIs

- **Audit Completeness**: 100% (no events lost)
- **Compliance**: Zero audit-related findings
- **Developer Velocity**: +30% for audit-related features

---

## Team & Resources

### Core Team

- **Tech Lead**: Architecture decisions, code reviews
- **Senior Engineer 1**: Event infrastructure, core refactoring
- **Senior Engineer 2**: Testing, monitoring, performance

### Stakeholders

- **Platform Architect**: Weekly reviews
- **Security Team**: Audit compliance validation
- **DevOps**: Infrastructure and monitoring setup

### Training Needs

- [ ] Team workshop on event-driven architecture
- [ ] Spring Modulith deep-dive session
- [ ] Kotlin coroutines best practices

---

## Next Steps

1. **Week 1**: Schedule kickoff meeting with stakeholders
2. **Week 1**: Create feature branch and project board
3. **Week 2**: Complete Phase 0 deliverables
4. **Week 2**: Present baseline metrics to team

This roadmap provides a clear path from tactical fixes to strategic transformation, ensuring system
stability while building toward a scalable, event-driven architecture.

---

**Document Version**: 1.0  
**Last Updated**: 2024-12-20  
**Status**: Ready for Review

# EAF Axon Framework v5 Integration Plan

## Executive Summary

This document outlines the strategic integration of Axon Framework v5 into the ACCI EAF platform,
simultaneously resolving the critical circular dependency in the audit architecture. The plan
leverages existing EAF infrastructure (Event Store SDK, Eventing SDK) while introducing Axon's
advanced event sourcing capabilities.

## Current State Analysis

### Existing Infrastructure Assets

- **PostgreSQL Event Store**: `domain_events` table with required `global_sequence_id`
- **NATS/JetStream**: Fully configured with EAF Eventing SDK
- **Idempotent Projectors**: Framework for reliable event processing
- **Multi-tenancy**: Built-in tenant isolation patterns

### Critical Issue: Circular Dependency

```
TenantSecurityAspect → AuditService → AuditRepository → Spring AOP → [CYCLE]
```

Currently mitigated with `@Lazy` annotation (architectural debt).

## Integration Architecture

### Phase 1: Foundation Layer (Week 1-2)

#### 1.1 Axon EventStorageEngine Adapter

```kotlin
// Adapter for EAF PostgreSQL Event Store SDK
class EafPostgresEventStorageEngine(
    private val eafEventStore: EafEventStoreSdk,
    private val serializer: Serializer,
    private val persistenceExceptionResolver: PersistenceExceptionResolver
) : EventStorageEngine {
    // Implementation bridging Axon to existing event store
}
```

**Key Requirements**:

- Map Axon's `DomainEventMessage` to EAF event structure
- Ensure `global_sequence_id` compatibility with `TrackingToken`
- Maintain tenant isolation in all operations

#### 1.2 Database Schema Verification

```sql
-- Verify existing schema compatibility
ALTER TABLE domain_events
ADD COLUMN IF NOT EXISTS axon_aggregate_identifier VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_axon_tracking
ON domain_events (tenant_id, global_sequence_id);
```

### Phase 2: Security Context Evolution (Week 2-3)

#### 2.1 CorrelationDataProvider Implementation

```kotlin
@Component
class SecurityContextCorrelationDataProvider : CorrelationDataProvider {
    override fun correlationDataFor(message: Message<*>): Map<String, *> {
        val context = SecurityContextHolder.getContext()
        return mapOf(
            "tenant_id" to extractTenantId(context),
            "user_id" to extractUserId(context),
            "correlation_id" to UUID.randomUUID().toString(),
            "source_ip" to extractIpAddress()
        )
    }
}
```

#### 2.2 Refactor TenantSecurityAspect

```kotlin
@Aspect
@Component
class TenantSecurityAspect {
    // Remove: private val auditService: AuditService

    @Before("@annotation(requiresTenant)")
    fun enrichSecurityContext(joinPoint: JoinPoint) {
        // Only enrich context, no service calls
        MDC.put("tenant_id", extractTenantId())
        MDC.put("user_id", extractUserId())
    }
}
```

### Phase 3: Event-Driven Core Implementation (Week 3-4)

#### 3.1 First Aggregate Migration

```kotlin
// Example: User aggregate with built-in audit trail
class User {
    @AggregateIdentifier
    private lateinit var userId: UserId

    @CommandHandler
    constructor(command: CreateUserCommand) {
        // Domain events ARE the audit trail
        AggregateLifecycle.apply(
            UserCreatedEvent(
                userId = command.userId,
                email = command.email,
                roles = command.roles
                // tenant_id, user_id auto-added via CorrelationDataProvider
            )
        )
    }
}
```

#### 3.2 Programmatic Axon Configuration

```kotlin
@Configuration
class AxonConfiguration {
    @Bean
    fun configurer(
        eventStorageEngine: EventStorageEngine,
        correlationProviders: List<CorrelationDataProvider>
    ): Configurer {
        return DefaultConfigurer.defaultConfiguration()
            .configureAggregate(
                AggregateConfigurer.defaultConfiguration(User::class.java)
                    .configureRepository { config ->
                        EventSourcingRepository.builder(User::class.java)
                            .eventStore(config.eventStore())
                            .build()
                    }
            )
            .configureCorrelationDataProviders { correlationProviders }
    }
}
```

### Phase 4: Event Propagation Bridge (Week 4-5)

#### 4.1 NATS Event Publisher (Transactional Outbox Pattern)

```kotlin
@Component
@ProcessingGroup("nats-event-publisher")
class NatsEventRelayProcessor(
    private val natsEventPublisher: NatsEventPublisher
) {
    @EventHandler
    suspend fun on(event: Any, @MetaData("tenant_id") tenantId: String) {
        // Event store acts as transactional outbox
        natsEventPublisher.publish(
            subject = "events.${event.javaClass.simpleName}",
            tenantId = tenantId,
            event = event
        )
    }
}
```

#### 4.2 Configure Reliable Processing

```kotlin
@Bean
fun natsEventProcessorConfiguration(): EventProcessingConfigurer {
    return { configurer ->
        configurer.registerTrackingEventProcessor("nats-event-publisher") { config ->
            TrackingEventProcessorConfiguration
                .forSingleThreadedProcessing()
                .andInitialTrackingToken { StreamableMessageSource.createHeadToken() }
        }
    }
}
```

### Phase 5: Migration Execution (Week 5-6)

#### 5.1 Parallel Running Strategy

1. Deploy new event-driven audit system alongside existing
2. Use feature flags to control rollout:
   ```kotlin
   @ConditionalOnProperty("eaf.audit.event-driven.enabled")
   ```
3. Monitor both systems for consistency
4. Gradually migrate tenants

#### 5.2 Cleanup Tasks

- Remove `AuditService` and `AuditRepository`
- Remove `@Lazy` annotations
- Archive old audit tables (keep for compliance)

## Risk Mitigation

### Technical Risks

| Risk                    | Mitigation Strategy                 |
| ----------------------- | ----------------------------------- |
| Event Store Performance | Pre-test with production-like load  |
| Schema Migration Errors | Blue-green deployment with rollback |
| Lost Audit Events       | Dual-write during transition period |
| Team Knowledge Gap      | Axon training sessions week 1       |

### Monitoring & Observability

```kotlin
@Component
class AxonMetricsConfiguration {
    @Bean
    fun metricsConfigurer(meterRegistry: MeterRegistry): ConfigurerModule {
        return MetricsConfigurer(meterRegistry)
    }
}
```

Key metrics to monitor:

- Event processing lag
- Event store write latency
- NATS publishing success rate
- Tenant isolation violations

## Success Criteria

### Week 6 Checkpoint

- [ ] Zero circular dependency exceptions
- [ ] 100% audit event capture rate
- [ ] < 10ms event processing latency (p99)
- [ ] Successful processing of 1M+ events
- [ ] All integration tests passing

### Performance Targets

- Request latency: 150ms → 25ms (6x improvement)
- Throughput: 60 RPS → 400 RPS (6.7x improvement)
- Startup time: < 30 seconds

## Rollback Plan

If critical issues arise:

1. Feature flag disables event-driven audit
2. Revert to synchronous `AuditService`
3. Events remain in event store for later processing
4. No data loss guaranteed

## Next Steps

1. **Week 0**: Team training on Axon Framework v5
2. **Week 1**: Implement EventStorageEngine adapter
3. **Week 2**: Deploy to development environment
4. **Week 3**: Begin pilot with single tenant
5. **Week 6**: Full production rollout

## Appendix: Technology Versions

- Axon Framework: 5.0.0
- Spring Boot: 3.2.x
- Kotlin: 1.9.x
- PostgreSQL: 15.x
- NATS: 2.10.x

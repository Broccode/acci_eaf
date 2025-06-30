---
sidebar_position: 9
title: Performance & Operations
---

# Performance & Operations

This module covers production deployment, performance optimization, monitoring, and operational best
practices for Axon Framework in the EAF platform.

## 📚 Learning Objectives

By the end of this module, you will be able to:

- Optimize Axon Framework performance for production workloads
- Implement comprehensive monitoring and alerting
- Configure proper deployment strategies
- Handle operational challenges and debugging
- Scale Axon applications effectively

## ⚡ Performance Optimization

### Event Store Performance

```kotlin
@Configuration
class EafEventStorePerformanceConfiguration {

    @Bean
    fun eventStorageEngine(
        dataSource: DataSource,
        serializer: Serializer
    ): EventStorageEngine {
        return EafPostgresEventStorageEngine(
            ConnectionProvider.from(dataSource),
            TransactionManager.from(dataSource)
        ).apply {
            // Optimize batch sizes for high-throughput scenarios
            setBatchSize(100) // Larger batches for better performance

            // Enable read-only optimization for projections
            setReadOnlyOptimization(true)

            // Configure connection pooling
            setMaxConcurrentConnections(20)

            // Enable compression for large events
            setEventSerializer(CompressingSerializer(serializer))
        }
    }

    @Bean
    fun eventProcessingConfiguration(): EventProcessingConfigurer {
        return { config ->
            // High-throughput projection processing
            config.registerTrackingEventProcessor("user-projections") {
                TrackingEventProcessorConfiguration
                    .forParallelProcessing(4) // Parallel threads
                    .andBatchSize(50) // Optimal batch size
                    .andInitialTrackingToken { it.eventStore().createHeadToken() }
                    .andEventAvailabilityTimeout(Duration.ofSeconds(1))
            }

            // Analytics processing with larger batches
            config.registerTrackingEventProcessor("analytics") {
                TrackingEventProcessorConfiguration
                    .forSingleThreadedProcessing()
                    .andBatchSize(200) // Large batches for analytics
                    .andInitialTrackingToken { it.eventStore().createHeadToken() }
            }

            // Configure error handling
            config.registerListenerInvocationErrorHandler("user-projections") { _, exception ->
                when (exception) {
                    is TransientException -> PropagatingErrorHandler.instance()
                    else -> LoggingErrorHandler.builder().build()
                }
            }
        }
    }
}
```

### Snapshot Configuration

```kotlin
@Configuration
class SnapshotConfiguration {

    @Bean
    fun snapshotTriggerDefinition(): SnapshotTriggerDefinition {
        return EventCountSnapshotTriggerDefinition(
            snapshotter = snapshotter(),
            threshold = 50 // Snapshot every 50 events
        )
    }

    @Bean
    fun snapshotter(
        eventStore: EventStore,
        snapshotRepository: SnapshotEventStore
    ): Snapshotter {
        return AggregateSnapshotter.builder()
            .eventStore(eventStore)
            .snapshotStore(snapshotRepository)
            .build()
    }

    @Bean
    fun snapshotScheduler(snapshotter: Snapshotter): SnapshotScheduler {
        return SnapshotScheduler.builder()
            .snapshotter(snapshotter)
            .scheduleImmediately(false) // Don't block command processing
            .build()
    }
}

// Optimized aggregate for snapshotting
@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
class OptimizedUserAggregate {

    @AggregateIdentifier
    private lateinit var userId: String

    private lateinit var profile: UserProfile
    private lateinit var roles: MutableSet<String>
    private lateinit var permissions: MutableSet<String>
    private var isActive: Boolean = true
    private var lastModified: Instant = Instant.now()

    // Snapshot optimization - only include essential state
    data class UserSnapshot(
        val userId: String,
        val profile: UserProfile,
        val roles: Set<String>,
        val permissions: Set<String>,
        val isActive: Boolean,
        val lastModified: Instant,
        val version: Long
    )

    @CommandHandler
    constructor(command: CreateUserCommand) {
        AggregateLifecycle.apply(
            UserCreatedEvent(
                userId = command.userId,
                email = command.email,
                profile = command.profile
            )
        )
    }

    // Snapshot handling
    @EventSourcingHandler
    fun on(snapshot: UserSnapshot) {
        this.userId = snapshot.userId
        this.profile = snapshot.profile
        this.roles = snapshot.roles.toMutableSet()
        this.permissions = snapshot.permissions.toMutableSet()
        this.isActive = snapshot.isActive
        this.lastModified = snapshot.lastModified
    }

    fun createSnapshot(): UserSnapshot {
        return UserSnapshot(
            userId = userId,
            profile = profile,
            roles = roles.toSet(),
            permissions = permissions.toSet(),
            isActive = isActive,
            lastModified = lastModified,
            version = AggregateLifecycle.getVersion()
        )
    }
}
```

### Caching Strategy

```kotlin
@Configuration
@EnableCaching
class AxonCacheConfiguration {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(10000) // Max cache entries
                .expireAfterWrite(Duration.ofMinutes(30)) // TTL
                .recordStats() // Enable metrics
        )
        return cacheManager
    }

    @Bean
    fun cachingEventSourcingRepository<T>(
        eventStore: EventStore,
        aggregateFactory: AggregateFactory<T>,
        cacheManager: CacheManager
    ): Repository<T> where T : Any {

        return CachingEventSourcingRepository.builder(aggregateFactory)
            .eventStore(eventStore)
            .cache(SpringCache(cacheManager.getCache("aggregates")))
            .snapshotTriggerDefinition(EventCountSnapshotTriggerDefinition(50))
            .build()
    }
}

// Cache-aware query handler
@Component
class CachedUserQueryHandler(
    private val userProjectionRepository: UserProjectionRepository,
    private val cacheManager: CacheManager
) {

    @QueryHandler
    @Cacheable(value = ["user-projections"], key = "#query.userId")
    fun handle(query: FindUserByIdQuery): UserProjection? {
        return userProjectionRepository.findById(query.userId).orElse(null)
    }

    @QueryHandler
    @Cacheable(value = ["user-searches"], key = "#query.hashCode()")
    fun handle(query: SearchUsersQuery): Page<UserProjection> {
        // Expensive search operation - cache results
        return userProjectionRepository.findBySearchCriteria(
            searchTerm = query.searchTerm,
            filters = query.filters,
            pageable = PageRequest.of(query.page, query.size)
        )
    }

    @EventHandler
    @CacheEvict(value = ["user-projections"], key = "#event.userId")
    fun on(event: UserUpdatedEvent) {
        // Invalidate user cache when updated
        cacheManager.getCache("user-searches")?.clear() // Clear search cache too
    }
}
```

## 📊 Comprehensive Monitoring

### Metrics Collection

```kotlin
@Configuration
class AxonMetricsConfiguration {

    @Bean
    fun messageMonitor(meterRegistry: MeterRegistry): MessageMonitor {
        return MessageCountingMonitor.buildMonitor()
            .withMessageMonitor(MessageTimerMonitor.buildMonitor())
            .withMessageMonitor(CapacityMonitor.buildMonitor(meterRegistry))
            .build()
    }

    @Bean
    fun eventProcessorMonitor(meterRegistry: MeterRegistry): MessageMonitor {
        return EventProcessorLatencyMonitor.builder()
            .meterRegistry(meterRegistry)
            .build()
    }
}

@Component
class CustomAxonMetrics(
    private val meterRegistry: MeterRegistry
) {

    private val commandCounter = Counter.builder("axon.commands.total")
        .description("Total number of commands processed")
        .register(meterRegistry)

    private val commandTimer = Timer.builder("axon.commands.duration")
        .description("Command processing duration")
        .register(meterRegistry)

    private val eventCounter = Counter.builder("axon.events.total")
        .description("Total number of events published")
        .register(meterRegistry)

    private val sagaCounter = Counter.builder("axon.sagas.total")
        .description("Total number of active sagas")
        .register(meterRegistry)

    private val projectionLag = Gauge.builder("axon.projections.lag")
        .description("Projection processing lag in milliseconds")
        .register(meterRegistry) { getCurrentProjectionLag() }

    @EventHandler
    fun recordCommand(command: Any, @MetaData metadata: MetaData) {
        val timer = Timer.start(meterRegistry)

        commandCounter.increment(
            Tags.of(
                Tag.of("command_type", command::class.simpleName!!),
                Tag.of("tenant_id", metadata["tenant_id"] as? String ?: "unknown")
            )
        )

        timer.stop(commandTimer.tag("command_type", command::class.simpleName!!))
    }

    @EventHandler
    fun recordEvent(event: Any, @MetaData metadata: MetaData) {
        eventCounter.increment(
            Tags.of(
                Tag.of("event_type", event::class.simpleName!!),
                Tag.of("tenant_id", metadata["tenant_id"] as? String ?: "unknown")
            )
        )
    }

    private fun getCurrentProjectionLag(): Double {
        // Calculate lag between latest event and projection update
        // Implementation depends on your tracking mechanism
        return 0.0 // Placeholder
    }
}
```

### Health Checks

```kotlin
@Component
class AxonHealthIndicator(
    private val eventProcessingConfiguration: EventProcessingConfiguration,
    private val eventStore: EventStore,
    private val commandBus: CommandBus
) : HealthIndicator {

    override fun health(): Health {
        val builder = Health.Builder()

        try {
            // Check event processors
            checkEventProcessors(builder)

            // Check event store
            checkEventStore(builder)

            // Check command bus
            checkCommandBus(builder)

            // Check saga repositories
            checkSagaRepositories(builder)

        } catch (e: Exception) {
            builder.down().withException(e)
        }

        return builder.build()
    }

    private fun checkEventProcessors(builder: Health.Builder) {
        val processors = eventProcessingConfiguration.eventProcessors()
        val runningCount = processors.count { it.value.isRunning }
        val totalCount = processors.size

        if (runningCount < totalCount) {
            builder.down()
            val stoppedProcessors = processors
                .filter { !it.value.isRunning }
                .map { "${it.key}: ${it.value.processingStatus()}" }

            builder.withDetail("stopped_processors", stoppedProcessors)
        }

        builder.withDetail("event_processors", mapOf(
            "total" to totalCount,
            "running" to runningCount,
            "stopped" to (totalCount - runningCount)
        ))

        // Check processor lag
        processors.forEach { (name, processor) ->
            if (processor is TrackingEventProcessor) {
                val progress = processor.processingStatus()
                builder.withDetail("${name}_lag", progress.toString())
            }
        }
    }

    private fun checkEventStore(builder: Health.Builder) {
        try {
            // Try to create a head token to verify connectivity
            eventStore.createHeadToken()
            builder.withDetail("event_store", "UP")
        } catch (e: Exception) {
            builder.down().withDetail("event_store", "DOWN: ${e.message}")
        }
    }

    private fun checkCommandBus(builder: Health.Builder) {
        try {
            // Send a simple health check command
            val healthCommand = HealthCheckCommand()
            commandBus.dispatch(GenericCommandMessage.asCommandMessage<Any>(healthCommand))
            builder.withDetail("command_bus", "UP")
        } catch (e: Exception) {
            builder.down().withDetail("command_bus", "DOWN: ${e.message}")
        }
    }
}

class HealthCheckCommand
```

### Alerting Configuration

```kotlin
@Configuration
class AlertingConfiguration {

    @Bean
    fun alertingManager(): AlertingManager {
        return AlertingManager.builder()
            .addAlert(EventProcessorStoppedAlert())
            .addAlert(HighProjectionLagAlert())
            .addAlert(CommandFailureRateAlert())
            .addAlert(SagaTimeoutAlert())
            .build()
    }
}

class EventProcessorStoppedAlert : Alert {

    @EventListener
    fun on(event: EventProcessorStatusChangedEvent) {
        if (event.status == EventProcessorStatus.STOPPED) {
            alertingService.sendAlert(
                severity = AlertSeverity.CRITICAL,
                title = "Event Processor Stopped",
                message = "Event processor ${event.processorName} has stopped",
                runbook = "https://docs.eaf.com/runbooks/event-processor-stopped",
                tags = mapOf(
                    "processor" to event.processorName,
                    "tenant" to event.tenantId
                )
            )
        }
    }
}

class HighProjectionLagAlert : Alert {

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    fun checkProjectionLag() {
        val lagThreshold = Duration.ofMinutes(5)

        projectionMonitors.forEach { (name, monitor) ->
            val lag = monitor.getCurrentLag()

            if (lag > lagThreshold) {
                alertingService.sendAlert(
                    severity = AlertSeverity.WARNING,
                    title = "High Projection Lag",
                    message = "Projection $name has lag of ${lag.toMinutes()} minutes",
                    runbook = "https://docs.eaf.com/runbooks/high-projection-lag",
                    tags = mapOf(
                        "projection" to name,
                        "lag_minutes" to lag.toMinutes().toString()
                    )
                )
            }
        }
    }
}
```

## 🚀 Deployment Strategies

### Production Configuration

```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

axon:
  eventhandling:
    processors:
      user-projections:
        mode: tracking
        source: eventStore
        thread-count: 4
        batch-size: 50
        initial-tracking-token: head
        token-store-claim-timeout: 10s
        event-availability-timeout: 1s

      analytics:
        mode: tracking
        source: eventStore
        thread-count: 1
        batch-size: 200
        initial-tracking-token: head

  serializer:
    events: jackson
    messages: jackson
    general: jackson

  metrics:
    auto-configuration:
      enabled: true
    dimensional-metrics-enabled: true

  snapshot:
    trigger:
      threshold: 50
    archiving:
      enabled: true
      max-age: 30d

logging:
  level:
    org.axonframework: INFO
    com.axians.eaf: DEBUG
    org.springframework.security: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  metrics:
    export:
      prometheus:
        enabled: true
  health:
    show-details: always
```

### Docker Configuration

```dockerfile
# Dockerfile for production
FROM openjdk:17-jre-slim

# Performance optimizations
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# JVM monitoring
ENV JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/var/log/gc.log"

# JMX for monitoring
ENV JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9999"

COPY target/app.jar /app/app.jar

WORKDIR /app

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080 9999

CMD ["java", "-jar", "app.jar"]
```

### Kubernetes Deployment

```yaml
# k8s-deployment.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eaf-user-service
  labels:
    app: eaf-user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: eaf-user-service
  template:
    metadata:
      labels:
        app: eaf-user-service
    spec:
      containers:
        - name: user-service
          image: eaf/user-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: 'prod,k8s'
            - name: DB_HOST
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: host
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          resources:
            requests:
              memory: '1Gi'
              cpu: '500m'
            limits:
              memory: '2Gi'
              cpu: '1000m'
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
            timeoutSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

---
apiVersion: v1
kind: Service
metadata:
  name: eaf-user-service
spec:
  selector:
    app: eaf-user-service
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: ClusterIP
```

## 🔧 Operational Procedures

### Event Replay Operations

```kotlin
@RestController
@RequestMapping("/operations/events")
class EventOperationsController(
    private val eventReplayService: EventReplayService,
    private val eventProcessingConfiguration: EventProcessingConfiguration
) {

    @PostMapping("/replay")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun replayEvents(
        @RequestParam tenantId: String,
        @RequestParam fromSequenceId: Long,
        @RequestParam(required = false) toSequenceId: Long?,
        @RequestParam(required = false) eventTypes: Set<String> = emptySet()
    ): ReplayResult {

        logger.info(
            "Starting event replay for tenant {} from {} to {}",
            tenantId,
            fromSequenceId,
            toSequenceId ?: "latest"
        )

        return eventReplayService.replayEventsForTenant(
            tenantId = tenantId,
            fromSequenceId = fromSequenceId,
            toSequenceId = toSequenceId,
            eventTypes = eventTypes
        )
    }

    @PostMapping("/processors/{processorName}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    fun resetEventProcessor(@PathVariable processorName: String): ResponseEntity<String> {
        val processor = eventProcessingConfiguration.eventProcessor(processorName)
            .orElseThrow { IllegalArgumentException("Processor not found: $processorName") }

        if (processor is TrackingEventProcessor) {
            processor.shutDown()
            processor.resetTokens()
            processor.start()

            return ResponseEntity.ok("Processor $processorName reset successfully")
        } else {
            return ResponseEntity.badRequest()
                .body("Processor $processorName is not a tracking processor")
        }
    }

    @GetMapping("/processors/status")
    fun getProcessorStatus(): Map<String, Any> {
        return eventProcessingConfiguration.eventProcessors()
            .mapValues { (_, processor) ->
                mapOf(
                    "running" to processor.isRunning,
                    "status" to processor.processingStatus(),
                    "type" to processor::class.simpleName
                )
            }
    }
}
```

### Performance Tuning Scripts

```bash
#!/bin/bash
# performance-tune.sh

echo "Starting Axon Framework performance tuning..."

# Database optimizations
echo "Optimizing database configuration..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
  -- Increase work memory for complex queries
  SET work_mem = '256MB';

  -- Optimize for event store queries
  SET random_page_cost = 1.1;

  -- Increase checkpoint segments
  SET checkpoint_segments = 32;

  -- Optimize shared buffers
  SET shared_buffers = '25% of RAM';

  -- Create indexes for event store
  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_events_aggregate_sequence
  ON domain_event_entry (aggregate_identifier, sequence_number);

  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_events_timestamp
  ON domain_event_entry (time_stamp);

  CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_events_tenant
  ON domain_event_entry ((meta_data->>'tenant_id'));

  -- Analyze tables
  ANALYZE domain_event_entry;
  ANALYZE snapshot_event_entry;
  ANALYZE tracking_token_entry;
"

# JVM optimizations
echo "Optimizing JVM settings..."
export JAVA_OPTS="
  -Xmx4g
  -Xms2g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:+UseLargePages
  -XX:ParallelGCThreads=8
  -XX:ConcGCThreads=2
"

# Connection pool optimization
echo "Optimizing connection pool..."
curl -X POST $APP_URL/actuator/configprops -H "Content-Type: application/json" -d '{
  "spring.datasource.hikari.maximum-pool-size": 50,
  "spring.datasource.hikari.minimum-idle": 10,
  "spring.datasource.hikari.connection-timeout": 30000,
  "spring.datasource.hikari.idle-timeout": 600000,
  "spring.datasource.hikari.max-lifetime": 1800000,
  "spring.datasource.hikari.leak-detection-threshold": 60000
}'

echo "Performance tuning completed!"
```

### Monitoring Dashboard

```kotlin
@RestController
@RequestMapping("/dashboard")
class MonitoringDashboardController(
    private val meterRegistry: MeterRegistry,
    private val eventProcessingConfiguration: EventProcessingConfiguration
) {

    @GetMapping("/metrics")
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "commands" to getCommandMetrics(),
            "events" to getEventMetrics(),
            "projections" to getProjectionMetrics(),
            "sagas" to getSagaMetrics(),
            "system" to getSystemMetrics()
        )
    }

    private fun getCommandMetrics(): Map<String, Any> {
        val commandCounter = meterRegistry.get("axon.commands.total").counter()
        val commandTimer = meterRegistry.get("axon.commands.duration").timer()

        return mapOf(
            "total_count" to commandCounter.count(),
            "average_duration_ms" to commandTimer.mean(TimeUnit.MILLISECONDS),
            "max_duration_ms" to commandTimer.max(TimeUnit.MILLISECONDS),
            "rate_per_second" to commandCounter.count() / commandTimer.totalTime(TimeUnit.SECONDS)
        )
    }

    private fun getEventMetrics(): Map<String, Any> {
        val eventCounter = meterRegistry.get("axon.events.total").counter()

        return mapOf(
            "total_count" to eventCounter.count(),
            "rate_per_second" to eventCounter.count() / System.currentTimeMillis() * 1000
        )
    }

    private fun getProjectionMetrics(): Map<String, Any> {
        val projectionTimer = meterRegistry.get("axon.projections.processing.duration").timer()
        val projectionLag = meterRegistry.get("axon.projections.lag").gauge()

        return mapOf(
            "average_processing_time_ms" to projectionTimer.mean(TimeUnit.MILLISECONDS),
            "current_lag_ms" to projectionLag.value(),
            "total_events_processed" to projectionTimer.count()
        )
    }

    @GetMapping("/health/detailed")
    fun getDetailedHealth(): Map<String, Any> {
        return mapOf(
            "event_processors" to getEventProcessorHealth(),
            "database" to getDatabaseHealth(),
            "memory" to getMemoryHealth(),
            "threads" to getThreadHealth()
        )
    }

    private fun getEventProcessorHealth(): Map<String, Any> {
        return eventProcessingConfiguration.eventProcessors()
            .mapValues { (_, processor) ->
                mapOf(
                    "running" to processor.isRunning,
                    "processing_status" to processor.processingStatus().toString(),
                    "active_threads" to getActiveThreadCount(processor)
                )
            }
    }
}
```

## 🎯 Best Practices Summary

### ✅ Performance Do's

- Configure appropriate batch sizes for your workload
- Use snapshots for large aggregates
- Implement proper caching strategies
- Monitor and tune database performance
- Use parallel processing where appropriate

### ✅ Operations Do's

- Implement comprehensive monitoring
- Set up proper alerting
- Have runbooks for common issues
- Practice incident response procedures
- Monitor business metrics, not just technical ones

### ❌ Common Mistakes

- Don't ignore projection lag
- Don't skip capacity planning
- Don't deploy without proper monitoring
- Don't forget to test disaster recovery
- Don't ignore memory and thread management

## 🚀 Next Steps

You've mastered Axon Framework operations! Complete your learning with:

**Next Module:** [FAQ & Troubleshooting](./faq.md) →

**Production Checklist:**

- [ ] Performance testing completed
- [ ] Monitoring dashboards configured
- [ ] Alerting rules defined
- [ ] Deployment pipeline ready
- [ ] Runbooks documented

---

💡 **Production Reality:** Monitoring and observability are not optional - they're essential for
running Axon Framework successfully in production!

---
sidebar_position: 6
title: EAF-Specific Patterns
---

# EAF-Specific Patterns

This module covers advanced patterns specific to the EAF platform, including multi-tenancy
strategies, security context handling, and NATS integration patterns.

## 📚 Learning Objectives

By the end of this module, you will be able to:

- Implement advanced multi-tenant patterns with Axon
- Handle security context propagation correctly
- Design NATS integration for reliable event distribution
- Use EAF SDKs effectively with Axon Framework
- Implement cross-service communication patterns

## 🏢 Advanced Multi-Tenancy Patterns

### Tenant-Aware Command Gateway

```kotlin
@Component
class EafTenantAwareCommandGateway(
    private val commandGateway: CommandGateway,
    private val tenantContextHolder: TenantContextHolder,
    private val securityContextAccessor: SecurityContextAccessor
) {

    private val logger = LoggerFactory.getLogger(EafTenantAwareCommandGateway::class.java)

    fun <R> send(command: Any): CompletableFuture<R> {
        val tenantId = tenantContextHolder.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context available")

        val securityContext = securityContextAccessor.getCurrentSecurityContext()
        val userId = securityContext?.userId
            ?: throw IllegalStateException("No authenticated user")

        // Validate tenant access
        validateTenantAccess(tenantId, userId)

        // Create enhanced metadata
        val metadata = MetaData.with(mapOf(
            "tenant_id" to tenantId,
            "user_id" to userId,
            "correlation_id" to UUID.randomUUID().toString(),
            "timestamp" to Instant.now().toString(),
            "ip_address" to securityContext.ipAddress,
            "user_agent" to securityContext.userAgent
        ))

        logger.debug(
            "Sending command {} for tenant {} by user {}",
            command::class.simpleName,
            tenantId,
            userId
        )

        return commandGateway.send(command, metadata)
    }

    fun <R> sendAndWait(command: Any): R {
        return send<R>(command).get()
    }

    fun <R> sendAndWait(command: Any, timeout: Duration): R {
        return send<R>(command).get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    private fun validateTenantAccess(tenantId: String, userId: String) {
        // Integration with IAM service to validate tenant access
        val hasAccess = iamClientService.hasUserAccessToTenant(userId, tenantId)
        if (!hasAccess) {
            throw TenantAccessDeniedException("User $userId does not have access to tenant $tenantId")
        }
    }
}
```

### Tenant-Isolated Event Processing

```kotlin
@Component
@ProcessingGroup("tenant-isolated-projections")
class TenantIsolatedProjectionHandler(
    private val userProjectionRepository: UserProjectionRepository,
    private val tenantMetricsService: TenantMetricsService
) {

    @EventHandler
    fun on(
        event: UserCreatedEvent,
        @MetaData("tenant_id") tenantId: String,
        @MetaData("user_id") actingUserId: String
    ) {
        // Validate tenant isolation
        require(event.tenantId == tenantId) {
            "Event tenant ID (${event.tenantId}) does not match metadata tenant ID ($tenantId)"
        }

        // Create projection with explicit tenant isolation
        val projection = UserProjection(
            userId = event.userId,
            tenantId = tenantId, // Always use metadata tenant ID
            email = event.email,
            firstName = event.firstName,
            lastName = event.lastName,
            // ... other fields
        )

        // Save with tenant validation
        userProjectionRepository.saveWithTenantValidation(projection, tenantId)

        // Update tenant metrics
        tenantMetricsService.incrementUserCount(tenantId)

        // Log with tenant context
        logger.info(
            "Created user projection for user {} in tenant {} by user {}",
            event.userId,
            tenantId,
            actingUserId
        )
    }
}

// Repository with tenant validation
interface TenantValidatedUserProjectionRepository : UserProjectionRepository {

    fun saveWithTenantValidation(projection: UserProjection, expectedTenantId: String): UserProjection {
        require(projection.tenantId == expectedTenantId) {
            "Projection tenant ID does not match expected tenant ID"
        }
        return save(projection)
    }

    fun findByIdWithTenantValidation(id: String, tenantId: String): UserProjection? {
        return findById(id).takeIf { it?.tenantId == tenantId }
    }
}
```

### Cross-Tenant Data Protection

```kotlin
@Component
class TenantDataProtectionAspect {

    @Around("@annotation(RequiresTenantIsolation)")
    fun enforceTenantIsolation(joinPoint: ProceedingJoinPoint): Any? {
        val currentTenantId = tenantContextHolder.getCurrentTenantId()
            ?: throw IllegalStateException("No tenant context")

        // Validate all arguments that implement TenantAware
        joinPoint.args.forEach { arg ->
            if (arg is TenantAware && arg.tenantId != currentTenantId) {
                throw TenantIsolationViolationException(
                    "Cross-tenant access attempt: expected $currentTenantId, got ${arg.tenantId}"
                )
            }
        }

        return joinPoint.proceed()
    }
}

interface TenantAware {
    val tenantId: String
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresTenantIsolation
```

## 🔒 Security Context Integration

### Security-Aware Command Handlers

```kotlin
@Component
class SecureUserCommandHandler(
    private val repository: Repository<User>,
    private val securityContextAccessor: SecurityContextAccessor,
    private val authorizationService: AuthorizationService
) {

    @CommandHandler
    @RequiresTenantIsolation
    fun handle(
        command: CreateUserCommand,
        @MetaData("tenant_id") tenantId: String,
        @MetaData("user_id") actingUserId: String
    ) {
        // Validate security context
        val securityContext = securityContextAccessor.getCurrentSecurityContext()
            ?: throw SecurityException("No security context available")

        // Check permissions
        authorizationService.requirePermission(
            userId = actingUserId,
            tenantId = tenantId,
            permission = "USER_CREATE",
            resourceContext = mapOf("department" to command.department)
        )

        // Audit the command
        auditService.logCommand(
            command = command,
            actingUserId = actingUserId,
            tenantId = tenantId,
            ipAddress = securityContext.ipAddress,
            userAgent = securityContext.userAgent
        )

        try {
            // Execute command
            val user = User(command)
            repository.save(user)

            logger.info(
                "User {} created successfully by {} in tenant {}",
                command.userId,
                actingUserId,
                tenantId
            )

        } catch (e: Exception) {
            // Log security-relevant failures
            auditService.logCommandFailure(
                command = command,
                actingUserId = actingUserId,
                tenantId = tenantId,
                error = e.message,
                securityContext = securityContext
            )
            throw e
        }
    }
}
```

### Role-Based Event Filtering

```kotlin
@Component
@ProcessingGroup("role-based-notifications")
class RoleBasedNotificationHandler(
    private val notificationService: NotificationService,
    private val userProjectionRepository: UserProjectionRepository
) {

    @EventHandler
    fun on(
        event: UserSuspendedEvent,
        @MetaData("tenant_id") tenantId: String
    ) {
        // Find users who should be notified based on roles
        val adminUsers = userProjectionRepository
            .findByTenantIdAndRole(tenantId, "ADMIN")

        val managerUsers = userProjectionRepository
            .findByTenantIdAndRole(tenantId, "MANAGER")

        // Create targeted notifications
        val notifications = (adminUsers + managerUsers).distinct().map { user ->
            UserSuspensionNotification(
                recipientUserId = user.userId,
                suspendedUserId = event.userId,
                reason = event.reason,
                suspendedBy = event.suspendedBy,
                tenantId = tenantId,
                notificationLevel = when {
                    "ADMIN" in user.roles -> NotificationLevel.HIGH
                    "MANAGER" in user.roles -> NotificationLevel.MEDIUM
                    else -> NotificationLevel.LOW
                }
            )
        }

        // Send notifications asynchronously
        notifications.forEach { notification ->
            notificationService.sendAsync(notification)
        }
    }
}
```

## 📡 Advanced NATS Integration

### Reliable Event Publishing

```kotlin
@Component
@ProcessingGroup("nats-reliable-publisher")
class ReliableNatsEventPublisher(
    private val natsEventPublisher: NatsEventPublisher,
    private val eventPublishingMetricsService: EventPublishingMetricsService,
    private val deadLetterQueueService: DeadLetterQueueService
) {

    private val logger = LoggerFactory.getLogger(ReliableNatsEventPublisher::class.java)

    @EventHandler
    suspend fun on(
        event: Any,
        @MetaData("tenant_id") tenantId: String,
        @MetaData("correlation_id") correlationId: String,
        @MetaData("global_sequence_id") sequenceId: Long
    ) {
        val eventType = event::class.simpleName!!
        val subject = "events.$eventType"

        val startTime = System.currentTimeMillis()
        var attempt = 0
        val maxAttempts = 3

        while (attempt < maxAttempts) {
            try {
                attempt++

                val publishResult = natsEventPublisher.publish(
                    subject = subject,
                    tenantId = tenantId,
                    event = event,
                    metadata = mapOf(
                        "correlation_id" to correlationId,
                        "sequence_id" to sequenceId.toString(),
                        "attempt" to attempt.toString(),
                        "max_attempts" to maxAttempts.toString()
                    )
                )

                // Record successful publish
                eventPublishingMetricsService.recordSuccess(
                    eventType = eventType,
                    tenantId = tenantId,
                    duration = System.currentTimeMillis() - startTime,
                    attempt = attempt
                )

                logger.debug(
                    "Successfully published {} to {} for tenant {} (attempt {})",
                    eventType,
                    subject,
                    tenantId,
                    attempt
                )

                return // Success!

            } catch (e: Exception) {
                logger.warn(
                    "Failed to publish {} to {} for tenant {} (attempt {}/{}): {}",
                    eventType,
                    subject,
                    tenantId,
                    attempt,
                    maxAttempts,
                    e.message
                )

                if (attempt >= maxAttempts) {
                    // Record failure and send to DLQ
                    eventPublishingMetricsService.recordFailure(
                        eventType = eventType,
                        tenantId = tenantId,
                        error = e.message ?: "Unknown error",
                        finalAttempt = attempt
                    )

                    deadLetterQueueService.sendToDeadLetter(
                        event = event,
                        subject = subject,
                        tenantId = tenantId,
                        correlationId = correlationId,
                        sequenceId = sequenceId,
                        error = e,
                        attempts = attempt
                    )

                    // Don't rethrow - let event processing continue
                    return
                }

                // Exponential backoff
                delay(1000L * attempt)
            }
        }
    }
}
```

### Event Type Routing

```kotlin
@Component
@ProcessingGroup("nats-event-router")
class NatsEventRouter(
    private val natsEventPublisher: NatsEventPublisher
) {

    @EventHandler
    suspend fun on(event: UserCreatedEvent, @MetaData("tenant_id") tenantId: String) {
        // Route to multiple subjects based on event content
        val routingTargets = determineRoutingTargets(event, tenantId)

        routingTargets.forEach { target ->
            natsEventPublisher.publish(
                subject = target.subject,
                tenantId = tenantId,
                event = event,
                metadata = target.metadata
            )
        }
    }

    @EventHandler
    suspend fun on(event: UserSuspendedEvent, @MetaData("tenant_id") tenantId: String) {
        // High-priority security event
        natsEventPublisher.publish(
            subject = "security.user.suspended",
            tenantId = tenantId,
            event = event,
            metadata = mapOf(
                "priority" to "HIGH",
                "category" to "SECURITY",
                "requires_immediate_attention" to "true"
            )
        )

        // Also send to general user events
        natsEventPublisher.publish(
            subject = "events.UserSuspended",
            tenantId = tenantId,
            event = event
        )
    }

    private fun determineRoutingTargets(
        event: UserCreatedEvent,
        tenantId: String
    ): List<RoutingTarget> {
        val targets = mutableListOf<RoutingTarget>()

        // General user events
        targets.add(RoutingTarget(
            subject = "events.UserCreated",
            metadata = emptyMap()
        ))

        // Department-specific routing
        event.department?.let { department ->
            targets.add(RoutingTarget(
                subject = "department.$department.user.created",
                metadata = mapOf("department" to department)
            ))
        }

        // Role-based routing
        if (event.roles.contains("ADMIN")) {
            targets.add(RoutingTarget(
                subject = "admin.user.created",
                metadata = mapOf("admin_alert" to "true")
            ))
        }

        return targets
    }
}

data class RoutingTarget(
    val subject: String,
    val metadata: Map<String, Any> = emptyMap()
)
```

### Event Replay and Recovery

```kotlin
@Component
class EventReplayService(
    private val eventStore: EventStore,
    private val natsEventPublisher: NatsEventPublisher,
    private val tenantContextHolder: TenantContextHolder
) {

    suspend fun replayEventsForTenant(
        tenantId: String,
        fromSequenceId: Long,
        toSequenceId: Long? = null,
        eventTypes: Set<String> = emptySet()
    ): ReplayResult {

        tenantContextHolder.setCurrentTenantId(tenantId)

        try {
            val startTime = System.currentTimeMillis()
            var processedCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()

            // Create tracking token for replay
            val startToken = GlobalSequenceTrackingToken(fromSequenceId)
            val endToken = toSequenceId?.let { GlobalSequenceTrackingToken(it) }

            // Stream events
            val eventStream = if (endToken != null) {
                eventStore.readEvents(startToken, endToken)
            } else {
                eventStore.readEvents(startToken)
            }

            eventStream.forEach { eventMessage ->
                try {
                    val eventType = eventMessage.payloadType.simpleName

                    // Filter by event types if specified
                    if (eventTypes.isNotEmpty() && eventType !in eventTypes) {
                        return@forEach
                    }

                    // Extract tenant from metadata
                    val eventTenantId = eventMessage.metaData["tenant_id"] as? String
                    if (eventTenantId != tenantId) {
                        return@forEach // Skip events from other tenants
                    }

                    // Republish to NATS
                    natsEventPublisher.publish(
                        subject = "replay.events.$eventType",
                        tenantId = tenantId,
                        event = eventMessage.payload,
                        metadata = mapOf(
                            "replay" to "true",
                            "original_sequence_id" to eventMessage.sequenceNumber.toString(),
                            "replay_timestamp" to Instant.now().toString()
                        )
                    )

                    processedCount++

                } catch (e: Exception) {
                    errorCount++
                    errors.add("Event ${eventMessage.identifier}: ${e.message}")
                    logger.error("Failed to replay event {}: {}", eventMessage.identifier, e.message, e)
                }
            }

            val duration = System.currentTimeMillis() - startTime

            return ReplayResult(
                tenantId = tenantId,
                processedCount = processedCount,
                errorCount = errorCount,
                errors = errors,
                durationMs = duration,
                success = errorCount == 0
            )

        } finally {
            tenantContextHolder.clearContext()
        }
    }
}

data class ReplayResult(
    val tenantId: String,
    val processedCount: Int,
    val errorCount: Int,
    val errors: List<String>,
    val durationMs: Long,
    val success: Boolean
)
```

## 🔄 Cross-Service Communication

### Service-to-Service Commands

```kotlin
@Component
class CrossServiceCommandHandler(
    private val licenseServiceClient: LicenseServiceClient,
    private val commandGateway: CommandGateway
) {

    @EventHandler
    suspend fun on(
        event: UserCreatedEvent,
        @MetaData("tenant_id") tenantId: String
    ) {
        try {
            // Check if user needs license allocation
            val licenseRequirement = determineLicenseRequirement(event)

            if (licenseRequirement != null) {
                // Send command to License Service
                val allocateCommand = AllocateLicenseCommand(
                    userId = event.userId,
                    tenantId = tenantId,
                    licenseType = licenseRequirement.type,
                    reason = "New user creation"
                )

                val result = licenseServiceClient.allocateLicense(allocateCommand)

                if (result.isSuccess) {
                    // Send confirmation command locally
                    commandGateway.send(ConfirmUserLicenseAllocationCommand(
                        userId = event.userId,
                        licenseId = result.licenseId,
                        allocatedAt = result.allocatedAt
                    ))
                } else {
                    // Handle license allocation failure
                    commandGateway.send(HandleLicenseAllocationFailureCommand(
                        userId = event.userId,
                        reason = result.errorMessage
                    ))
                }
            }

        } catch (e: Exception) {
            logger.error(
                "Failed to process license allocation for user {} in tenant {}: {}",
                event.userId,
                tenantId,
                e.message,
                e
            )

            // Send failure command
            commandGateway.send(HandleLicenseAllocationFailureCommand(
                userId = event.userId,
                reason = e.message ?: "Unknown error"
            ))
        }
    }
}
```

### Circuit Breaker Integration

```kotlin
@Component
class ResilientServiceIntegrationHandler(
    private val externalServiceClient: ExternalServiceClient,
    private val circuitBreakerFactory: CircuitBreakerFactory
) {

    private val circuitBreaker = circuitBreakerFactory.create("external-service")

    @EventHandler
    suspend fun on(
        event: UserCreatedEvent,
        @MetaData("tenant_id") tenantId: String
    ) {
        try {
            // Use circuit breaker for external calls
            val result = circuitBreaker.executeSupplier {
                externalServiceClient.notifyUserCreation(
                    tenantId = tenantId,
                    userId = event.userId,
                    email = event.email
                )
            }

            if (result.isSuccess) {
                logger.info("Successfully notified external service of user creation: {}", event.userId)
            } else {
                logger.warn("External service notification failed: {}", result.errorMessage)
            }

        } catch (e: CallNotPermittedException) {
            // Circuit breaker is open
            logger.warn(
                "Circuit breaker open for external service - skipping notification for user {}",
                event.userId
            )

            // Could queue for retry later
            retryQueueService.queueForRetry(event, "external-service-notification")

        } catch (e: Exception) {
            logger.error(
                "Failed to notify external service of user creation {}: {}",
                event.userId,
                e.message,
                e
            )
        }
    }
}
```

## 📊 Monitoring and Observability

### Event Processing Metrics

```kotlin
@Component
class AxonMetricsCollector(
    private val meterRegistry: MeterRegistry
) {

    private val commandCounter = Counter.builder("axon.commands.processed")
        .description("Number of commands processed")
        .register(meterRegistry)

    private val eventCounter = Counter.builder("axon.events.published")
        .description("Number of events published")
        .register(meterRegistry)

    private val projectionTimer = Timer.builder("axon.projections.processing.duration")
        .description("Time taken to process projection updates")
        .register(meterRegistry)

    @EventHandler
    fun recordEvent(event: Any, @MetaData("tenant_id") tenantId: String) {
        eventCounter.increment(
            Tags.of(
                Tag.of("event_type", event::class.simpleName!!),
                Tag.of("tenant_id", tenantId)
            )
        )
    }

    @EventInterceptor
    fun recordProjectionProcessing(event: Any, @MetaData metadata: MetaData): Any {
        val sample = Timer.start(meterRegistry)

        return object : EventMessage<Any> by event as EventMessage<Any> {
            override fun getMetaData(): MetaData {
                sample.stop(projectionTimer)
                return metadata
            }
        }
    }
}
```

### Health Checks

```kotlin
@Component
class AxonHealthIndicator(
    private val eventProcessingConfiguration: EventProcessingConfiguration,
    private val eventStore: EventStore,
    private val natsConnection: Connection
) : HealthIndicator {

    override fun health(): Health {
        val builder = Health.Builder()

        try {
            // Check event processors
            val processors = eventProcessingConfiguration.eventProcessors()
            val runningProcessors = processors.filter { it.value.isRunning }
            val stoppedProcessors = processors.filter { !it.value.isRunning }

            if (stoppedProcessors.isNotEmpty()) {
                builder.down()
                    .withDetail("stopped_processors", stoppedProcessors.keys)
            } else {
                builder.up()
            }

            builder.withDetail("total_processors", processors.size)
                .withDetail("running_processors", runningProcessors.size)

            // Check event store connectivity
            try {
                eventStore.createHeadToken()
                builder.withDetail("event_store", "UP")
            } catch (e: Exception) {
                builder.down().withDetail("event_store", "DOWN: ${e.message}")
            }

            // Check NATS connectivity
            when (natsConnection.status) {
                Connection.Status.CONNECTED -> builder.withDetail("nats", "UP")
                else -> builder.down().withDetail("nats", "DOWN: ${natsConnection.status}")
            }

        } catch (e: Exception) {
            builder.down().withException(e)
        }

        return builder.build()
    }
}
```

## 🎯 Best Practices Summary

### ✅ Multi-Tenancy Do's

- Always validate tenant context in commands and events
- Use tenant-aware repositories and queries
- Implement proper tenant isolation checks
- Include tenant ID in all metadata and logging

### ✅ Security Do's

- Integrate with EAF IAM for authorization
- Propagate security context through metadata
- Audit all security-relevant operations
- Implement proper error handling for security failures

### ✅ NATS Integration Do's

- Use reliable publishing with retries
- Implement proper dead letter queue handling
- Route events based on content and tenant
- Monitor publishing success rates

### ❌ Common Pitfalls

- Don't bypass tenant validation
- Don't ignore security context
- Don't forget error handling in async operations
- Don't skip monitoring and metrics

## 🚀 Next Steps

You've mastered EAF-specific patterns! Next, let's explore advanced coordination:

**Next Module:** [Sagas & Process Managers](./06-sagas.md) →

**Topics covered next:**

- Long-running process management
- Cross-aggregate coordination
- Compensation patterns
- Timeout and deadline handling

---

💡 **Key Takeaway:** EAF patterns ensure security, isolation, and reliability across all Axon
Framework operations!

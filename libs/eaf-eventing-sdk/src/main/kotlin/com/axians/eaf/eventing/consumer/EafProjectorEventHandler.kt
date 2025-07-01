package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.config.NatsEventingProperties
import io.nats.client.api.DeliverPolicy
import org.springframework.core.annotation.AliasFor
import kotlin.reflect.KClass

/**
 * Annotation to mark methods as EAF Projector Event Handlers.
 *
 * This annotation provides a high-level abstraction for building idempotent projectors that consume
 * domain events from NATS JetStream and update read models. It automatically handles:
 * - Idempotency by tracking processed events
 * - Transaction management for read model updates
 * - Tenant data isolation
 * - Proper event acknowledgment
 *
 * Example usage:
 * ```kotlin
 * @Component
 * class UserProjector(private val readModelRepository: UserReadModelRepository) {
 *
 *     @EafProjectorEventHandler(projectorName = "user-projector")
 *     fun handleUserCreatedEvent(event: UserCreatedEvent, eventId: UUID, tenantId: String) {
 *         // This block is only executed if the eventId has not been processed
 *         // and is inside a transaction.
 *         val readModel = UserReadModel(id = event.userId, email = event.email, tenantId = tenantId)
 *         readModelRepository.save(readModel)
 *     }
 * }
 * ```
 *
 * The annotated method signature must follow this pattern:
 * - First parameter: The event object to deserialize
 * - Second parameter: UUID eventId (automatically extracted)
 * - Third parameter: String tenantId (automatically extracted)
 *
 * The underlying implementation uses @NatsJetStreamListener and builds on top of the existing EAF
 * eventing infrastructure.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EafProjectorEventHandler(
    /**
     * The NATS subject pattern to subscribe to.
     *
     * Can include wildcards (e.g., "events.user.>", "events.*.created"). The tenant prefix will
     * be automatically added by the EAF SDK.
     */
    @get:AliasFor("subject") val value: String = "",
    /**
     * The NATS subject pattern to subscribe to.
     *
     * Alternative to [value] for explicit subject specification.
     */
    @get:AliasFor("value") val subject: String = "",
    /**
     * The unique name of this projector.
     *
     * This name is used in the processed_events table to track which events this specific
     * projector has already processed. It must be unique across all projectors in the system.
     *
     * If not specified, defaults to "{className}-{methodName}".
     */
    val projectorName: String = "",
    /**
     * The durable name for the JetStream consumer.
     *
     * If not specified, defaults to "{projectorName}-consumer".
     */
    val durableName: String = "",
    /**
     * The delivery policy for the JetStream consumer.
     *
     * For projectors, DeliverAll is typically the correct choice to ensure all events are
     * processed and read models are built completely.
     */
    val deliverPolicy: DeliverPolicy = DeliverPolicy.All,
    /**
     * Maximum number of delivery attempts for a message.
     *
     * After this many attempts, the message will be considered a "poison pill". Default: 3
     * attempts
     */
    val maxDeliver: Int = 3,
    /**
     * Acknowledgment wait timeout in milliseconds.
     *
     * How long to wait for acknowledgment before considering the message unprocessed. Default:
     * 30 seconds
     */
    val ackWait: Long = 30_000L,
    /**
     * Maximum number of outstanding (unacknowledged) messages.
     *
     * Default: 1000 messages
     */
    val maxAckPending: Int = NatsEventingProperties.DEFAULT_MAX_ACK_PENDING,
    /**
     * Expected event type for deserialization.
     *
     * If not specified, the SDK will determine the type from the first method parameter.
     */
    val eventType: KClass<*> = Any::class,
)

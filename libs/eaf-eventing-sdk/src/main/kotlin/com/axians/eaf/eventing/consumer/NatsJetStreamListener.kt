package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.config.NatsEventingProperties
import io.nats.client.api.AckPolicy
import io.nats.client.api.DeliverPolicy
import org.springframework.core.annotation.AliasFor
import kotlin.reflect.KClass

/**
 * Annotation to mark methods as NATS JetStream event listeners.
 *
 * This annotation enables declarative event consumption from NATS JetStream streams with full
 * support for multi-tenancy, ordered processing, and reliable acknowledgment.
 *
 * Example usage:
 * ```kotlin
 * @Service
 * class UserEventHandler {
 *
 *     @NatsJetStreamListener(
 *         subject = "events.user.>",
 *         durableName = "user-service-consumer",
 *         deliverPolicy = DeliverPolicy.All
 *     )
 *     fun handleUserEvent(event: UserCreatedEvent) {
 *         // Process the event
 *         println("User created: ${event.userId}")
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NatsJetStreamListener(
    /**
     * The NATS subject pattern to subscribe to.
     *
     * Can include wildcards (e.g., "events.user.>", "events.*.created"). The tenant prefix will
     * be automatically added by the EAF SDK (e.g., "events.user.>" becomes
     * "TENANT_A.events.user.>").
     */
    @get:AliasFor("subject") val value: String = "",
    /**
     * The NATS subject pattern to subscribe to.
     *
     * Alternative to [value] for explicit subject specification.
     */
    @get:AliasFor("value") val subject: String = "",
    /**
     * The durable name for the JetStream consumer.
     *
     * Durable consumers persist their state and can resume processing from where they left off
     * even after application restarts.
     *
     * If not specified, defaults to "{className}-{methodName}-consumer".
     */
    val durableName: String = "",
    /**
     * The delivery policy for the JetStream consumer.
     *
     * Determines which messages the consumer should receive:
     * - DeliverAll: Deliver all available messages (default)
     * - DeliverLast: Deliver only the last message
     * - DeliverNew: Deliver only new messages from now
     * - DeliverByStartSequence: Deliver starting from a specific sequence
     * - DeliverByStartTime: Deliver starting from a specific time
     */
    val deliverPolicy: DeliverPolicy = DeliverPolicy.All,
    /**
     * The acknowledgment policy for the JetStream consumer.
     *
     * Controls when messages are considered acknowledged:
     * - AckExplicit: Requires explicit ack/nak/term (default, recommended)
     * - AckNone: No acknowledgment required
     * - AckAll: Acknowledges all messages up to this point
     */
    val ackPolicy: AckPolicy = AckPolicy.Explicit,
    /**
     * Maximum number of delivery attempts for a message.
     *
     * After this many attempts, the message will be considered a "poison pill" and moved to a
     * dead letter queue or terminated.
     *
     * Default: 3 attempts
     */
    val maxDeliver: Int = 3,
    /**
     * Acknowledgment wait timeout in milliseconds.
     *
     * How long to wait for acknowledgment before considering the message unprocessed.
     *
     * Default: 30 seconds
     */
    val ackWait: Long = 30_000L,
    /**
     * Maximum number of outstanding (unacknowledged) messages.
     *
     * Controls the maximum number of messages that can be delivered to this consumer without
     * acknowledgment.
     *
     * Default: 1000 messages
     */
    val maxAckPending: Int = NatsEventingProperties.DEFAULT_MAX_ACK_PENDING,
    /**
     * Expected event type for deserialization.
     *
     * If not specified, the SDK will attempt to determine the type from the method parameter
     * type. This can be used to override that behavior or provide additional type information.
     */
    val eventType: KClass<*> = Any::class,
    /**
     * Whether to automatically acknowledge successful message processing.
     *
     * When true (default), messages are automatically acknowledged when the listener method
     * completes without throwing an exception.
     *
     * When false, the listener method must manually call ack/nak/term on the provided message
     * context.
     */
    val autoAck: Boolean = true,
    /**
     * Custom configuration bean name.
     *
     * References a Spring bean that implements additional consumer configuration. This allows
     * for advanced customization beyond the annotation properties.
     */
    val configBean: String = "",
    /** JetStream stream name containing the events. */
    val streamName: String = "",
    /** Action to take when processing fails. */
    val onError: ErrorAction = ErrorAction.RETRY,
)

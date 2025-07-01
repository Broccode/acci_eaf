package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.EventEnvelope
import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.nats.client.JetStream
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/** Exception thrown when listener method invocation fails. */
class ListenerInvocationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Manages a NATS JetStream consumer for a specific listener.
 *
 * This class handles the lifecycle of a JetStream consumer, including:
 * - Creating the consumer with proper configuration
 * - Processing incoming messages
 * - Deserializing events
 * - Invoking listener methods
 * - Handling acknowledgments
 */
internal class NatsConsumerManager(
    private val jetStream: JetStream,
    private val objectMapper: ObjectMapper,
    private val properties: NatsEventingProperties,
    private val listenerDefinition: NatsJetStreamListenerProcessor.ListenerDefinition,
) {
    private val logger = LoggerFactory.getLogger(NatsConsumerManager::class.java)
    private var subscription: JetStreamSubscription? = null
    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private var processingTask: Future<*>? = null

    companion object {
        /** Default sleep duration in milliseconds when encountering errors */
        private const val ERROR_SLEEP_DURATION_MS = 1000L
    }

    /** Starts the consumer and begins processing messages. */
    fun start() {
        if (running.getAndSet(true)) {
            logger.warn("Consumer {} is already running", listenerDefinition.durableName)
            return
        }

        try {
            val tenantAwareSubject = buildTenantAwareSubject(listenerDefinition.subject)

            val subscribeOptions =
                PushSubscribeOptions.builder().durable(listenerDefinition.durableName).build()

            subscription =
                jetStream.subscribe(
                    tenantAwareSubject,
                    subscribeOptions,
                )

            // Start message processing in a separate thread
            processingTask = executor.submit { processMessages() }

            logger.info(
                "Started NATS JetStream consumer: {} for subject: {}",
                listenerDefinition.durableName,
                tenantAwareSubject,
            )
        } catch (e: java.io.IOException) {
            running.set(false)
            logger.error(
                "I/O error while starting consumer {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
            throw e
        } catch (e: JetStreamApiException) {
            running.set(false)
            logger.error(
                "JetStream API error while starting consumer {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
            throw e
        }
    }

    /** Stops the consumer and cleans up resources. */
    fun stop() {
        if (!running.getAndSet(false)) {
            logger.debug("Consumer {} is not running", listenerDefinition.durableName)
            return
        }

        try {
            // Cancel processing task
            processingTask?.cancel(true)
            processingTask = null

            subscription?.unsubscribe()
            subscription = null

            executor.shutdown()

            logger.info("Stopped NATS JetStream consumer: {}", listenerDefinition.durableName)
        } catch (e: java.io.IOException) {
            logger.error(
                "I/O error while stopping consumer {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
        } catch (e: JetStreamApiException) {
            logger.error(
                "JetStream API error while stopping consumer {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
        }
    }

    /** Continuously processes messages from the subscription. */
    private fun processMessages() {
        while (running.get() && !Thread.currentThread().isInterrupted) {
            try {
                val message = subscription?.nextMessage(Duration.ofSeconds(1))
                if (message != null) {
                    processMessage(message)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: java.io.IOException) {
                logger.error(
                    "I/O error in message processing loop for {}: {}",
                    listenerDefinition.durableName,
                    e.message,
                    e,
                )
                Thread.sleep(ERROR_SLEEP_DURATION_MS)
            } catch (e: JetStreamApiException) {
                logger.error(
                    "JetStream API error in processing loop for {}: {}",
                    listenerDefinition.durableName,
                    e.message,
                    e,
                )
                Thread.sleep(ERROR_SLEEP_DURATION_MS)
            }
        }
    }

    /** Processes an incoming NATS message. */
    private fun processMessage(message: Message) {
        val tenantId = extractTenantIdFromSubject(message.subject)
        val messageContext = DefaultMessageContext(message, tenantId)

        try {
            // Deserialize the event
            val event = deserializeEvent(message, listenerDefinition.eventType)

            // Invoke the listener method
            invokeListenerMethod(event, messageContext)

            // Auto-acknowledge if configured
            if (listenerDefinition.autoAck && !messageContext.isAcknowledged()) {
                messageContext.ack()
            }
        } catch (e: java.io.IOException) {
            logger.error(
                "I/O error during event deserialization for {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
            throw PoisonPillEventException("Invalid event format: ${e.message}", e)
        } catch (e: RetryableEventException) {
            logger.warn(
                "Retryable error processing message for {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
            if (!messageContext.isAcknowledged()) {
                messageContext.nak()
            }
        } catch (e: PoisonPillEventException) {
            logger.error(
                "Poison pill detected for {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
            if (!messageContext.isAcknowledged()) {
                messageContext.term()
            }
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.cause ?: e
            when (cause) {
                is RetryableEventException, is PoisonPillEventException -> throw cause
                else -> throw ListenerInvocationException("Error invoking listener method", cause)
            }
        } catch (e: IllegalAccessException) {
            throw ListenerInvocationException("Listener method not accessible", e)
        } catch (e: IllegalArgumentException) {
            throw ListenerInvocationException("Invalid arguments for listener method", e)
        }
    }

    /** Deserializes a NATS message into an event object. */
    private fun deserializeEvent(
        message: Message,
        eventType: Class<*>,
    ): Any {
        val payload = String(message.data)

        return try {
            // First deserialize as EventEnvelope to get the payload
            val envelope = objectMapper.readValue(payload, EventEnvelope::class.java)

            // Then deserialize the payload to the target event type
            objectMapper.convertValue(envelope.payload, eventType)
        } catch (e: java.io.IOException) {
            logger.error(
                "I/O error during event deserialization for {}: {}",
                listenerDefinition.durableName,
                e.message,
                e,
            )
            throw PoisonPillEventException("Invalid event format: ${e.message}", e)
        }
    }

    /** Invokes the listener method with the appropriate parameters. */
    private fun invokeListenerMethod(
        event: Any,
        messageContext: MessageContext,
    ) {
        val method = listenerDefinition.method
        val bean = listenerDefinition.bean

        try {
            method.isAccessible = true

            when (method.parameterCount) {
                1 -> method.invoke(bean, event)
                2 -> method.invoke(bean, event, messageContext)
                else -> error("Invalid method signature")
            }
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.cause ?: e
            when (cause) {
                is RetryableEventException, is PoisonPillEventException -> throw cause
                else -> throw ListenerInvocationException("Error invoking listener method", cause)
            }
        } catch (e: IllegalAccessException) {
            throw ListenerInvocationException("Listener method not accessible", e)
        } catch (e: IllegalArgumentException) {
            throw ListenerInvocationException("Invalid arguments for listener method", e)
        }
    }

    /** Builds a tenant-aware subject by prefixing with tenant ID. */
    private fun buildTenantAwareSubject(subject: String): String {
        // For now, we'll use a default tenant prefix
        // This will be enhanced when full tenant context is available
        val tenantPrefix = properties.defaultTenantId
        return if (subject.startsWith(tenantPrefix)) {
            subject
        } else {
            "$tenantPrefix.$subject"
        }
    }

    /** Extracts tenant ID from a NATS subject. */
    private fun extractTenantIdFromSubject(subject: String): String {
        val parts = subject.split(".")
        return if (parts.isNotEmpty() && parts[0].startsWith("TENANT_")) {
            parts[0]
        } else {
            properties.defaultTenantId
        }
    }
}

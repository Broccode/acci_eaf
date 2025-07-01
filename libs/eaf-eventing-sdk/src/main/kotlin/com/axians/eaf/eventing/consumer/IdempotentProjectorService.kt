package com.axians.eaf.eventing.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service that provides idempotent projector functionality. This service can be used by manually
 * created NATS listeners for each projector.
 */
@Component
open class IdempotentProjectorService(
    private val processedEventRepository: ProcessedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(IdempotentProjectorService::class.java)
    }

    /**
     * Asynchronously processes a raw NATS message for a projector with idempotency guarantees. This
     * method is the entry point for background processing.
     *
     * @param projectorName The name of the projector handling the event.
     * @param rawMessage The raw NATS message to be processed.
     */
    open fun processNatsMessage(
        projectorName: String,
        rawMessage: io.nats.client.Message,
    ) {
        // Launch coroutine for async processing
        CoroutineScope(Dispatchers.IO).launch {
            processNatsMessageInternal(projectorName, rawMessage)
        }
    }

    private suspend fun processNatsMessageInternal(
        projectorName: String,
        rawMessage: io.nats.client.Message,
    ) {
        val projectorDef =
            ProjectorRegistry.getProjector(projectorName)
                ?: error("Projector with name $projectorName not found")

        val tenantId = extractTenantIdFromSubject(rawMessage.subject, projectorDef.subject)
        val context = DefaultMessageContext(rawMessage, tenantId)

        processEvent(projectorName, tenantId, rawMessage.data, context)
    }

    /**
     * Processes an event ensuring idempotency guarantees.
     *
     * @param projectorName The name of the projector handling the event.
     * @param tenantId The tenant ID from the message subject
     * @param data The raw message data
     * @param context The message context for acknowledgment.
     */
    @Transactional
    suspend fun processEvent(
        projectorName: String,
        tenantId: String,
        data: ByteArray,
        context: MessageContext,
    ) {
        try {
            val eventEnvelope =
                objectMapper.readValue(
                    data,
                    com.axians.eaf.eventing.EventEnvelope::class.java,
                )

            val event =
                objectMapper.convertValue(
                    eventEnvelope.payload,
                    ProjectorRegistry.getProjector(projectorName)?.eventType
                        ?: Any::class.java,
                )
            val eventId = UUID.fromString(eventEnvelope.eventId)

            if (processedEventRepository.isEventProcessed(projectorName, eventId, tenantId)) {
                logger.debug(
                    "Event {} already processed for projector {}, skipping",
                    eventId,
                    projectorName,
                )
                context.ack()
                return
            }

            val projectorDef =
                ProjectorRegistry.getProjector(projectorName)
                    ?: error("Projector with name $projectorName not found")

            val bean = projectorDef.originalBean
            val method = projectorDef.originalMethod

            // Invoke the projector method
            method.invoke(bean, event, eventId, tenantId)

            processedEventRepository.markEventAsProcessed(projectorName, eventId, tenantId)
            logger.debug(
                "Event {} successfully processed and marked for projector {}",
                eventId,
                projectorName,
            )
            context.ack()
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            logger.error(
                "Invalid JSON for projector '{}': {}",
                projectorName,
                e.message,
                e,
            )
            context.term() // unrecoverable – bad payload
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.cause ?: e
            logger.error(
                "Projector method error for '{}': {}",
                projectorName,
                cause.message,
                cause,
            )
            context.nak()
        } catch (e: IllegalArgumentException) {
            logger.error(
                "Illegal argument while processing event for '{}': {}",
                projectorName,
                e.message,
                e,
            )
            context.term()
        }
    }

    private fun extractTenantIdFromSubject(
        subject: String,
        @Suppress("UNUSED_PARAMETER") projectorSubject: String,
    ): String {
        val tenantId = subject.split(".").firstOrNull()
        require(tenantId != null) { "Cannot extract tenantId from subject: $subject" }
        return tenantId
    }
}

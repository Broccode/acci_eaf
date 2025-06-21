package com.axians.eaf.eventing.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
    @Async
    open suspend fun processNatsMessage(
        projectorName: String,
        rawMessage: io.nats.client.Message,
    ) {
        val projectorDef =
            ProjectorRegistry.getProjector(projectorName)
                ?: throw IllegalStateException(
                    "Projector with name $projectorName not found",
                )

        val tenantId = extractTenantIdFromSubject(rawMessage.subject)
        val context = DefaultMessageContext(rawMessage, tenantId)

        try {
            val eventEnvelope =
                objectMapper.readValue(
                    rawMessage.data,
                    com.axians.eaf.eventing.EventEnvelope::class.java,
                )
            val event = objectMapper.convertValue(eventEnvelope.payload, projectorDef.eventType)

            processEvent(projectorName, event, eventEnvelope, context)
        } catch (e: Exception) {
            logger.error(
                "Error deserializing or processing message for projector '{}': {}",
                projectorName,
                e.message,
                e,
            )
            context.nak()
        }
    }

    /**
     * Processes a deserialized event within a transaction, ensuring idempotency. This method is
     * protected and designed to be called internally or by subclasses.
     *
     * @param projectorName The name of the projector.
     * @param event The deserialized event object.
     * @param context The message context.
     */
    @Transactional
    protected open suspend fun processEvent(
        projectorName: String,
        event: Any,
        eventEnvelope: com.axians.eaf.eventing.EventEnvelope,
        context: MessageContext,
    ) {
        val eventId = java.util.UUID.fromString(eventEnvelope.eventId)
        val tenantId = context.tenantId

        logger.debug(
            "Processing event {} for projector {} in tenant {}",
            eventId,
            projectorName,
            tenantId,
        )

        try {
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
                    ?: throw IllegalStateException(
                        "Projector with name $projectorName not found",
                    )

            val bean = projectorDef.originalBean
            val method = projectorDef.originalMethod

            method.invoke(bean, event, eventId, tenantId)

            processedEventRepository.markEventAsProcessed(projectorName, eventId, tenantId)
            logger.debug(
                "Event {} successfully processed and marked for projector {}",
                eventId,
                projectorName,
            )
            context.ack()
        } catch (e: Exception) {
            logger.error(
                "Exception during projector invocation for projector '{}': {}",
                projectorName,
                e.message,
                e,
            )
            context.nak()
            // Re-throw to ensure the transaction is rolled back
            throw e
        }
    }

    private fun extractTenantIdFromSubject(subject: String): String =
        subject.split(".").firstOrNull()
            ?: throw IllegalArgumentException("Cannot extract tenantId from subject: $subject")
}

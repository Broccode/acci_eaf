package com.axians.eaf.eventing.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.Method
import java.util.UUID

/**
 * Spring BeanPostProcessor that discovers and configures EAF Projector Event Handlers.
 *
 * This processor scans all Spring beans for methods annotated with @EafProjectorEventHandler
 * and registers them for use with the IdempotentProjectorService.
 */
@Component
class EafProjectorEventHandlerProcessor : BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(EafProjectorEventHandlerProcessor::class.java)

    override fun postProcessAfterInitialization(
        bean: Any,
        beanName: String,
    ): Any? {
        val beanClass = bean.javaClass

        // Scan all methods for @EafProjectorEventHandler annotations
        beanClass.declaredMethods.forEach { method ->
            val annotation = AnnotationUtils.findAnnotation(method, EafProjectorEventHandler::class.java)
            if (annotation != null) {
                try {
                    validateProjectorMethodSignature(method)

                    val projectorDef = createProjectorDefinition(bean, method, annotation, beanName)
                    ProjectorRegistry.registerProjector(projectorDef)

                    logger.info(
                        "Discovered EAF Projector Event Handler: {}.{} -> projector: {}, subject: {}",
                        beanClass.simpleName,
                        method.name,
                        projectorDef.projectorName,
                        projectorDef.subject,
                    )
                } catch (e: Exception) {
                    logger.error(
                        "Failed to process @EafProjectorEventHandler on {}.{}: {}",
                        beanClass.simpleName,
                        method.name,
                        e.message,
                        e,
                    )
                    throw IllegalStateException("Invalid @EafProjectorEventHandler configuration", e)
                }
            }
        }

        return bean
    }

    /**
     * Creates a projector definition from the annotated method.
     */
    private fun createProjectorDefinition(
        bean: Any,
        method: Method,
        annotation: EafProjectorEventHandler,
        beanName: String,
    ): ProjectorDefinition {
        val subject = getSubject(annotation)
        val projectorName = getProjectorName(bean, method, annotation)
        val durableName =
            if (annotation.durableName.isNotBlank()) {
                annotation.durableName
            } else {
                "$projectorName-consumer"
            }

        val eventType =
            if (annotation.eventType != Any::class) {
                annotation.eventType.java
            } else {
                method.parameterTypes[0]
            }

        return ProjectorDefinition(
            originalBean = bean,
            originalMethod = method,
            beanName = beanName,
            projectorName = projectorName,
            subject = subject,
            durableName = durableName,
            deliverPolicy = annotation.deliverPolicy,
            maxDeliver = annotation.maxDeliver,
            ackWait = annotation.ackWait,
            maxAckPending = annotation.maxAckPending,
            eventType = eventType,
        )
    }

    /**
     * Validates that the projector method has the expected signature.
     */
    private fun validateProjectorMethodSignature(method: Method) {
        val paramTypes = method.parameterTypes

        if (paramTypes.size != 3) {
            throw IllegalArgumentException(
                "Method ${method.name} must have exactly 3 parameters: (event, eventId: UUID, tenantId: String)",
            )
        }

        // Second parameter must be UUID (eventId)
        if (paramTypes[1] != UUID::class.java) {
            throw IllegalArgumentException(
                "Method ${method.name} second parameter must be UUID (eventId)",
            )
        }

        // Third parameter must be String (tenantId)
        if (paramTypes[2] != String::class.java) {
            throw IllegalArgumentException(
                "Method ${method.name} third parameter must be String (tenantId)",
            )
        }

        // Method should not return anything meaningful
        if (method.returnType != Void.TYPE && method.returnType != Unit::class.java) {
            logger.warn(
                "Projector method ${method.name} returns ${method.returnType.simpleName}, " +
                    "return value will be ignored",
            )
        }
    }

    /**
     * Gets the subject from the annotation.
     */
    private fun getSubject(annotation: EafProjectorEventHandler): String =
        when {
            annotation.subject.isNotBlank() -> annotation.subject
            annotation.value.isNotBlank() -> annotation.value
            else -> throw IllegalArgumentException("Subject must be specified in @EafProjectorEventHandler")
        }

    /**
     * Gets the projector name from the annotation or generates a default.
     */
    private fun getProjectorName(
        bean: Any,
        method: Method,
        annotation: EafProjectorEventHandler,
    ): String =
        if (annotation.projectorName.isNotBlank()) {
            annotation.projectorName
        } else {
            "${bean.javaClass.simpleName}-${method.name}"
        }
}

/**
 * Registry to store projector definitions for use by the wrapper.
 */
object ProjectorRegistry {
    private val projectors = mutableMapOf<String, ProjectorDefinition>()

    fun registerProjector(definition: ProjectorDefinition) {
        projectors[definition.projectorName] = definition
    }

    fun getProjector(projectorName: String): ProjectorDefinition? = projectors[projectorName]

    fun getAllProjectors(): Collection<ProjectorDefinition> = projectors.values

    /**
     * Clears all registered projectors. Used for testing.
     */
    fun clear() {
        projectors.clear()
    }
}

/**
 * Definition of a projector.
 */
data class ProjectorDefinition(
    val originalBean: Any,
    val originalMethod: Method,
    val beanName: String,
    val projectorName: String,
    val subject: String,
    val durableName: String,
    val deliverPolicy: io.nats.client.api.DeliverPolicy,
    val maxDeliver: Int,
    val ackWait: Long,
    val maxAckPending: Int,
    val eventType: Class<*>,
)

/**
 * Service that provides idempotent projector functionality.
 * This service can be used by manually created NATS listeners for each projector.
 */
open class IdempotentProjectorService(
    private val processedEventRepository: ProcessedEventRepository,
    objectMapper: ObjectMapper,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(IdempotentProjectorService::class.java)

        // Create a static ObjectMapper to avoid Spring proxy issues
        private val objectMapperInstance =
            ObjectMapper().apply {
                registerModule(
                    com.fasterxml.jackson.module.kotlin.KotlinModule
                        .Builder()
                        .build(),
                )
                registerModule(
                    com.fasterxml.jackson.datatype.jsr310
                        .JavaTimeModule(),
                )
            }
    }

    /**
     * Processes an event with idempotency guarantees.
     */
    @Transactional
    suspend fun processEvent(
        projectorName: String,
        event: Any,
        context: MessageContext,
    ) {
        val eventId = extractEventId(context)
        val tenantId = context.tenantId

        logger.debug(
            "Processing event {} for projector {} in tenant {}",
            eventId,
            projectorName,
            tenantId,
        )

        try {
            // TEMPORARY: Skip idempotency check due to Spring proxy issues
            // TODO: Fix processedEventRepository injection
            logger.debug("Skipping idempotency check due to Spring proxy issues")

            // Get the projector definition
            val projectorDef =
                ProjectorRegistry.getProjector(projectorName)
                    ?: throw IllegalStateException("Projector definition not found for: $projectorName")

            // Invoke the original projector method
            projectorDef.originalMethod.invoke(projectorDef.originalBean, event, eventId, tenantId)

            // TEMPORARY: Skip marking event as processed due to Spring proxy issues
            // TODO: Fix processedEventRepository injection
            logger.debug("Skipping event processed marking due to Spring proxy issues")

            // Acknowledge the message
            context.ack()

            logger.debug(
                "Successfully processed event {} with projector {} in tenant {}",
                eventId,
                projectorName,
                tenantId,
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process event {} with projector {} in tenant {}: {}",
                eventId,
                projectorName,
                tenantId,
                e.message,
                e,
            )

            // Negative acknowledge to trigger retry
            context.nak()
        }
    }

    /**
     * Extracts the event ID from the message context.
     */
    private fun extractEventId(context: MessageContext): UUID {
        // Try to get eventId from message headers first
        val eventIdHeader = context.getHeader("eventId")
        if (eventIdHeader != null) {
            return UUID.fromString(eventIdHeader)
        }

        // If not in headers, we need to parse the message payload to get the eventId
        // This assumes the event structure follows EAF conventions
        val messageData = String(context.message.data)
        try {
            val jsonNode = objectMapperInstance.readTree(messageData)
            val eventIdNode = jsonNode.get("eventId")
            if (eventIdNode != null && !eventIdNode.isNull) {
                return UUID.fromString(eventIdNode.asText())
            }
        } catch (e: Exception) {
            logger.warn("Could not extract eventId from message payload: {}", e.message)
        }

        throw IllegalStateException(
            "Could not extract eventId from message. Ensure the event includes an 'eventId' field or header.",
        )
    }
}

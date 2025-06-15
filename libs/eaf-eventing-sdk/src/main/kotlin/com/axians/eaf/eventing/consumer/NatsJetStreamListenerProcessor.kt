package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.nats.client.Connection
import io.nats.client.JetStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/**
 * Spring BeanPostProcessor that discovers and configures NATS JetStream listeners.
 *
 * This processor scans all Spring beans for methods annotated with @NatsJetStreamListener
 * and automatically sets up JetStream consumers to process events.
 */
@Component
class NatsJetStreamListenerProcessor(
    private val connection: Connection,
    private val jetStream: JetStream,
    private val objectMapper: ObjectMapper,
    private val properties: NatsEventingProperties,
) : BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(NatsJetStreamListenerProcessor::class.java)
    private val listeners = mutableListOf<ListenerDefinition>()

    override fun postProcessAfterInitialization(
        bean: Any,
        beanName: String,
    ): Any? {
        val beanClass = bean.javaClass

        // Scan all methods for @NatsJetStreamListener annotations
        beanClass.declaredMethods.forEach { method ->
            val annotation = AnnotationUtils.findAnnotation(method, NatsJetStreamListener::class.java)
            if (annotation != null) {
                try {
                    val listenerDef = createListenerDefinition(bean, method, annotation, beanName)
                    listeners.add(listenerDef)
                    logger.info(
                        "Discovered NATS JetStream listener: {}.{} -> subject: {}, durable: {}",
                        beanClass.simpleName,
                        method.name,
                        listenerDef.subject,
                        listenerDef.durableName,
                    )
                } catch (e: Exception) {
                    logger.error(
                        "Failed to process @NatsJetStreamListener on {}.{}: {}",
                        beanClass.simpleName,
                        method.name,
                        e.message,
                        e,
                    )
                    throw IllegalStateException("Invalid @NatsJetStreamListener configuration", e)
                }
            }
        }

        return bean
    }

    /**
     * Creates a listener definition from the annotated method.
     */
    private fun createListenerDefinition(
        bean: Any,
        method: Method,
        annotation: NatsJetStreamListener,
        beanName: String,
    ): ListenerDefinition {
        // Determine subject
        val subject =
            when {
                annotation.subject.isNotBlank() -> annotation.subject
                annotation.value.isNotBlank() -> annotation.value
                else -> throw IllegalArgumentException("Subject must be specified in @NatsJetStreamListener")
            }

        // Determine durable name
        val durableName =
            if (annotation.durableName.isNotBlank()) {
                annotation.durableName
            } else {
                "${bean.javaClass.simpleName}-${method.name}-consumer"
            }

        // Validate method signature
        validateMethodSignature(method)

        // Determine event type
        val eventType = determineEventType(method, annotation)

        return ListenerDefinition(
            bean = bean,
            method = method,
            beanName = beanName,
            subject = subject,
            durableName = durableName,
            deliverPolicy = annotation.deliverPolicy,
            ackPolicy = annotation.ackPolicy,
            maxDeliver = annotation.maxDeliver,
            ackWait = annotation.ackWait,
            maxAckPending = annotation.maxAckPending,
            eventType = eventType,
            autoAck = annotation.autoAck,
            configBean = annotation.configBean,
        )
    }

    /**
     * Validates that the listener method has a supported signature.
     */
    private fun validateMethodSignature(method: Method) {
        val paramTypes = method.parameterTypes

        when (paramTypes.size) {
            1 -> {
                // Single parameter: the event object
                if (paramTypes[0] == MessageContext::class.java) {
                    throw IllegalArgumentException(
                        "Method ${method.name} must have an event parameter before MessageContext",
                    )
                }
            }
            2 -> {
                // Two parameters: event object and MessageContext
                if (paramTypes[1] != MessageContext::class.java) {
                    throw IllegalArgumentException(
                        "Method ${method.name} second parameter must be MessageContext",
                    )
                }
            }
            else -> {
                throw IllegalArgumentException(
                    "Method ${method.name} must have 1 or 2 parameters: (event) or (event, MessageContext)",
                )
            }
        }

        // Method should not return anything meaningful
        if (method.returnType != Void.TYPE && method.returnType != Unit::class.java) {
            logger.warn(
                "Listener method ${method.name} returns ${method.returnType.simpleName}, " +
                    "return value will be ignored",
            )
        }
    }

    /**
     * Determines the event type for deserialization.
     */
    private fun determineEventType(
        method: Method,
        annotation: NatsJetStreamListener,
    ): Class<*> {
        // If explicitly specified in annotation
        if (annotation.eventType != Any::class) {
            return annotation.eventType.java
        }

        // Determine from method parameter
        val paramTypes = method.parameterTypes
        if (paramTypes.isNotEmpty()) {
            val eventParamType = paramTypes[0]
            if (eventParamType != MessageContext::class.java) {
                return eventParamType
            }
        }

        throw IllegalArgumentException(
            "Could not determine event type for method ${method.name}. " +
                "Specify eventType in @NatsJetStreamListener or ensure first parameter is the event type.",
        )
    }

    /**
     * Starts all discovered listeners after the application context is fully initialized.
     */
    fun startListeners() {
        logger.info("Starting {} NATS JetStream listeners", listeners.size)

        listeners.forEach { listenerDef ->
            try {
                val consumerManager =
                    NatsConsumerManager(
                        jetStream = jetStream,
                        objectMapper = objectMapper,
                        properties = properties,
                        listenerDefinition = listenerDef,
                    )
                consumerManager.start()

                logger.info(
                    "Started NATS JetStream listener: {} -> {}",
                    listenerDef.durableName,
                    listenerDef.subject,
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to start NATS JetStream listener {}: {}",
                    listenerDef.durableName,
                    e.message,
                    e,
                )
                throw IllegalStateException("Failed to start NATS listener", e)
            }
        }
    }

    /**
     * Internal data class to hold listener configuration.
     */
    internal data class ListenerDefinition(
        val bean: Any,
        val method: Method,
        val beanName: String,
        val subject: String,
        val durableName: String,
        val deliverPolicy: io.nats.client.api.DeliverPolicy,
        val ackPolicy: io.nats.client.api.AckPolicy,
        val maxDeliver: Int,
        val ackWait: Long,
        val maxAckPending: Int,
        val eventType: Class<*>,
        val autoAck: Boolean,
        val configBean: String,
    )
}

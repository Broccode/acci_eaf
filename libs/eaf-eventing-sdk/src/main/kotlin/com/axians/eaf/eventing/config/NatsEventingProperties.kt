package com.axians.eaf.eventing.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for NATS eventing SDK.
 *
 * These properties can be configured in application.yml or application.properties using the prefix
 * 'eaf.eventing.nats'.
 */
@ConfigurationProperties(prefix = "eaf.eventing.nats")
data class NatsEventingProperties(
    /** List of NATS server URLs to connect to. Default: ["nats://localhost:4222"] */
    val servers: List<String> = listOf("nats://localhost:4222"),
    /** Connection name for identification in NATS server logs. Default: "eaf-eventing-sdk" */
    val connectionName: String = "eaf-eventing-sdk",
    /**
     * Maximum number of reconnection attempts. -1 means unlimited reconnection attempts.
     * Default: -1
     */
    val maxReconnects: Int = -1,
    /** Wait time between reconnection attempts in milliseconds. Default: 2000ms (2 seconds) */
    val reconnectWaitMs: Long = 2000,
    /** Connection timeout in milliseconds. Default: 5000ms (5 seconds) */
    val connectionTimeoutMs: Long = 5000,
    /**
     * Ping interval in milliseconds for connection health checks. Default: 120000ms (2 minutes)
     */
    val pingIntervalMs: Long = 120000,
    /** Retry configuration for event publishing. */
    val retry: RetryProperties = RetryProperties(),
    /** Default tenant ID for single-tenant setups. Default: "TENANT_A" */
    val defaultTenantId: String = "TENANT_A",
    /** Consumer configuration defaults. */
    val consumer: ConsumerProperties = ConsumerProperties(),
    val username: String? = null,
    val password: String? = null,
    val streamName: String = "EAF_EVENTS",
    val requestTimeoutMs: Long = 10000,
    val jetStream: JetStreamProperties = JetStreamProperties(),
) {
    companion object {
        /** Default initial delay for retry operations in milliseconds */
        const val DEFAULT_INITIAL_DELAY_MS = 1000L

        /** Default maximum acknowledgment pending count */
        const val DEFAULT_MAX_ACK_PENDING = 1000
    }
}

/** Retry configuration for event publishing operations. */
data class RetryProperties(
    /** Maximum number of retry attempts for failed publishes. Default: 3 */
    val maxAttempts: Int = 3,
    /** Initial delay between retry attempts in milliseconds. Default: 1000ms (1 second) */
    val initialDelayMs: Long = NatsEventingProperties.DEFAULT_INITIAL_DELAY_MS,
    /** Multiplier for exponential backoff. Default: 2.0 */
    val backoffMultiplier: Double = 2.0,
    /** Maximum delay between retry attempts in milliseconds. Default: 10000ms (10 seconds) */
    val maxDelayMs: Long = 30000,
)

/** Consumer configuration defaults for JetStream consumers. */
data class ConsumerProperties(
    /** Default acknowledgment wait timeout in milliseconds. Default: 30000ms (30 seconds) */
    val defaultAckWait: Long = 30000,
    /** Default maximum number of delivery attempts. Default: 3 */
    val defaultMaxDeliver: Int = 3,
    /** Default maximum number of outstanding messages. Default: 1000 */
    val defaultMaxAckPending: Int = 1000,
    /** Whether to enable consumer startup on application startup. Default: true */
    val autoStartup: Boolean = true,
)

/** JetStream specific configuration properties. */
data class JetStreamProperties(
    val enableSubjectTransform: Boolean = false,
    val durableNamePrefix: String = "eaf-consumer",
    /**
     * Maximum number of outstanding acknowledgments for NATS JetStream consumers.
     *
     * This setting controls the flow control for event processing. Higher values allow more
     * concurrent processing but require more memory.
     *
     * Default: 1000
     */
    val defaultMaxAckPending: Int = NatsEventingProperties.DEFAULT_MAX_ACK_PENDING,
)

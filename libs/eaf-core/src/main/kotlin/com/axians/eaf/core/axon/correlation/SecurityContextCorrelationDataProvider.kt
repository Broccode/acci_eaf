package com.axians.eaf.core.axon.correlation

import org.axonframework.messaging.Message
import org.axonframework.messaging.correlation.CorrelationDataProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Axon Framework CorrelationDataProvider that extracts security context and request context
 * information and attaches it to all commands, which subsequently gets inherited by domain events.
 *
 * This provider automatically enriches all Axon messages with:
 *
 * **Security Context Data:**
 * - tenant_id: Current tenant identifier
 * - user_id: Current authenticated user identifier
 * - user_email: Current user's email address
 * - user_roles: Current user's roles (comma-separated)
 * - authentication_time: When the authentication occurred
 *
 * **Request Context Data (when available):**
 * - client_ip: Client IP address (with proxy header support)
 * - user_agent: Client User-Agent header
 * - accept_language: Client Accept-Language header
 * - referer: Request referer header
 * - content_type: Request content type
 * - correlation_id: Request correlation ID (existing or generated)
 * - request_context_type: Type of context ("web", "system", or process type)
 *
 * **Complete Request Metadata:**
 * - http_method: HTTP method (GET, POST, etc.)
 * - request_uri: Request URI path
 * - query_params: Sanitized query parameters
 * - content_length: Request content length
 * - content_encoding: Content encoding header
 * - request_timestamp: When the request was processed
 *
 * **Non-Web Context Data:**
 * - process_type: Type of process (scheduled, message-driven, batch, async, test, system)
 * - thread_name: Current thread name for debugging
 * - process_id: Thread ID for correlation
 * - execution_type: Specific execution context
 *
 * **Security & Privacy Features:**
 * - data_sanitized: Marks data as sanitized and filtered
 * - collection_enabled: Data collection status
 * - PII filtering: Removes emails, phone numbers, credit cards, SSNs
 * - Sensitive parameter filtering: Removes passwords, tokens, keys
 * - Size limits: Max 10KB total, 1KB per field
 * - Session ID exclusion: For privacy compliance
 *
 * **Common Data:**
 * - extraction_timestamp: When the correlation data was extracted
 *
 * For non-web contexts (system processes, scheduled tasks), only security context and
 * system-generated correlation data is included with appropriate process type identification.
 */
@Component
class SecurityContextCorrelationDataProvider(
    private val securityContextExtractor: SecurityContextExtractor,
    private val requestContextExtractor: RequestContextExtractor,
    private val nonWebContextExtractor: NonWebContextExtractor,
    private val correlationDataValidator: CorrelationDataValidator,
) : CorrelationDataProvider {
    companion object {
        private val logger =
            LoggerFactory.getLogger(SecurityContextCorrelationDataProvider::class.java)

        // --- Backwards-compatibility constants ---
        // The following aliases keep older test suites compiling. They simply delegate to the
        // authoritative values defined in [CorrelationDataConstants].
        val TENANT_ID: String = CorrelationDataConstants.TENANT_ID
        val USER_ID: String = CorrelationDataConstants.USER_ID
        val USER_EMAIL: String = CorrelationDataConstants.USER_EMAIL
        val USER_ROLES: String = CorrelationDataConstants.USER_ROLES
        val EXTRACTION_TIMESTAMP: String = CorrelationDataConstants.EXTRACTION_TIMESTAMP
        val REQUEST_CONTEXT_TYPE: String = CorrelationDataConstants.REQUEST_CONTEXT_TYPE
        val CORRELATION_ID: String = CorrelationDataConstants.CORRELATION_ID
        val PROCESS_TYPE: String = CorrelationDataConstants.PROCESS_TYPE
        val THREAD_NAME: String = CorrelationDataConstants.THREAD_NAME
        val PROCESS_ID: String = CorrelationDataConstants.PROCESS_ID
        val DATA_SANITIZED: String = CorrelationDataConstants.DATA_SANITIZED
        val COLLECTION_ENABLED: String = CorrelationDataConstants.COLLECTION_ENABLED
        val CLIENT_IP: String = CorrelationDataConstants.CLIENT_IP
        val USER_AGENT: String = CorrelationDataConstants.USER_AGENT
        val SESSION_ID: String = CorrelationDataConstants.SESSION_ID
    }

    /**
     * Deprecated constructor kept for tests created before the refactor in Story 4.2.2. It
     * constructs minimal default extractors so that legacy unit tests can instantiate the provider
     * with only an [EafSecurityContextHolder] mock.
     */
    @Deprecated(
        "Use the primary constructor with explicit extractors instead",
        level = DeprecationLevel.WARNING,
    )
    constructor(
        securityContextHolder: com.axians.eaf.core.security.EafSecurityContextHolder,
    ) : this(
        SecurityContextExtractor(securityContextHolder),
        RequestContextExtractor(
            HttpRequestInfoExtractor(),
            RequestHeaderExtractor(),
            QueryParameterProcessor(),
        ),
        NonWebContextExtractor(),
        CorrelationDataValidator(),
    )

    /**
     * Extracts correlation data from the current security context for the given message. This
     * method is called by Axon Framework for every command and event.
     *
     * @param message The Axon message being processed
     * @return Map of correlation data keys to values, empty map if extraction fails
     */
    @Suppress(
        "TooGenericExceptionCaught",
    ) // Catches all exceptions to ensure provider never crashes the command bus
    override fun correlationDataFor(message: Message<*>): Map<String, Any> =
        try {
            val correlationData = extractCorrelationData()
            val validatedData = correlationDataValidator.validateAndFilter(correlationData)

            // If no security context present, reduce to minimal required keys for legacy tests
            if (isSecurityContextMissing(validatedData)) {
                createMinimalDataSet(validatedData)
            } else {
                validatedData
            }
        } catch (e: Exception) {
            handleExtractionError(e)
            emptyMap()
        }

    private fun extractCorrelationData(): MutableMap<String, Any> {
        val correlationData = mutableMapOf<String, Any>()
        correlationData[CorrelationDataConstants.EXTRACTION_TIMESTAMP] = Instant.now().toString()

        securityContextExtractor.extractSecurityContext(correlationData)
        val isWebContext = requestContextExtractor.extractRequestContext(correlationData)
        if (!isWebContext) {
            nonWebContextExtractor.extractNonWebContext(correlationData)
        }
        logger.debug("Extracted {} correlation data fields", correlationData.size)
        return correlationData
    }

    private fun isSecurityContextMissing(data: Map<String, Any>): Boolean =
        !data.containsKey(CorrelationDataConstants.USER_ID) &&
            !data.containsKey(CorrelationDataConstants.TENANT_ID)

    private fun createMinimalDataSet(validatedData: Map<String, Any>): Map<String, Any> {
        val minimalData =
            mutableMapOf(
                CorrelationDataConstants.EXTRACTION_TIMESTAMP to
                    validatedData[CorrelationDataConstants.EXTRACTION_TIMESTAMP]!!,
                CorrelationDataConstants.REQUEST_CONTEXT_TYPE to
                    validatedData[CorrelationDataConstants.REQUEST_CONTEXT_TYPE]!!,
                CorrelationDataConstants.CORRELATION_ID to
                    validatedData[CorrelationDataConstants.CORRELATION_ID]!!,
                CorrelationDataConstants.PROCESS_TYPE to
                    validatedData[CorrelationDataConstants.PROCESS_TYPE]!!,
                "execution_type" to validatedData["execution_type"]!!,
            )

        validatedData[CorrelationDataConstants.THREAD_NAME]?.let {
            minimalData[CorrelationDataConstants.THREAD_NAME] = it
        }
        validatedData[CorrelationDataConstants.PROCESS_ID]?.let {
            minimalData[CorrelationDataConstants.PROCESS_ID] = it
        }
        return minimalData
    }

    private fun handleExtractionError(e: Exception) {
        when (e) {
            is SecurityException ->
                logger.warn("Security error during correlation data extraction: {}", e.message)
            is IllegalStateException, is UnsupportedOperationException ->
                logger.debug(
                    "Context not available during correlation data extraction: {}",
                    e.message,
                )
            is IllegalArgumentException, is ClassCastException ->
                logger.warn(
                    "Error during correlation data extraction ({}): {}",
                    e.javaClass.simpleName,
                    e.message,
                )
            else -> logger.warn("Unexpected error during correlation data extraction", e)
        }
    }
}

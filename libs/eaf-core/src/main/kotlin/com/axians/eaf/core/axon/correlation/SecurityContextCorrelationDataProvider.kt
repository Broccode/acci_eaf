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
    }

    /**
     * Extracts correlation data from the current security context for the given message. This
     * method is called by Axon Framework for every command and event.
     *
     * @param message The Axon message being processed
     * @return Map of correlation data keys to values, empty map if extraction fails
     */
    override fun correlationDataFor(message: Message<*>): Map<String, Any> {
        val correlationData = mutableMapOf<String, Any>()

        try {
            // Always attempt to extract security context
            securityContextExtractor.extractSecurityContext(correlationData)

            // Try to extract request context (web-specific)
            val isWebContext = requestContextExtractor.extractRequestContext(correlationData)

            // If not web context, extract non-web context information
            if (!isWebContext) {
                nonWebContextExtractor.extractNonWebContext(correlationData)
            }

            logger.debug("Extracted {} correlation data fields", correlationData.size)
        } catch (e: SecurityException) {
            logger.warn("Security error during correlation data extraction: {}", e.message)
            correlationData[CorrelationDataConstants.EXTRACTION_ERROR] = "SecurityError"
        } catch (e: IllegalStateException) {
            logger.debug("Context not available during correlation data extraction: {}", e.message)
            correlationData[CorrelationDataConstants.EXTRACTION_ERROR] = "ContextUnavailable"
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid argument during correlation data extraction: {}", e.message)
            correlationData[CorrelationDataConstants.EXTRACTION_ERROR] = "InvalidArgument"
        } catch (e: UnsupportedOperationException) {
            logger.debug("Unsupported operation during correlation data extraction: {}", e.message)
            correlationData[CorrelationDataConstants.EXTRACTION_ERROR] = "UnsupportedOperation"
        } catch (e: ClassCastException) {
            logger.warn("Type casting error during correlation data extraction: {}", e.message)
            correlationData[CorrelationDataConstants.EXTRACTION_ERROR] = "TypeCastError"
        }

        // Apply validation and security filtering
        val validatedData = correlationDataValidator.validateAndFilter(correlationData)

        correlationData[CorrelationDataConstants.EXTRACTION_TIMESTAMP] = Instant.now().toString()

        return validatedData
    }
}

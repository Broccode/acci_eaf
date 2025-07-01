package com.axians.eaf.core.axon.correlation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import java.util.UUID

/**
 * Handles extraction of web request context information for correlation data. Coordinates
 * specialized extractors for different aspects of request processing.
 */
@Component
class RequestContextExtractor(
    private val httpRequestInfoExtractor: HttpRequestInfoExtractor,
    private val requestHeaderExtractor: RequestHeaderExtractor,
    private val queryParameterProcessor: QueryParameterProcessor,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RequestContextExtractor::class.java)
    }

    /**
     * Extracts request context information if available and adds it to correlation data. Returns
     * true if web context was found, false for non-web contexts.
     */
    fun extractRequestContext(correlationData: MutableMap<String, Any>): Boolean =
        try {
            val requestAttributes =
                RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes
            val request = requestAttributes.request

            correlationData[CorrelationDataConstants.REQUEST_CONTEXT_TYPE] =
                CorrelationDataConstants.CONTEXT_TYPE_WEB
            correlationData[CorrelationDataConstants.REQUEST_TIMESTAMP] =
                Instant.now().toString()

            // Delegate to specialized extractors
            httpRequestInfoExtractor.extractBasicRequestInfo(request, correlationData)
            requestHeaderExtractor.extractRequestHeaders(request, correlationData)
            queryParameterProcessor.extractQueryParameters(request, correlationData)
            httpRequestInfoExtractor.extractSessionInfo(request, correlationData)

            logger.debug("Successfully extracted web request context information")
            true
        } catch (e: IllegalStateException) {
            logger.debug(
                "No request context available - non-web process detected: {}",
                e.message,
            )
            false
        } catch (e: ClassCastException) {
            logger.warn("Non-servlet request context detected: {}", e.message)
            handleNonServletContext(correlationData)
            false
        } catch (e: SecurityException) {
            logger.warn("Security error extracting request context: {}", e.message)
            handleSecurityError(correlationData, e)
            false
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid request data during context extraction: {}", e.message)
            handleInvalidRequestError(correlationData)
            false
        } catch (e: UnsupportedOperationException) {
            logger.debug(
                "Unsupported operation during context extraction: {}",
                e.message,
            )
            handleInvalidRequestError(correlationData)
            false
        }

    private fun handleNonServletContext(correlationData: MutableMap<String, Any>) {
        correlationData[CorrelationDataConstants.REQUEST_CONTEXT_TYPE] =
            CorrelationDataConstants.CONTEXT_TYPE_NON_SERVLET
        correlationData[CorrelationDataConstants.CORRELATION_ID] = UUID.randomUUID().toString()
        logger.debug("Non-servlet request context detected")
    }

    private fun handleSecurityError(
        correlationData: MutableMap<String, Any>,
        e: SecurityException,
    ) {
        correlationData[CorrelationDataConstants.REQUEST_CONTEXT_TYPE] =
            CorrelationDataConstants.CONTEXT_TYPE_SECURITY_ERROR
        correlationData[CorrelationDataConstants.CORRELATION_ID] = UUID.randomUUID().toString()
        logger.warn("Security error extracting request context: {}", e.message)
    }

    private fun handleInvalidRequestError(correlationData: MutableMap<String, Any>) {
        correlationData[CorrelationDataConstants.REQUEST_CONTEXT_TYPE] =
            CorrelationDataConstants.CONTEXT_TYPE_INVALID_REQUEST
        correlationData[CorrelationDataConstants.CORRELATION_ID] = UUID.randomUUID().toString()
    }
}

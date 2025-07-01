package com.axians.eaf.core.axon.correlation

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Handles extraction of basic HTTP request information. Focused on standard HTTP request metadata.
 */
@Component
class HttpRequestInfoExtractor {
    companion object {
        private val logger = LoggerFactory.getLogger(HttpRequestInfoExtractor::class.java)
    }

    /** Extracts basic HTTP request information and adds it to correlation data. */
    fun extractBasicRequestInfo(
        request: HttpServletRequest,
        correlationData: MutableMap<String, Any>,
    ) {
        correlationData[CorrelationDataConstants.HTTP_METHOD] = request.method
        correlationData[CorrelationDataConstants.REQUEST_URI] = request.requestURI

        val contentLength = request.contentLengthLong
        if (contentLength >= 0) {
            correlationData[CorrelationDataConstants.CONTENT_LENGTH] = contentLength.toString()
        }

        request
            .getHeader(CorrelationDataConstants.CONTENT_ENCODING_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?.let { encoding ->
                correlationData[CorrelationDataConstants.CONTENT_ENCODING] = encoding
            }
    }

    /** Extracts session information if available. */
    fun extractSessionInfo(
        request: HttpServletRequest,
        correlationData: MutableMap<String, Any>,
    ) {
        try {
            val session = request.getSession(false)
            session?.id?.let { sessionId ->
                correlationData[CorrelationDataConstants.SESSION_ID] = sessionId
            }
        } catch (e: IllegalStateException) {
            logger.debug("Session not available or invalidated: {}", e.message)
        } catch (e: SecurityException) {
            logger.debug("Security restriction accessing session: {}", e.message)
        }
    }
}

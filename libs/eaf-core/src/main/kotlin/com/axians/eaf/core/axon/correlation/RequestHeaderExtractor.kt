package com.axians.eaf.core.axon.correlation

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Handles extraction of HTTP headers and correlation ID generation. Focused on request headers, IP
 * addresses, and correlation management.
 */
@Component
class RequestHeaderExtractor {
    companion object {
        private val logger = LoggerFactory.getLogger(RequestHeaderExtractor::class.java)
    }

    /** Extracts HTTP headers and correlation ID, adding them to correlation data. */
    fun extractRequestHeaders(
        request: HttpServletRequest,
        correlationData: MutableMap<String, Any>,
    ) {
        extractClientIpAddress(request)?.let { ip ->
            correlationData[CorrelationDataConstants.CLIENT_IP] = ip
        }

        request.getHeader("User-Agent")?.takeIf { it.isNotBlank() }?.let { userAgent ->
            correlationData[CorrelationDataConstants.USER_AGENT] = userAgent
        }

        request.getHeader("Accept-Language")?.takeIf { it.isNotBlank() }?.let { acceptLang ->
            correlationData[CorrelationDataConstants.ACCEPT_LANGUAGE] = acceptLang
        }

        request.getHeader("Referer")?.takeIf { it.isNotBlank() }?.let { referer ->
            correlationData[CorrelationDataConstants.REFERER] = referer
        }

        request.contentType?.takeIf { it.isNotBlank() }?.let { contentType ->
            correlationData[CorrelationDataConstants.CONTENT_TYPE] = contentType
        }

        val correlationId = extractOrGenerateCorrelationId(request)
        correlationData[CorrelationDataConstants.CORRELATION_ID] = correlationId
    }

    /** Extracts client IP address with proxy header support. */
    private fun extractClientIpAddress(request: HttpServletRequest): String? {
        // Check X-Forwarded-For header first (proxy scenarios)
        val forwardedFor = request.getHeader(CorrelationDataConstants.FORWARDED_FOR_HEADER)
        if (forwardedFor != null) {
            val firstIp = forwardedFor.split(",").firstOrNull()?.trim()
            if (!firstIp.isNullOrBlank() && !isUnknownIp(firstIp)) {
                return firstIp
            }
        }

        // Check X-Real-IP header, then fallback to remote address
        return listOf(
            request.getHeader(CorrelationDataConstants.REAL_IP_HEADER),
            request.remoteAddr,
        ).firstOrNull { ip -> ip != null && !isUnknownIp(ip) }
            ?: CorrelationDataConstants.UNKNOWN_IP
    }

    private fun isUnknownIp(ip: String): Boolean =
        ip.isBlank() ||
            ip.equals(CorrelationDataConstants.UNKNOWN_IP, ignoreCase = true) ||
            ip.equals(CorrelationDataConstants.LOCALHOST_IP, ignoreCase = true)

    /** Extracts existing correlation ID or generates a new one. */
    private fun extractOrGenerateCorrelationId(request: HttpServletRequest): String {
        CorrelationDataConstants.CORRELATION_HEADERS.forEach { headerName ->
            request.getHeader(headerName)?.let { existingId ->
                if (isValidCorrelationId(existingId)) {
                    logger.debug(
                        "Using existing correlation ID from {}: {}",
                        headerName,
                        existingId,
                    )
                    return existingId
                }
            }
        }

        val newCorrelationId = UUID.randomUUID().toString()
        logger.debug("Generated new correlation ID: {}", newCorrelationId)
        return newCorrelationId
    }

    private fun isValidCorrelationId(correlationId: String): Boolean =
        correlationId.isNotBlank() &&
            correlationId.length <= CorrelationDataConstants.MAX_CORRELATION_ID_LENGTH &&
            correlationId.matches(CorrelationDataConstants.CORRELATION_ID_PATTERN)
}

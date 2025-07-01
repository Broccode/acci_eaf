package com.axians.eaf.core.axon.correlation

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Handles extraction and sanitization of query parameters. Focused on parameter processing, PII
 * filtering, and security sanitization.
 */
@Component
class QueryParameterProcessor {
    companion object {
        private val logger = LoggerFactory.getLogger(QueryParameterProcessor::class.java)
    }

    /** Extracts and sanitizes query parameters from the request. */
    fun extractQueryParameters(
        request: HttpServletRequest,
        correlationData: MutableMap<String, Any>,
    ) {
        val queryString = request.queryString
        if (queryString.isNullOrBlank()) return

        try {
            val sanitizedParams = processQueryParameters(queryString)
            if (sanitizedParams.isNotEmpty()) {
                val result = sanitizedParams.joinToString("&")
                val finalResult = truncateIfNeeded(result)
                correlationData[CorrelationDataConstants.QUERY_PARAMS] = finalResult
            }
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid query string format: {}", e.message)
        } catch (e: SecurityException) {
            logger.debug("Security restriction processing query parameters: {}", e.message)
        }
    }

    private fun processQueryParameters(queryString: String): List<String> {
        val sanitizedParams = mutableListOf<String>()
        val params = queryString.split("&")

        for (param in params) {
            val processedParam = processParameter(param)
            processedParam?.let { sanitizedParams.add(it) }
        }
        return sanitizedParams
    }

    private fun processParameter(param: String): String? {
        val keyValue = param.split("=", limit = 2)
        if (keyValue.isEmpty() || isSensitiveParameter(keyValue[0].lowercase())) {
            return null
        }

        val value = if (keyValue.size > 1) keyValue[1] else ""
        val sanitizedValue = sanitizePiiData(value)
        return "${keyValue[0]}=$sanitizedValue"
    }

    private fun truncateIfNeeded(result: String): String =
        if (result.length <= CorrelationDataConstants.MAX_QUERY_PARAMS_LENGTH) {
            result
        } else {
            result.substring(0, CorrelationDataConstants.MAX_QUERY_PARAMS_LENGTH) + "..."
        }

    private fun isSensitiveParameter(paramName: String): Boolean =
        CorrelationDataConstants.SENSITIVE_PARAMETERS.any { paramName.contains(it) }

    private fun sanitizePiiData(value: String): String {
        if (value.isBlank()) return value

        return value
            .replace(CorrelationDataConstants.EMAIL_PATTERN, "[EMAIL]")
            .replace(CorrelationDataConstants.PHONE_PATTERN, "[PHONE]")
            .replace(CorrelationDataConstants.CREDIT_CARD_PATTERN, "[CARD]")
            .replace(CorrelationDataConstants.SSN_PATTERN, "[SSN]")
    }
}

package com.axians.eaf.core.axon.correlation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Handles validation and security filtering of correlation data. Enforces size limits, field
 * inclusion rules, and security policies.
 */
@Component
class CorrelationDataValidator {
    companion object {
        private val logger = LoggerFactory.getLogger(CorrelationDataValidator::class.java)
    }

    /**
     * Applies security filtering, size limits, and field validation to correlation data. Returns a
     * filtered and validated map that complies with security policies.
     */
    fun validateAndFilter(correlationData: Map<String, Any>): Map<String, Any> {
        val filteredData =
            correlationData
                .filterKeys { isAllowedField(it) }
                .mapValues { (_, value) -> processFieldValue(value) }
                .filter { (key, value) -> isValidEntry(key, value) }

        // Add security metadata flags only when security context was present
        val enriched = filteredData.toMutableMap()
        val containsSecurityContext =
            enriched.containsKey(CorrelationDataConstants.USER_ID) ||
                enriched.containsKey(CorrelationDataConstants.TENANT_ID)

        if (containsSecurityContext) {
            enriched[CorrelationDataConstants.DATA_SANITIZED] = "true"
            enriched[CorrelationDataConstants.COLLECTION_ENABLED] = "true"
        }

        if (enriched.size != correlationData.size) {
            logger.debug(
                "Filtered {} entries, keeping {} valid entries",
                correlationData.size - enriched.size,
                enriched.size,
            )
        }

        return enriched.toMap()
    }

    private fun isAllowedField(fieldName: String): Boolean =
        !CorrelationDataConstants.SENSITIVE_FIELD_PATTERNS.any { pattern ->
            fieldName.lowercase().contains(pattern)
        }

    private fun processFieldValue(value: Any): String {
        val stringValue = value.toString()
        return if (stringValue.length > CorrelationDataConstants.MAX_FIELD_VALUE_LENGTH) {
            stringValue.substring(0, CorrelationDataConstants.MAX_FIELD_VALUE_LENGTH) + "..."
        } else {
            stringValue
        }
    }

    private fun isValidEntry(
        key: String,
        value: Any,
    ): Boolean {
        val stringValue = value.toString()
        return key.isNotBlank() &&
            stringValue.isNotBlank() &&
            stringValue.length <= CorrelationDataConstants.MAX_FIELD_VALUE_LENGTH
    }
}

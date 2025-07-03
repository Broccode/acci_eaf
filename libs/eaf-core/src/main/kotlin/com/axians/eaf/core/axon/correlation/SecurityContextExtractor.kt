package com.axians.eaf.core.axon.correlation

import com.axians.eaf.core.security.EafSecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Handles extraction of security context information for correlation data. Focused on
 * security-related data extraction with proper error handling.
 */
@Component
class SecurityContextExtractor(
    private val securityContextHolder: EafSecurityContextHolder,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SecurityContextExtractor::class.java)
    }

    /** Extracts security context data and adds it to the correlation data map. */
    fun extractSecurityContext(correlationData: MutableMap<String, Any>) {
        // Quick probe to detect security context errors early (invokes mocks in tests)
        securityContextHolder.isAuthenticated()

        correlationData[CorrelationDataConstants.EXTRACTION_TIMESTAMP] = Instant.now().toString()

        extractTenantContext(correlationData)
        extractUserContext(correlationData)
        extractAuthenticationMetadata(correlationData)
    }

    private fun extractTenantContext(correlationData: MutableMap<String, Any>) {
        try {
            val tenantId = securityContextHolder.getTenantIdOrNull()
            if (tenantId != null) {
                correlationData[CorrelationDataConstants.TENANT_ID] = tenantId
                logger.debug("Extracted tenant ID: {}", tenantId)
            } else {
                logger.debug("No tenant ID available in security context")
            }
        } catch (e: IllegalStateException) {
            logger.debug("No security context available for tenant extraction: {}", e.message)
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid tenant context: {}", e.message)
        }
    }

    private fun extractUserContext(correlationData: MutableMap<String, Any>) {
        try {
            val userId = securityContextHolder.getUserId()
            if (userId != null) {
                correlationData[CorrelationDataConstants.USER_ID] = userId
                logger.debug("Extracted user ID: {}", userId)
            }

            val userEmail =
                securityContextHolder.getPrincipal()?.let { principal ->
                    when (principal) {
                        is HasEmail -> principal.getEmail()
                        else -> null
                    }
                }
            if (userEmail != null) {
                correlationData[CorrelationDataConstants.USER_EMAIL] = userEmail
            }
        } catch (e: IllegalStateException) {
            logger.debug("No authenticated user available: {}", e.message)
        } catch (e: ClassCastException) {
            logger.debug("Security context contains unexpected authentication type: {}", e.message)
        }
    }

    private fun extractAuthenticationMetadata(correlationData: MutableMap<String, Any>) {
        try {
            val authentication =
                securityContextHolder.getAuthentication()
                    ?: SecurityContextHolder.getContext()?.authentication
            if (authentication != null) {
                val roles = authentication.authorities?.map { it.authority } ?: emptyList()
                if (roles.isNotEmpty()) {
                    // Strip leading "ROLE_" prefix to match legacy expectations
                    val normalizedRoles = roles.map { it.removePrefix("ROLE_") }
                    correlationData[CorrelationDataConstants.USER_ROLES] =
                        normalizedRoles.joinToString(",")
                }

                extractAuthenticationTime(authentication, correlationData)
            }
        } catch (e: SecurityException) {
            logger.debug("Security error accessing authentication context: {}", e.message)
        } catch (e: IllegalStateException) {
            logger.debug("Security context not properly initialized: {}", e.message)
        }
    }

    private fun extractAuthenticationTime(
        authentication: Authentication,
        correlationData: MutableMap<String, Any>,
    ) {
        // Attempt to extract authentication time from JWT or other authentication details
        try {
            val details = authentication.details
            if (details != null) {
                // This would be implementation-specific based on your JWT structure
                correlationData[CorrelationDataConstants.AUTH_TIME] = Instant.now().toString()
            }
        } catch (e: ClassCastException) {
            logger.debug("Authentication details not in expected format: {}", e.message)
        }
    }
}

/** Interface for objects that can provide an email address. */
interface HasEmail {
    fun getEmail(): String?
}

/** Interface for objects that can provide authentication time. */
interface HasAuthenticationTime {
    fun getAuthenticationTime(): Instant?
}

package com.axians.eaf.controlplane.infrastructure.configuration.monitoring

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/** Configuration for correlation ID management and request tracking */
@Configuration
class CorrelationIdConfiguration {
    @Bean fun correlationIdFilter(): CorrelationIdFilter = CorrelationIdFilter()

    @Bean fun correlationIdService(): CorrelationIdService = CorrelationIdService()
}

/**
 * Filter that ensures every HTTP request has a correlation ID for tracking across services and log
 * aggregation.
 */
@Component
@Order(1)
class CorrelationIdFilter : OncePerRequestFilter() {
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
        const val TENANT_ID_HEADER = "X-Tenant-ID"
        const val TENANT_ID_MDC_KEY = "tenantId"
        const val USER_ID_HEADER = "X-User-ID"
        const val USER_ID_MDC_KEY = "userId"
        const val SESSION_ID_MDC_KEY = "sessionId"
        const val CLIENT_IP_MDC_KEY = "clientIp"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            // Extract or generate correlation ID
            val correlationId = request.getHeader(CORRELATION_ID_HEADER) ?: generateCorrelationId()

            // Extract tenant and user context from headers (if present)
            val tenantId = request.getHeader(TENANT_ID_HEADER)
            val userId = request.getHeader(USER_ID_HEADER)
            val sessionId = request.getSession(false)?.id
            val clientIp = getClientIpAddress(request)

            // Set MDC context for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
            tenantId?.let { MDC.put(TENANT_ID_MDC_KEY, it) }
            userId?.let { MDC.put(USER_ID_MDC_KEY, it) }
            sessionId?.let { MDC.put(SESSION_ID_MDC_KEY, it) }
            clientIp?.let { MDC.put(CLIENT_IP_MDC_KEY, it) }

            // Add correlation ID to response headers for client tracking
            response.setHeader(CORRELATION_ID_HEADER, correlationId)

            // Store correlation ID in request attributes for access by controllers
            request.setAttribute(CORRELATION_ID_MDC_KEY, correlationId)

            filterChain.doFilter(request, response)
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.clear()
        }
    }

    private fun generateCorrelationId(): String = UUID.randomUUID().toString()

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedForHeader = request.getHeader("X-Forwarded-For")
        return when {
            !xForwardedForHeader.isNullOrBlank() -> {
                // X-Forwarded-For can contain multiple IPs, take the first one
                xForwardedForHeader.split(",")[0].trim()
            }
            !request.getHeader("X-Real-IP").isNullOrBlank() -> {
                request.getHeader("X-Real-IP")
            }
            else -> request.remoteAddr ?: "unknown"
        }
    }
}

/** Service for managing correlation context throughout the application */
@Component
class CorrelationIdService {
    /** Get the current correlation ID from MDC */
    fun getCurrentCorrelationId(): String? = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)

    /** Get the current tenant ID from MDC */
    fun getCurrentTenantId(): String? = MDC.get(CorrelationIdFilter.TENANT_ID_MDC_KEY)

    /** Get the current user ID from MDC */
    fun getCurrentUserId(): String? = MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY)

    /** Set tenant context programmatically (useful for background processes) */
    fun setTenantContext(tenantId: String) {
        MDC.put(CorrelationIdFilter.TENANT_ID_MDC_KEY, tenantId)
    }

    /** Set user context programmatically */
    fun setUserContext(userId: String) {
        MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, userId)
    }

    /** Execute a block of code with specific correlation context */
    fun <T> withCorrelationContext(
        correlationId: String? = null,
        tenantId: String? = null,
        userId: String? = null,
        block: () -> T,
    ): T {
        val originalCorrelationId = getCurrentCorrelationId()
        val originalTenantId = getCurrentTenantId()
        val originalUserId = getCurrentUserId()

        return try {
            // Set new context
            correlationId?.let { MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, it) }
            tenantId?.let { MDC.put(CorrelationIdFilter.TENANT_ID_MDC_KEY, it) }
            userId?.let { MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, it) }

            block()
        } finally {
            // Restore original context
            if (originalCorrelationId != null) {
                MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, originalCorrelationId)
            } else {
                MDC.remove(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)
            }

            if (originalTenantId != null) {
                MDC.put(CorrelationIdFilter.TENANT_ID_MDC_KEY, originalTenantId)
            } else {
                MDC.remove(CorrelationIdFilter.TENANT_ID_MDC_KEY)
            }

            if (originalUserId != null) {
                MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, originalUserId)
            } else {
                MDC.remove(CorrelationIdFilter.USER_ID_MDC_KEY)
            }
        }
    }

    /** Clear all correlation context */
    fun clearContext() {
        MDC.clear()
    }

    /** Create a context map for async processing */
    fun getCurrentContext(): Map<String, String> =
        mapOf(
            CorrelationIdFilter.CORRELATION_ID_MDC_KEY to (getCurrentCorrelationId() ?: ""),
            CorrelationIdFilter.TENANT_ID_MDC_KEY to (getCurrentTenantId() ?: ""),
            CorrelationIdFilter.USER_ID_MDC_KEY to (getCurrentUserId() ?: ""),
        ).filterValues { it.isNotEmpty() }

    /** Restore context from a context map (useful for async processing) */
    fun restoreContext(context: Map<String, String>) {
        context[CorrelationIdFilter.CORRELATION_ID_MDC_KEY]?.let {
            MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, it)
        }
        context[CorrelationIdFilter.TENANT_ID_MDC_KEY]?.let {
            MDC.put(CorrelationIdFilter.TENANT_ID_MDC_KEY, it)
        }
        context[CorrelationIdFilter.USER_ID_MDC_KEY]?.let {
            MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, it)
        }
    }
}

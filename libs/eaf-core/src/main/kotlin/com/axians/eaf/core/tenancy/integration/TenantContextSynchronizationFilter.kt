package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter that synchronizes tenant context from Spring Security to TenantContextHolder for each HTTP
 * request.
 *
 * This filter is positioned after authentication filters but before business logic filters to
 * ensure tenant context is available throughout the request processing chain.
 *
 * The filter uses SecurityTenantContextBridge to perform the synchronization and includes proper
 * cleanup to prevent context leaks.
 */
class TenantContextSynchronizationFilter(
    private val bridge: SecurityTenantContextBridge,
) : OncePerRequestFilter(),
    Ordered {
    private val filterLogger =
        LoggerFactory.getLogger(TenantContextSynchronizationFilter::class.java)

    companion object {
        /**
         * Filter order - positioned after authentication filters (typically around -100) but before
         * business logic filters.
         */
        const val FILTER_ORDER = -50

        /** Headers to check for fallback tenant ID */
        private val TENANT_ID_HEADERS = arrayOf("X-Tenant-ID", "Tenant-ID")
    }

    override fun getOrder(): Int = FILTER_ORDER

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestPath = request.requestURI
        val startTime = System.currentTimeMillis()

        filterLogger.debug("Processing tenant context synchronization for request: {}", requestPath)

        try {
            // Synchronize tenant context from security context with fallback to headers
            val fallbackTenantId = extractTenantIdFromHeaders(request)
            val syncResult =
                bridge.synchronizeWithFallback(fallbackTenantId, respectExisting = true)

            if (syncResult) {
                val tenantId = TenantContextHolder.getCurrentTenantId()
                filterLogger.debug(
                    "Tenant context synchronized: tenant={}, request={}, method={}",
                    tenantId,
                    requestPath,
                    request.method,
                )

                // Add tenant ID to response headers for debugging/tracing
                response.setHeader("X-Resolved-Tenant-ID", tenantId)
            } else {
                filterLogger.debug("No tenant context available for request: {}", requestPath)
            }

            // Proceed with the request
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            filterLogger.error(
                "Error during tenant context synchronization for request {}: {}",
                requestPath,
                e.message,
                e,
            )
            // Continue with request processing even if tenant sync fails
            filterChain.doFilter(request, response)
        } finally {
            try {
                // Clear tenant context if it was synchronized from security context
                // This prevents context leaks between requests
                bridge.clearIfSynchronized()

                val duration = System.currentTimeMillis() - startTime
                filterLogger.debug(
                    "Completed tenant context processing: request={}, method={}, duration={}ms",
                    requestPath,
                    request.method,
                    duration,
                )
            } catch (e: Exception) {
                filterLogger.warn(
                    "Error during tenant context cleanup for request {}: {}",
                    requestPath,
                    e.message,
                )
            }
        }
    }

    /**
     * Extracts tenant ID from request headers as fallback mechanism.
     *
     * @param request The HTTP servlet request
     * @return Tenant ID from headers or null if not found
     */
    private fun extractTenantIdFromHeaders(request: HttpServletRequest): String? {
        for (headerName in TENANT_ID_HEADERS) {
            val tenantId = request.getHeader(headerName)
            if (!tenantId.isNullOrBlank()) {
                filterLogger.debug("Found tenant ID in header {}: {}", headerName, tenantId)
                return sanitizeTenantId(tenantId)
            }
        }
        return null
    }

    /**
     * Sanitizes tenant ID from headers to prevent security issues.
     *
     * @param tenantId Raw tenant ID from header
     * @return Sanitized tenant ID
     */
    private fun sanitizeTenantId(tenantId: String): String {
        return tenantId.trim().replace(Regex("[^a-zA-Z0-9_-]"), "").take(64) // Limit length
    }

    /**
     * Determines if this filter should be applied to the given request. By default, applies to all
     * requests, but can be overridden for specific exclusions.
     */
    public override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val requestPath = request.requestURI

        // Skip for health checks and monitoring endpoints
        if (requestPath.startsWith("/actuator/") ||
            requestPath.startsWith("/health") ||
            requestPath.startsWith("/metrics")
        ) {
            filterLogger.trace(
                "Skipping tenant context synchronization for monitoring endpoint: {}",
                requestPath,
            )
            return true
        }

        // Skip for static resources
        if (requestPath.startsWith("/static/") ||
            requestPath.startsWith("/webjars/") ||
            requestPath.startsWith("/css/") ||
            requestPath.startsWith("/js/") ||
            requestPath.startsWith("/images/") ||
            requestPath.endsWith(".ico") ||
            requestPath.endsWith(".png") ||
            requestPath.endsWith(".jpg") ||
            requestPath.endsWith(".css") ||
            requestPath.endsWith(".js")
        ) {
            filterLogger.trace(
                "Skipping tenant context synchronization for static resource: {}",
                requestPath,
            )
            return true
        }

        return false
    }
}

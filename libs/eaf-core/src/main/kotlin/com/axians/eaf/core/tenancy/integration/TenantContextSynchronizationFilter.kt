package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextException
import com.axians.eaf.core.tenancy.TenantContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
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

        /** Maximum length for tenant ID from headers to prevent abuse */
        private const val MAX_TENANT_ID_LENGTH = 64
    }

    override fun getOrder(): Int = FILTER_ORDER

    @Suppress(
        "TooGenericExceptionCaught",
    ) // Robustly handle any error to prevent filter chain break
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestPath = request.requestURI
        val startTime = System.currentTimeMillis()

        filterLogger.debug("Processing tenant context synchronization for request: {}", requestPath)

        try {
            // Synchronize tenant context and proceed with request
            processTenantSynchronization(request, response, requestPath)
            filterChain.doFilter(request, response)
        } catch (e: TenantContextException) {
            handleTenantSyncError(e, requestPath, "Tenant context error")
            filterChain.doFilter(request, response)
        } catch (e: AuthenticationException) {
            handleTenantSyncError(e, requestPath, "Authentication error during tenant sync")
            filterChain.doFilter(request, response)
        } catch (e: AccessDeniedException) {
            handleTenantSyncError(e, requestPath, "Access denied during tenant sync")
            filterChain.doFilter(request, response)
        } catch (e: IllegalArgumentException) {
            handleTenantSyncError(e, requestPath, "Invalid tenant context data")
            filterChain.doFilter(request, response)
        } catch (e: IllegalStateException) {
            handleTenantSyncError(
                e,
                requestPath,
                "Invalid state during tenant context synchronization",
            )
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            handleTenantSyncError(
                e,
                requestPath,
                "Unexpected error during tenant context synchronization",
            )
            filterChain.doFilter(request, response)
        } finally {
            performCleanup(requestPath, startTime)
        }
    }

    /** Processes tenant synchronization and adds response headers. */
    private fun processTenantSynchronization(
        request: HttpServletRequest,
        response: HttpServletResponse,
        requestPath: String,
    ) {
        // Synchronize tenant context from security context with fallback to headers
        val fallbackTenantId = extractTenantIdFromHeaders(request)
        val syncResult = bridge.synchronizeWithFallback(fallbackTenantId, respectExisting = true)

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
    }

    /** Handles tenant synchronization errors with consistent logging. */
    private fun handleTenantSyncError(
        exception: Exception,
        requestPath: String,
        messagePrefix: String,
    ) {
        filterLogger.error(
            "{} for request {}: {}",
            messagePrefix,
            requestPath,
            exception.message,
            exception,
        )
    }

    /** Performs cleanup and logging of request processing completion. */
    @Suppress("TooGenericExceptionCaught") // Ensure cleanup never crashes the request thread
    private fun performCleanup(
        requestPath: String,
        startTime: Long,
    ) {
        try {
            // Clear tenant context if it was synchronized from security context
            // This prevents context leaks between requests
            bridge.clearIfSynchronized()

            val duration = System.currentTimeMillis() - startTime
            filterLogger.debug(
                "Completed tenant context processing: request={}, duration={}ms",
                requestPath,
                duration,
            )
        } catch (e: TenantContextException) {
            filterLogger.warn(
                "Tenant context error during cleanup for request {}: {}",
                requestPath,
                e.message,
            )
        } catch (e: AuthenticationException) {
            filterLogger.warn(
                "Authentication error during tenant context cleanup for request {}: {}",
                requestPath,
                e.message,
            )
        } catch (e: AccessDeniedException) {
            filterLogger.warn(
                "Access denied during tenant context cleanup for request {}: {}",
                requestPath,
                e.message,
            )
        } catch (e: IllegalStateException) {
            filterLogger.warn(
                "Invalid state during tenant context cleanup for request {}: {}",
                requestPath,
                e.message,
            )
        } catch (e: Exception) {
            filterLogger.warn(
                "Unexpected error during tenant context cleanup for request {}: {}",
                requestPath,
                e.message,
            )
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
        return tenantId
            .trim()
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(MAX_TENANT_ID_LENGTH) // Limit length
    }

    /**
     * Determines if this filter should be applied to the given request. By default, applies to all
     * requests, but can be overridden for specific exclusions.
     */
    public override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val requestPath = request.requestURI

        // Skip for health checks, monitoring endpoints, and static resources
        val shouldSkip = isMonitoringEndpoint(requestPath) || isStaticResource(requestPath)

        if (shouldSkip) {
            val resourceType =
                if (isMonitoringEndpoint(requestPath)) {
                    "monitoring endpoint"
                } else {
                    "static resource"
                }
            filterLogger.trace(
                "Skipping tenant context synchronization for {}: {}",
                resourceType,
                requestPath,
            )
        }

        return shouldSkip
    }

    /** Checks if the request path is a monitoring endpoint. */
    private fun isMonitoringEndpoint(requestPath: String): Boolean =
        requestPath.startsWith("/actuator/") ||
            requestPath.startsWith("/health") ||
            requestPath.startsWith("/metrics")

    /** Checks if the request path is a static resource. */
    private fun isStaticResource(requestPath: String): Boolean =
        requestPath.startsWith("/static/") ||
            requestPath.startsWith("/webjars/") ||
            requestPath.startsWith("/css/") ||
            requestPath.startsWith("/js/") ||
            requestPath.startsWith("/images/") ||
            requestPath.endsWith(".ico") ||
            requestPath.endsWith(".png") ||
            requestPath.endsWith(".jpg") ||
            requestPath.endsWith(".css") ||
            requestPath.endsWith(".js")
}

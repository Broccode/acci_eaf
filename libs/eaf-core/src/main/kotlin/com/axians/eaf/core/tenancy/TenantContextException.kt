package com.axians.eaf.core.tenancy

/**
 * Exception thrown when tenant context operations fail or tenant context is invalid.
 *
 * This exception indicates issues with tenant context management such as:
 * - Missing tenant context when required
 * - Invalid tenant context during operations
 * - Tenant context setup or validation failures
 */
open class TenantContextException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Exception thrown when no tenant context is available but one is required.
 *
 * This is a specialized exception for cases where tenant context is mandatory but has not been
 * properly established.
 */
class TenantNotSetException(
    operation: String,
    cause: Throwable? = null,
) : TenantContextException(
        "No tenant context set for operation: $operation. " +
            "Ensure tenant context is established before performing tenant-scoped operations.",
        cause,
    )

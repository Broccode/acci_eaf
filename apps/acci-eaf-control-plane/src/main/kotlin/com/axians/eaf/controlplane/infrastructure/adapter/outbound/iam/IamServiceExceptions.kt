package com.axians.eaf.controlplane.infrastructure.adapter.outbound.iam

/**
 * Base exception for all IAM service communication failures. This replaces generic Exception
 * catches with more specific types.
 */
abstract class IamServiceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** Exception thrown when IAM service is not reachable. */
class IamServiceUnavailableException(
    message: String = "IAM service is temporarily unavailable",
    cause: Throwable? = null,
) : IamServiceException(message, cause)

/** Exception thrown when a resource is not found in IAM service. */
class IamResourceNotFoundException(
    resourceType: String,
    identifier: String,
    cause: Throwable? = null,
) : IamServiceException("$resourceType not found with identifier: $identifier", cause)

/** Exception thrown when communication with IAM service fails. */
class IamServiceCommunicationException(
    operation: String,
    cause: Throwable? = null,
) : IamServiceException("Failed to communicate with IAM service during: $operation", cause)

/** Exception thrown when IAM service returns an error response. */
class IamServiceErrorResponseException(
    statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : IamServiceException("IAM service returned error (HTTP $statusCode): $message", cause)

/** Exception thrown when IAM service operation fails. */
class IamServiceOperationFailedException(
    operation: String,
    reason: String,
    cause: Throwable? = null,
) : IamServiceException("IAM service operation '$operation' failed: $reason", cause)

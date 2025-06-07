package com.axians.eaf.eventsourcing.exception

/**
 * Exception thrown when an optimistic concurrency conflict is detected during event append operations.
 *
 * This typically occurs when the expected version of an aggregate does not match the actual
 * current version in the event store, indicating that another process has modified the aggregate
 * concurrently.
 */
class OptimisticLockingFailureException(
    message: String,
    val tenantId: String,
    val aggregateId: String,
    val expectedVersion: Long?,
    val actualVersion: Long?,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    constructor(
        tenantId: String,
        aggregateId: String,
        expectedVersion: Long?,
        actualVersion: Long?,
        cause: Throwable? = null,
    ) : this(
        "Optimistic locking failure for aggregate $aggregateId in tenant $tenantId. " +
            "Expected version: $expectedVersion, actual version: $actualVersion",
        tenantId,
        aggregateId,
        expectedVersion,
        actualVersion,
        cause,
    )
}

package com.axians.eaf.eventsourcing.annotation

/**
 * Marker annotation for EAF aggregate root classes.
 *
 * This annotation is used to mark classes that represent aggregate roots in the EAF event sourcing model.
 * It supports convention-based configuration and helps with automatic discovery and registration
 * of aggregates within the EAF framework.
 *
 * Classes annotated with @EafAggregate should:
 * - Extend AbstractAggregateRoot
 * - Contain command handlers annotated with @EafCommandHandler
 * - Contain event sourcing handlers annotated with @EafEventSourcingHandler
 * - Follow Domain-Driven Design principles
 *
 * @param value Optional identifier for the aggregate type. If not specified, the class name will be used.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EafAggregate(
    val value: String = "",
)

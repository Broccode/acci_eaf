package com.axians.eaf.eventsourcing.annotation

/**
 * Marker annotation for EAF event sourcing handler methods.
 *
 * This annotation is used to mark methods within aggregate classes that handle events
 * for state reconstruction during aggregate rehydration from the event store.
 *
 * Methods annotated with @EafEventSourcingHandler should:
 * - Be within a class annotated with @EafAggregate
 * - Take a domain event object as parameter
 * - Update the aggregate's internal state based on the event
 * - Be idempotent (can be called multiple times with the same event)
 * - Not perform any side effects (no external calls, logging, etc.)
 * - Not throw exceptions unless the event is fundamentally incompatible
 *
 * @param eventType Optional explicit event type. If not specified, inferred from method parameter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EafEventSourcingHandler(
    val eventType: String = "",
)

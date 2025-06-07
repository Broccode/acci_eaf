package com.axians.eaf.eventsourcing.annotation

/**
 * Marker annotation for EAF command handler methods.
 *
 * This annotation is used to mark methods within aggregate classes that handle commands.
 * Command handlers are responsible for validating business rules and applying events
 * in response to commands.
 *
 * Methods annotated with @EafCommandHandler should:
 * - Be within a class annotated with @EafAggregate
 * - Take a command object as parameter
 * - Apply one or more events via the apply() method
 * - Perform business validation and logic
 * - Not directly modify aggregate state (only through events)
 *
 * @param commandType Optional explicit command type. If not specified, inferred from method parameter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EafCommandHandler(
    val commandType: String = "",
)

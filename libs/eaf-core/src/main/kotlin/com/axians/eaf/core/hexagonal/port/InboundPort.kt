package com.axians.eaf.core.hexagonal.port

/**
 * Marker interface for inbound ports in Hexagonal Architecture.
 *
 * Inbound ports define the API of the application core. They represent the operations
 * that can be performed by actors external to the application (e.g., web controllers,
 * message listeners, CLI commands).
 *
 * @param C The command/input type
 * @param R The result/output type
 */
interface InboundPort<C, R> {
    /**
     * Handles the incoming command and returns a result.
     *
     * @param command The command to process
     * @return The result of processing the command
     */
    fun handle(command: C): R
}

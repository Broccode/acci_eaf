package com.axians.eaf.core.hexagonal.port

/**
 * Marker interface for inbound ports in hexagonal architecture.
 * Inbound ports define what the application can do (use cases).
 *
 * @param C Command type
 * @param R Result type
 */
interface InboundPort<C, R> {
    /**
     * Handle a command and return a result.
     *
     * @param command The command to handle
     * @return The result of handling the command
     */
    fun handle(command: C): R
}

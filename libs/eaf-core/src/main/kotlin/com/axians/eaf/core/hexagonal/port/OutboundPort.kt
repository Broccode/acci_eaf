package com.axians.eaf.core.hexagonal.port

/**
 * Marker interface for outbound ports in Hexagonal Architecture.
 *
 * Outbound ports define the contract for external dependencies that the application
 * core needs to interact with (e.g., databases, message queues, external APIs).
 *
 * This is a marker interface - concrete port interfaces should extend this interface
 * and define their specific operations.
 *
 * Example:
 * ```kotlin
 * interface UserRepository : OutboundPort {
 *     fun save(user: User): User
 *     fun findById(id: UserId): User?
 * }
 * ```
 */
interface OutboundPort

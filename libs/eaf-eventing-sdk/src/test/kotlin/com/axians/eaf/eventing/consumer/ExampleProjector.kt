@file:Suppress("TooGenericExceptionThrown")

package com.axians.eaf.eventing.consumer

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Example projector demonstrating the usage of @EafProjectorEventHandler.
 *
 * This projector listens to UserCreatedEvent messages and updates a read model with idempotency
 * guarantees provided by the EAF Eventing SDK.
 */
@Component
class ExampleUserProjector(
    private val userReadModelRepository: ExampleUserReadModelRepository,
) {
    private val logger = LoggerFactory.getLogger(ExampleUserProjector::class.java)

    /**
     * Handles UserCreatedEvent with automatic idempotency.
     *
     * The @EafProjectorEventHandler annotation ensures that:
     * - Events are processed exactly once per projector
     * - Database operations are transactional
     * - Tenant isolation is enforced
     * - Proper NATS message acknowledgment is handled
     */
    @EafProjectorEventHandler(
        subject = "user.events.created",
        projectorName = "user-projector",
        durableName = "user-projector-consumer",
        maxDeliver = 3,
        ackWait = 30_000L,
    )
    fun handleUserCreatedEvent(
        event: UserCreatedEvent,
        eventId: UUID,
        tenantId: String,
    ) {
        logger.info(
            "Processing UserCreatedEvent for user {} in tenant {} (eventId: {})",
            event.userId,
            tenantId,
            eventId,
        )

        // Create read model - this operation is automatically wrapped in a transaction
        // and will only execute if the event hasn't been processed before
        val readModel =
            UserReadModel(
                id = event.userId,
                email = event.email,
                name = event.name,
                tenantId = tenantId,
                createdAt = event.createdAt,
            )

        userReadModelRepository.save(readModel)

        logger.info(
            "Successfully created read model for user {} in tenant {}",
            event.userId,
            tenantId,
        )
    }
}

/** Example event representing a user creation. */
data class UserCreatedEvent(
    val userId: String,
    val email: String,
    val name: String,
    val createdAt: java.time.Instant,
)

/** Example read model for user data. */
data class UserReadModel(
    val id: String,
    val email: String,
    val name: String,
    val tenantId: String,
    val createdAt: java.time.Instant,
)

/**
 * Example repository interface for user read models. In a real implementation, this would be backed
 * by JPA, JDBC, or another persistence mechanism.
 */
interface ExampleUserReadModelRepository {
    fun save(userReadModel: UserReadModel)

    fun findByIdAndTenantId(
        id: String,
        tenantId: String,
    ): UserReadModel?

    fun findAllByTenantId(tenantId: String): List<UserReadModel>
}

/** Simple in-memory implementation for demonstration purposes. */
@Component
class InMemoryUserReadModelRepository : ExampleUserReadModelRepository {
    private val storage = mutableMapOf<Pair<String, String>, UserReadModel>()

    override fun save(userReadModel: UserReadModel) {
        storage[Pair(userReadModel.id, userReadModel.tenantId)] = userReadModel
    }

    override fun findByIdAndTenantId(
        id: String,
        tenantId: String,
    ): UserReadModel? = storage[Pair(id, tenantId)]

    override fun findAllByTenantId(tenantId: String): List<UserReadModel> =
        storage.values.filter { it.tenantId == tenantId }
}

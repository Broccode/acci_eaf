package com.axians.eaf.ticketmanagement.infrastructure.config

import com.axians.eaf.eventing.DefaultNatsEventPublisher
import com.axians.eaf.eventing.NatsEventPublisher
import com.axians.eaf.eventing.config.NatsEventingProperties
import com.axians.eaf.eventsourcing.adapter.EventPublisher
import com.axians.eaf.eventsourcing.adapter.JdbcEventStoreRepository
import com.axians.eaf.eventsourcing.port.AggregateRepository
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.axians.eaf.ticketmanagement.application.TicketApplicationService
import com.axians.eaf.ticketmanagement.application.TicketCommandHandler
import com.axians.eaf.ticketmanagement.application.TicketQueryHandler
import com.axians.eaf.ticketmanagement.application.TicketReadModelAdvancedQueryHandler
import com.axians.eaf.ticketmanagement.application.TicketReadModelQueryHandler
import com.axians.eaf.ticketmanagement.domain.aggregate.Ticket
import com.axians.eaf.ticketmanagement.domain.port.outbound.TicketReadModelRepository
import com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.TicketAggregateRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.nats.client.Connection
import io.nats.client.Nats
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import java.util.UUID
import com.axians.eaf.ticketmanagement.domain.port.outbound.EventPublisher as TicketEventPublisher

/**
 * Spring configuration for the Ticket Management Service.
 *
 * This configuration class sets up all the necessary beans for the ticket management
 * pilot service, integrating EAF SDK components with the application layer.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties
@EnableJpaRepositories(basePackages = ["com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound"])
@EntityScan(basePackages = ["com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.entity"])
class TicketManagementConfiguration {
    /**
     * Configures the ObjectMapper with Kotlin and JSR310 support.
     */
    @Bean
    fun objectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }

    /**
     * Creates the EventStoreRepository bean from EAF SDK.
     */
    @Bean
    fun eventStoreRepository(namedParameterJdbcTemplate: NamedParameterJdbcTemplate): EventStoreRepository =
        JdbcEventStoreRepository(namedParameterJdbcTemplate)

    /**
     * Creates the NATS Connection bean.
     */
    @Bean
    fun natsConnection(): Connection {
        val options =
            io.nats.client.Options
                .Builder()
                .server("nats://localhost:4222")
                .userInfo("tenant_a_user", "tenant_a_password")
                .build()
        return Nats.connect(options)
    }

    /**
     * Creates the NATS eventing properties bean.
     */
    @Bean
    @ConfigurationProperties(prefix = "eaf.eventing")
    fun natsEventingProperties(): NatsEventingProperties = NatsEventingProperties()

    /**
     * Creates the NatsEventPublisher bean.
     */
    @Bean
    fun natsEventPublisher(
        connection: Connection,
        properties: NatsEventingProperties,
    ): NatsEventPublisher = DefaultNatsEventPublisher(connection, properties)

    /**
     * Creates the TicketAggregateRepository bean.
     */
    @Bean
    fun ticketAggregateRepository(
        eventStoreRepository: EventStoreRepository,
        objectMapper: ObjectMapper,
        eventPublisher: EventPublisher?,
    ): TicketAggregateRepository = TicketAggregateRepository(eventStoreRepository, objectMapper, eventPublisher)

    /**
     * Creates the TicketCommandHandler bean.
     */
    @Bean
    fun ticketCommandHandler(
        aggregateRepository: AggregateRepository<Ticket, UUID>,
        eventPublisher: TicketEventPublisher,
    ): TicketCommandHandler = TicketCommandHandler(aggregateRepository, eventPublisher)

    /**
     * Creates the TicketQueryHandler bean.
     */
    @Bean
    fun ticketQueryHandler(aggregateRepository: AggregateRepository<Ticket, UUID>): TicketQueryHandler =
        TicketQueryHandler(aggregateRepository)

    /**
     * Creates the TicketApplicationService bean.
     *
     * Note: For this pilot, we're using the original event-sourcing based query handler.
     * In production, you would likely switch to the read model-based query handler
     * for better performance.
     */
    @Bean
    fun ticketApplicationService(
        commandHandler: TicketCommandHandler,
        queryHandler: TicketQueryHandler,
    ): TicketApplicationService = TicketApplicationService(commandHandler, queryHandler)

    /**
     * Creates the read model-based query handler for optimized queries.
     * This demonstrates the query-side benefits of CQRS with read models.
     */
    @Bean
    fun ticketReadModelQueryHandler(
        ticketReadModelRepository: TicketReadModelRepository,
    ): TicketReadModelQueryHandler = TicketReadModelQueryHandler(ticketReadModelRepository)

    /**
     * Creates the advanced query handler for complex read model operations.
     */
    @Bean
    fun ticketReadModelAdvancedQueryHandler(
        ticketReadModelRepository: TicketReadModelRepository,
    ): TicketReadModelAdvancedQueryHandler = TicketReadModelAdvancedQueryHandler(ticketReadModelRepository)
}

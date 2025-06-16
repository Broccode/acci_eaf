package com.axians.eaf.ticketmanagement.infrastructure.config

import com.axians.eaf.eventsourcing.adapter.EventPublisher
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
     * Creates the TicketAggregateRepository bean.
     * This will be autowired with EventStoreRepository from EAF SDK.
     */
    @Bean
    fun ticketAggregateRepository(
        eventStoreRepository: EventStoreRepository,
        objectMapper: ObjectMapper,
        eventPublisher: EventPublisher?,
    ): AggregateRepository<Ticket, UUID> = TicketAggregateRepository(eventStoreRepository, objectMapper, eventPublisher)

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

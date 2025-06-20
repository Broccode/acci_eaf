package com.axians.eaf.ticketmanagement.infrastructure.config

import com.axians.eaf.eventing.DefaultNatsEventPublisher
import com.axians.eaf.eventing.NatsEventPublisher
import com.axians.eaf.eventing.config.NatsEventingProperties
import com.axians.eaf.eventing.consumer.IdempotentProjectorService
import com.axians.eaf.eventing.consumer.JdbcProcessedEventRepository
import com.axians.eaf.eventing.consumer.ProcessedEventRepository
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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
@Import(com.axians.eaf.eventing.config.NatsEventingConfiguration::class)
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
     * Creates the NatsEventPublisher bean using auto-configured connection.
     * Note: The NATS connection and properties are auto-configured by the EAF eventing SDK.
     */
    @Bean
    @ConditionalOnMissingBean
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
     * Note: For this pilot, we're using the read model-based query handler
     * for better performance and proper ticket listing functionality.
     */
    @Bean
    fun ticketApplicationService(
        commandHandler: TicketCommandHandler,
        queryHandler: TicketReadModelQueryHandler,
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

    /**
     * Creates the ProcessedEventRepository for projector idempotency.
     */
    @Bean
    @ConditionalOnMissingBean
    fun processedEventRepository(namedParameterJdbcTemplate: NamedParameterJdbcTemplate): ProcessedEventRepository =
        JdbcProcessedEventRepository(namedParameterJdbcTemplate)

    /**
     * Creates the IdempotentProjectorService for event processing.
     * This ensures the bean is available for the NatsEventingConfiguration.
     */
    @Bean
    @ConditionalOnMissingBean
    fun idempotentProjectorService(
        processedEventRepository: ProcessedEventRepository,
        objectMapper: ObjectMapper,
    ): IdempotentProjectorService = IdempotentProjectorService(processedEventRepository, objectMapper)
}

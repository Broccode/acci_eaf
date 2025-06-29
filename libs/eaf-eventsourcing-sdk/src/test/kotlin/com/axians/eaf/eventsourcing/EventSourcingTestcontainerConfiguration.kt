package com.axians.eaf.eventsourcing

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Test configuration for Event Sourcing SDK integration tests using Testcontainers.
 *
 * This configuration provides:
 * - PostgreSQL container with EAF event store schema
 * - Automatic service connection configuration
 * - Consistent test database setup
 */
@TestConfiguration
class EventSourcingTestcontainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("eaf_eventstore_test")
            .withUsername("eaf_test")
            .withPassword("eaf_test_password")
            .withInitScript("test-schema.sql")
}

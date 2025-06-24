package com.axians.eaf.controlplane.test

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Testcontainer configuration for Control Plane integration tests. Follows EAF SDK integration
 * testing patterns.
 */
@TestConfiguration
class ControlPlaneTestcontainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("eaf_control_plane_test")
            .withUsername("test_user")
            .withPassword("test_password")
}

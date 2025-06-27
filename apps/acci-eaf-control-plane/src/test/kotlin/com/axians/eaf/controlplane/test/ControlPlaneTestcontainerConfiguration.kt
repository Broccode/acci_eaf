package com.axians.eaf.controlplane.test

import com.axians.eaf.controlplane.domain.port.AuditRepository
import com.axians.eaf.controlplane.domain.service.RoleService
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UserService
import com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository.JpaAuditRepository
import com.axians.eaf.eventing.NatsEventPublisher
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Testcontainer configuration for Control Plane integration tests. Follows EAF SDK integration
 * testing patterns.
 *
 * Excludes problematic components that cause circular dependencies and provides mock
 * implementations.
 */
@TestConfiguration
@ComponentScan(
    excludeFilters =
        [
            ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = [".*\\.JpaAuditRepositoryImpl"],
            ),
        ],
)
class ControlPlaneTestcontainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("eaf_control_plane_test")
            .withUsername("test_user")
            .withPassword("test_password")

    @Bean @Primary
    fun mockNatsEventPublisher(): NatsEventPublisher = mockk(relaxed = true)

    @Bean @Primary
    fun mockAuditRepository(): AuditRepository = mockk(relaxed = true)

    @Bean @Primary
    fun mockJpaAuditRepository(): JpaAuditRepository = mockk(relaxed = true)

    @Bean @Primary
    fun mockRoleService(): RoleService = mockk(relaxed = true)

    @Bean @Primary
    fun mockUserService(): UserService = mockk(relaxed = true)

    @Bean @Primary
    fun mockTenantService(): TenantService = mockk(relaxed = true)
}

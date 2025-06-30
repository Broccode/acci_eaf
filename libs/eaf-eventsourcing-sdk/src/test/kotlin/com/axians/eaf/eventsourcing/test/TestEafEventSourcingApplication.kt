package com.axians.eaf.eventsourcing.test

import com.axians.eaf.core.security.DefaultEafSecurityContextHolder
import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.core.tenancy.integration.TenantContextIntegrationProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test application for EAF Event Sourcing SDK integration tests.
 *
 * This application is specifically configured for integration testing with refined component
 * scanning to avoid conflicts with main application classes and security dependencies.
 */
@SpringBootApplication(
    scanBasePackages =
        [
            "com.axians.eaf.eventsourcing.axon",
            "com.axians.eaf.eventsourcing.adapter",
            "com.axians.eaf.eventsourcing.port",
            "com.axians.eaf.core.tenancy",
        ],
)
class TestEafEventSourcingApplication {
    /**
     * Provide EafSecurityContextHolder bean for integration tests. This prevents "No qualifying
     * bean of type 'EafSecurityContextHolder'" errors when SecurityTenantContextBridge tries to
     * autowire this dependency.
     */
    @Bean
    @Primary
    fun eafSecurityContextHolder(): EafSecurityContextHolder = DefaultEafSecurityContextHolder()

    /**
     * Provide TenantContextIntegrationProperties bean for integration tests. This prevents "No
     * qualifying bean of type 'TenantContextIntegrationProperties'" errors when
     * TenantContextSecurityValidator tries to autowire this dependency.
     */
    @Bean
    @Primary
    fun tenantContextIntegrationProperties(): TenantContextIntegrationProperties =
        TenantContextIntegrationProperties(
            enabled = true,
            respectExistingContext = true,
            enableHeaderFallback = true,
            tenantHeaders = listOf("X-Tenant-ID", "Tenant-ID"),
            addResponseHeaders = false, // Reduce test overhead
            enableDetailedLogging = false, // Reduce test overhead
        )
}

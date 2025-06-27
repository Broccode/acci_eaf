package com.axians.eaf.iam.infrastructure.adapter.inbound

import com.axians.eaf.iam.application.service.InitializationResult
import com.axians.eaf.iam.application.service.SystemInitializationService
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.ApplicationArguments

class DataInitializerAdapterTest {
    private val systemInitializationService = mockk<SystemInitializationService>()
    private val applicationArguments = mockk<ApplicationArguments>()

    @Test
    fun `should call initialization service when enabled`() {
        // Given
        val properties = SystemInitializationProperties(initializeDefaultTenant = true)
        val adapter = DataInitializerAdapter(systemInitializationService, properties)

        every { systemInitializationService.initializeDefaultTenantIfRequired() } returns
            InitializationResult(wasInitialized = true, message = "TestTenant")

        // When
        adapter.run(applicationArguments)

        // Then
        verify { systemInitializationService.initializeDefaultTenantIfRequired() }
    }

    @Test
    fun `should not call initialization service when disabled`() {
        // Given
        val properties = SystemInitializationProperties(initializeDefaultTenant = false)
        val adapter = DataInitializerAdapter(systemInitializationService, properties)

        // When
        adapter.run(applicationArguments)

        // Then
        verify(exactly = 0) { systemInitializationService.initializeDefaultTenantIfRequired() }
    }

    @Test
    fun `should handle initialization service exceptions gracefully`() {
        // Given
        val properties = SystemInitializationProperties(initializeDefaultTenant = true)
        val adapter = DataInitializerAdapter(systemInitializationService, properties)

        every { systemInitializationService.initializeDefaultTenantIfRequired() } throws
            RuntimeException("Database connection failed")

        // When & Then - should not throw exception
        adapter.run(applicationArguments)

        verify { systemInitializationService.initializeDefaultTenantIfRequired() }
    }
}

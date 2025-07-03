package com.axians.eaf.iam.infrastructure.adapter.inbound

import com.axians.eaf.iam.application.service.InitializationResult
import com.axians.eaf.iam.application.service.SystemInitializationService
import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.ApplicationArguments
import org.springframework.dao.DataAccessException

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

        // Use DataAccessException which is the expected type for database connection failures
        every { systemInitializationService.initializeDefaultTenantIfRequired() } throws
            object : DataAccessException("Database connection failed") {}

        // When - should not throw exception (adapter catches and logs it)
        // The adapter is expected to handle exceptions gracefully without re-throwing
        adapter.run(applicationArguments)

        // Then - verify the service was called (completing without exception means success)
        verify { systemInitializationService.initializeDefaultTenantIfRequired() }
    }
}

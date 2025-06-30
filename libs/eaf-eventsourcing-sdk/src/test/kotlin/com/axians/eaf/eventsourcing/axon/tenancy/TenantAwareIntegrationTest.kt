package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantAwareIntegrationTest {
    private lateinit var commandInterceptor: TenantAwareCommandInterceptor
    private lateinit var eventInterceptor: TenantAwareEventInterceptor
    private lateinit var commandMetadata: TenantAwareCommandMetadata
    private lateinit var eventMetadata: TenantAwareEventMetadata
    private lateinit var workingIntegration: WorkingTenantIntegration
    private lateinit var workingMetadata: WorkingTenantMetadata
    private lateinit var workingConfiguration: WorkingTenantConfiguration

    @BeforeEach
    fun setUp() {
        // Initialize components manually for integration testing
        commandInterceptor = TenantAwareCommandInterceptor()
        eventInterceptor = TenantAwareEventInterceptor()
        commandMetadata = TenantAwareCommandMetadata()
        eventMetadata = TenantAwareEventMetadata()
        workingIntegration = WorkingTenantIntegration()
        workingMetadata = WorkingTenantMetadata()
        workingConfiguration = WorkingTenantConfiguration()

        // Clear any existing tenant context
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clean up tenant context after each test
        TenantContextHolder.clear()
    }

    @Test
    fun `should create all tenant components successfully`() {
        // Verify all components are properly created
        assertNotNull(commandInterceptor)
        assertNotNull(eventInterceptor)
        assertNotNull(commandMetadata)
        assertNotNull(eventMetadata)
        assertNotNull(workingIntegration)
        assertNotNull(workingMetadata)
        assertNotNull(workingConfiguration)
    }

    @Test
    fun `should demonstrate end-to-end tenant context flow`() {
        // Given
        val tenantId = "integration-test-tenant"

        // When
        TenantContextHolder.executeInTenantContext(tenantId) {
            // Verify tenant context is set
            assertEquals(tenantId, TenantContextHolder.getCurrentTenantId())

            // Test metadata creation
            val metadata = workingMetadata.createTenantMetadata(null)
            assertEquals(tenantId, metadata["tenant_id"])
            assertEquals("THREAD_CONTEXT", metadata["tenant_source"])

            // Test validation
            val isValid = workingMetadata.validateTenantContext("test-operation")
            assertEquals(true, isValid)

            "integration-test-result"
        }

        // Then
        // Verify tenant context is cleaned up
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle multiple tenant contexts in sequence`() {
        val tenant1 = "tenant-1"
        val tenant2 = "tenant-2"

        // Test first tenant
        TenantContextHolder.executeInTenantContext(tenant1) {
            assertEquals(tenant1, TenantContextHolder.getCurrentTenantId())

            val metadata1 = workingMetadata.createTenantMetadata(null)
            assertEquals(tenant1, metadata1["tenant_id"])
        }

        // Verify cleanup
        assertNull(TenantContextHolder.getCurrentTenantId())

        // Test second tenant
        TenantContextHolder.executeInTenantContext(tenant2) {
            assertEquals(tenant2, TenantContextHolder.getCurrentTenantId())

            val metadata2 = workingMetadata.createTenantMetadata(null)
            assertEquals(tenant2, metadata2["tenant_id"])
        }

        // Verify final cleanup
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle operations without tenant context`() {
        // Verify no tenant context initially
        assertNull(TenantContextHolder.getCurrentTenantId())

        // Test metadata creation without tenant
        val metadata = workingMetadata.createTenantMetadata(null)
        assertEquals("SYSTEM_COMMAND", metadata["tenant_source"])

        // Test validation without tenant
        val isValid = workingMetadata.validateTenantContext("system-operation")
        assertEquals(false, isValid)
    }

    @Test
    fun `should verify configuration properties`() {
        assertEquals(true, workingConfiguration.tenantProcessingEnabled)
        assertEquals(true, workingConfiguration.requireTenantForCommands)
        assertEquals(false, workingConfiguration.requireTenantForEvents)
        assertEquals(true, workingConfiguration.debugLoggingEnabled)
    }

    @Test
    fun `should create tenant-aware command metadata correctly`() {
        val tenantId = "command-test-tenant"

        TenantContextHolder.executeInTenantContext(tenantId) {
            val metadata = commandMetadata.createTenantAwareMetadata()

            assertEquals(tenantId, metadata.get("tenant_id"))
            assertEquals("THREAD_CONTEXT", metadata.get("tenant_source"))
            assertNotNull(metadata.get("tenant_timestamp"))
        }
    }

    @Test
    fun `should create tenant-aware event metadata correctly`() {
        val tenantId = "event-test-tenant"

        TenantContextHolder.executeInTenantContext(tenantId) {
            val metadata = eventMetadata.createDomainEventMetadata(null, "TestEvent")

            assertEquals(tenantId, metadata.get("tenant_id"))
            assertEquals("DOMAIN_EVENT", metadata.get("event_source"))
            assertNotNull(metadata.get("event_timestamp"))
        }
    }

    @Test
    fun `should handle scheduled command metadata creation`() {
        val tenantId = "scheduled-tenant"
        val scheduledBy = "integration-test"

        val metadata = commandMetadata.createScheduledCommandMetadata(tenantId, scheduledBy)

        assertEquals(tenantId, metadata.get("tenant_id"))
        assertEquals("SCHEDULED_TASK", metadata.get("tenant_source"))
        assertEquals(scheduledBy, metadata.get("scheduled_by"))
        assertEquals("scheduled", metadata.get("execution_type"))
    }

    @Test
    fun `should perform tenant authorization validation`() {
        val tenantId = "auth-test-tenant"
        val metadata = commandMetadata.createTenantAwareMetadata(tenantId)

        // Should not throw for valid tenant
        commandMetadata.validateTenantAuthorization(metadata, tenantId)

        // Should not throw for any tenant (null requirement)
        commandMetadata.validateTenantAuthorization(metadata, null)
    }
}

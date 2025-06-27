package com.axians.eaf.controlplane.domain.model.tenant

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TenantTest {
    private val validSettings = TenantSettings.default("example.com")
    private val now = Instant.now()

    @Test
    fun `should create tenant with valid data`() {
        // When
        val tenant =
            Tenant.create(
                name = "Acme Corporation",
                settings = validSettings,
                now = now,
            )

        // Then
        assertNotNull(tenant.id)
        assertEquals("Acme Corporation", tenant.name)
        assertEquals(TenantStatus.ACTIVE, tenant.status)
        assertEquals(validSettings, tenant.settings)
        assertEquals(now, tenant.createdAt)
        assertEquals(now, tenant.lastModified)
        assertNull(tenant.archivedAt)
    }

    @Test
    fun `should trim tenant name when creating`() {
        // When
        val tenant =
            Tenant.create(
                name = "  Acme Corporation  ",
                settings = validSettings,
            )

        // Then
        assertEquals("Acme Corporation", tenant.name)
    }

    @Test
    fun `should reject blank tenant name`() {
        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                Tenant.create(
                    name = "   ",
                    settings = validSettings,
                )
            }
        assertTrue(exception.message!!.contains("Tenant name cannot be blank"))
    }

    @Test
    fun `should reject tenant name exceeding 100 characters`() {
        // Given
        val longName = "A".repeat(101)

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                Tenant.create(
                    name = longName,
                    settings = validSettings,
                )
            }
        assertTrue(exception.message!!.contains("Tenant name cannot exceed 100 characters"))
    }

    @Test
    fun `should update tenant details successfully`() {
        // Given
        val tenant = Tenant.create("Original Name", validSettings, now)
        val newSettings =
            TenantSettings(
                maxUsers = 200,
                allowedDomains = listOf("newdomain.com"),
                features = setOf("advanced-feature"),
            )
        val updateTime = now.plusSeconds(3600)

        // When
        val updatedTenant =
            tenant.updateDetails(
                newName = "Updated Name",
                newSettings = newSettings,
                now = updateTime,
            )

        // Then
        assertEquals(tenant.id, updatedTenant.id)
        assertEquals("Updated Name", updatedTenant.name)
        assertEquals(newSettings, updatedTenant.settings)
        assertEquals(updateTime, updatedTenant.lastModified)
        assertEquals(TenantStatus.ACTIVE, updatedTenant.status)
        assertEquals(now, updatedTenant.createdAt) // Should not change
    }

    @Test
    fun `should not allow updating archived tenant`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now).archive(now.plusSeconds(1000))

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> { tenant.updateDetails("New Name", validSettings) }
        assertTrue(exception.message!!.contains("Cannot update archived tenant"))
    }

    @Test
    fun `should suspend active tenant successfully`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now)
        val suspendTime = now.plusSeconds(3600)

        // When
        val suspendedTenant = tenant.suspend(suspendTime)

        // Then
        assertEquals(tenant.id, suspendedTenant.id)
        assertEquals(TenantStatus.SUSPENDED, suspendedTenant.status)
        assertEquals(suspendTime, suspendedTenant.lastModified)
        assertNull(suspendedTenant.archivedAt)
    }

    @Test
    fun `should not allow suspending non-active tenant`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now).suspend(now.plusSeconds(1000))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { tenant.suspend() }
        assertTrue(exception.message!!.contains("Can only suspend active tenants"))
    }

    @Test
    fun `should reactivate suspended tenant successfully`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now).suspend(now.plusSeconds(1000))
        val reactivateTime = now.plusSeconds(3600)

        // When
        val reactivatedTenant = tenant.reactivate(reactivateTime)

        // Then
        assertEquals(tenant.id, reactivatedTenant.id)
        assertEquals(TenantStatus.ACTIVE, reactivatedTenant.status)
        assertEquals(reactivateTime, reactivatedTenant.lastModified)
        assertNull(reactivatedTenant.archivedAt)
    }

    @Test
    fun `should not allow reactivating non-suspended tenant`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { tenant.reactivate() }
        assertTrue(exception.message!!.contains("Can only reactivate suspended tenants"))
    }

    @Test
    fun `should archive active tenant successfully`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now)
        val archiveTime = now.plusSeconds(3600)

        // When
        val archivedTenant = tenant.archive(archiveTime)

        // Then
        assertEquals(tenant.id, archivedTenant.id)
        assertEquals(TenantStatus.ARCHIVED, archivedTenant.status)
        assertEquals(archiveTime, archivedTenant.lastModified)
        assertEquals(archiveTime, archivedTenant.archivedAt)
    }

    @Test
    fun `should archive suspended tenant successfully`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now).suspend(now.plusSeconds(1000))
        val archiveTime = now.plusSeconds(3600)

        // When
        val archivedTenant = tenant.archive(archiveTime)

        // Then
        assertEquals(TenantStatus.ARCHIVED, archivedTenant.status)
        assertEquals(archiveTime, archivedTenant.archivedAt)
    }

    @Test
    fun `should not allow archiving already archived tenant`() {
        // Given
        val tenant = Tenant.create("Test Tenant", validSettings, now).archive(now.plusSeconds(1000))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { tenant.archive() }
        assertTrue(exception.message!!.contains("Tenant is already archived"))
    }

    @Test
    fun `should identify operational tenant correctly`() {
        // Given
        val activeTenant = Tenant.create("Active Tenant", validSettings, now)
        val suspendedTenant = activeTenant.suspend(now.plusSeconds(1000))
        val archivedTenant = activeTenant.archive(now.plusSeconds(2000))

        // When & Then
        assertTrue(activeTenant.isOperational())
        assertFalse(suspendedTenant.isOperational())
        assertFalse(archivedTenant.isOperational())
    }

    @Test
    fun `should identify modifiable tenant correctly`() {
        // Given
        val activeTenant = Tenant.create("Active Tenant", validSettings, now)
        val suspendedTenant = activeTenant.suspend(now.plusSeconds(1000))
        val archivedTenant = activeTenant.archive(now.plusSeconds(2000))

        // When & Then
        assertTrue(activeTenant.canBeModified())
        assertTrue(suspendedTenant.canBeModified())
        assertFalse(archivedTenant.canBeModified())
    }

    @Test
    fun `should get primary domain correctly`() {
        // Given
        val settings =
            TenantSettings(
                maxUsers = 100,
                allowedDomains = listOf("primary.com", "secondary.com"),
                features = emptySet(),
            )
        val tenant = Tenant.create("Test Tenant", settings, now)

        // When & Then
        assertEquals("primary.com", tenant.getPrimaryDomain())
    }

    @Test
    fun `should enforce archived tenant must have archive date`() {
        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                Tenant(
                    id = TenantId.generate(),
                    name = "Test",
                    status = TenantStatus.ARCHIVED,
                    settings = validSettings,
                    createdAt = now,
                    lastModified = now,
                    archivedAt = null,
                )
            }
        assertTrue(exception.message!!.contains("Archived tenant must have an archive date"))
    }

    @Test
    fun `should enforce only archived tenants can have archive date`() {
        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                Tenant(
                    id = TenantId.generate(),
                    name = "Test",
                    status = TenantStatus.ACTIVE,
                    settings = validSettings,
                    createdAt = now,
                    lastModified = now,
                    archivedAt = now,
                )
            }
        assertTrue(exception.message!!.contains("Only archived tenants can have an archive date"))
    }

    @Test
    fun `should enforce last modified not before created date`() {
        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                Tenant(
                    id = TenantId.generate(),
                    name = "Test",
                    status = TenantStatus.ACTIVE,
                    settings = validSettings,
                    createdAt = now,
                    lastModified = now.minusSeconds(3600),
                )
            }
        assertTrue(exception.message!!.contains("Last modified cannot be before created date"))
    }
}

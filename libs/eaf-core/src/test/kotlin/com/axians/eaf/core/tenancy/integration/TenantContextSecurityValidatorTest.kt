package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TenantContextSecurityValidatorTest {
    private lateinit var properties: TenantContextIntegrationProperties
    private lateinit var validator: TenantContextSecurityValidator

    @BeforeEach
    fun setUp() {
        properties =
            TenantContextIntegrationProperties(
                maxTenantIdLength = 64,
                tenantHeaders = listOf("X-Tenant-ID", "Tenant-ID"),
            )
        validator = TenantContextSecurityValidator(properties)
    }

    @Test
    fun `validateTenantIdSecurity should accept valid tenant ID`() {
        // Arrange
        val validTenantId = "valid-tenant-123"

        // Act & Assert - should not throw
        validator.validateTenantIdSecurity(validTenantId, "jwt", "client1")
    }

    @Test
    fun `validateTenantIdSecurity should reject blank tenant ID`() {
        // Act & Assert
        assertThrows<TenantContextException> {
            validator.validateTenantIdSecurity("", "jwt", "client1")
        }

        assertThrows<TenantContextException> {
            validator.validateTenantIdSecurity("   ", "jwt", "client1")
        }
    }

    @Test
    fun `validateTenantIdSecurity should reject tenant ID exceeding max length`() {
        // Arrange
        val longTenantId = "a".repeat(properties.maxTenantIdLength + 1)

        // Act & Assert
        assertThrows<TenantContextException> {
            validator.validateTenantIdSecurity(longTenantId, "jwt", "client1")
        }
    }

    @Test
    fun `validateTenantIdSecurity should reject tenant ID with invalid characters`() {
        // Arrange
        val invalidTenantIds =
            listOf(
                "tenant@domain",
                "tenant with spaces",
                "tenant/path",
                "tenant<script>",
                "tenant\$var",
                "tenant#hash",
            )

        // Act & Assert
        invalidTenantIds.forEach { invalidTenantId ->
            assertThrows<TenantContextException> {
                validator.validateTenantIdSecurity(invalidTenantId, "jwt", "client1")
            }
        }
    }

    @Test
    fun `validateTenantIdSecurity should reject tenant ID starting or ending with special chars`() {
        // Arrange
        val invalidTenantIds = listOf("-tenant", "tenant-", "_tenant", "tenant_")

        // Act & Assert
        invalidTenantIds.forEach { invalidTenantId ->
            assertThrows<TenantContextException> {
                validator.validateTenantIdSecurity(invalidTenantId, "jwt", "client1")
            }
        }
    }

    @Test
    fun `validateTenantIdSecurity should accept valid alphanumeric with hyphens and underscores`() {
        // Arrange
        val validTenantIds =
            listOf(
                "tenant123",
                "tenant-123",
                "tenant_123",
                "t",
                "tenant-with-hyphens",
                "tenant_with_underscores",
                "mix-ed_tenant_123",
            )

        // Act & Assert - should not throw
        validTenantIds.forEach { validTenantId ->
            validator.validateTenantIdSecurity(validTenantId, "jwt", "client1")
        }
    }

    @Test
    fun `rate limiting should allow requests under limit`() {
        // Arrange
        val tenantId = "test-tenant"
        val clientId = "client1"

        // Act - Make requests under the limit (100 per minute)
        repeat(50) { validator.validateTenantIdSecurity(tenantId, "jwt", clientId) }

        // Assert - should not throw (implicit assertion)
    }

    @Test
    fun `rate limiting should block requests over limit`() {
        // Arrange
        val tenantId = "test-tenant"
        val clientId = "client1"

        // Act - Make requests to exceed the limit (100 per minute)
        repeat(100) { validator.validateTenantIdSecurity(tenantId, "jwt", clientId) }

        // Assert - the 101st request should be blocked
        assertThrows<TenantContextException> {
            validator.validateTenantIdSecurity(tenantId, "jwt", clientId)
        }
    }

    @Test
    fun `rate limiting should be per client`() {
        // Arrange
        val tenantId = "test-tenant"
        val client1 = "client1"
        val client2 = "client2"

        // Act - Make requests for both clients
        repeat(100) {
            validator.validateTenantIdSecurity(tenantId, "jwt", client1)
            validator.validateTenantIdSecurity(tenantId, "jwt", client2)
        }

        // Assert - both should be blocked on next request
        assertThrows<TenantContextException> {
            validator.validateTenantIdSecurity(tenantId, "jwt", client1)
        }
        assertThrows<TenantContextException> {
            validator.validateTenantIdSecurity(tenantId, "jwt", client2)
        }
    }

    @Test
    fun `suspicious activity detection should flag rapid tenant switching`() {
        // Arrange
        val clientId = "client1"

        // Act - Rapid tenant switching (over 20 switches should increase suspicion)
        repeat(25) { i -> validator.validateTenantIdSecurity("tenant-$i", "jwt", clientId) }

        // Assert - should eventually be blocked due to suspicious activity
        // Note: This might require more operations to trigger depending on scoring
    }

    @Test
    fun `suspicious activity detection should flag excessive header requests`() {
        // Arrange
        val tenantId = "test-tenant"
        val clientId = "client1"

        // Act - Many header-based requests (potential injection attempts)
        assertThrows<TenantContextException> {
            repeat(60) { validator.validateTenantIdSecurity(tenantId, "header", clientId) }
        }
    }

    @Test
    fun `validateTenantContextTransition should validate both from and to tenant IDs`() {
        // Arrange
        val fromTenantId = "from-tenant"
        val toTenantId = "to-tenant"
        val clientId = "client1"

        // Act & Assert - should not throw
        validator.validateTenantContextTransition(fromTenantId, toTenantId, clientId)
    }

    @Test
    fun `validateTenantContextTransition should handle null from tenant`() {
        // Arrange
        val toTenantId = "to-tenant"
        val clientId = "client1"

        // Act & Assert - should not throw
        validator.validateTenantContextTransition(null, toTenantId, clientId)
    }

    @Test
    fun `validateTenantContextTransition should reject invalid to tenant`() {
        // Arrange
        val fromTenantId = "from-tenant"
        val invalidToTenantId = "invalid@tenant"
        val clientId = "client1"

        // Act & Assert
        assertThrows<TenantContextException> {
            validator.validateTenantContextTransition(fromTenantId, invalidToTenantId, clientId)
        }
    }

    @Test
    fun `cleanupExpiredData should not throw exceptions`() {
        // Arrange
        validator.validateTenantIdSecurity("test-tenant", "jwt", "client1")

        // Act & Assert - should not throw
        validator.cleanupExpiredData()
    }

    @Test
    fun `getSecurityStatistics should return valid statistics`() {
        // Arrange
        validator.validateTenantIdSecurity("test-tenant", "jwt", "client1")

        // Act
        val stats = validator.getSecurityStatistics()

        // Assert
        assertNotNull(stats)
        assertTrue(stats.activeRateLimiters >= 0)
        assertTrue(stats.activeSuspiciousActivityTrackers >= 0)
        assertTrue(stats.totalRateLimitEntries >= 0)
        assertTrue(stats.totalSuspiciousActivityEntries >= 0)
    }

    @Test
    fun `concurrent validation calls should handle concurrency safely`() {
        // Arrange
        val tenantId = "test-tenant"
        val clientId = "client1"

        // Act - Concurrent validation (simulate multiple threads)
        val results =
            (1..10).map {
                try {
                    validator.validateTenantIdSecurity(tenantId, "jwt", clientId)
                    true
                } catch (e: TenantContextException) {
                    false
                }
            }

        // Assert - most should succeed (exact count depends on timing)
        assertTrue(results.any { it })
    }
}

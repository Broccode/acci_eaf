package com.axians.eaf.core.tenancy

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class TenantContextHolderEnhancedTest {
    @BeforeEach
    fun setUp() {
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Nested
    inner class FullTenantContextManagement {
        @Test
        fun `setCurrentTenantContext should set full context`() {
            // Given
            val tenantContext =
                TenantContext(
                    tenantId = "test-tenant",
                    tenantName = "Test Tenant",
                    organizationName = "Test Org",
                    region = "us-east-1",
                    subscriptionTier = "premium",
                    metadata = mapOf("key1" to "value1", "key2" to "value2"),
                )

            // When
            TenantContextHolder.setCurrentTenantContext(tenantContext)

            // Then
            assertEquals("test-tenant", TenantContextHolder.getCurrentTenantId())
            assertEquals(tenantContext, TenantContextHolder.getCurrentTenantContext())
        }

        @Test
        fun `getCurrentTenantContext should return null when no context set`() {
            // When/Then
            assertNull(TenantContextHolder.getCurrentTenantContext())
        }

        @Test
        fun `setCurrentTenantId should create minimal TenantContext`() {
            // Given
            val tenantId = "simple-tenant"

            // When
            TenantContextHolder.setCurrentTenantId(tenantId)

            // Then
            val context = TenantContextHolder.getCurrentTenantContext()
            assertNotNull(context)
            assertEquals(tenantId, context!!.tenantId)
            assertNull(context.tenantName)
            assertTrue(context.metadata.isEmpty())
        }

        @Test
        fun `setCurrentTenantContext should validate context`() {
            // Given - invalid context with blank tenant ID
            val invalidContext = TenantContext(tenantId = "", tenantName = "Test")

            // When/Then
            assertThrows<IllegalArgumentException> {
                TenantContextHolder.setCurrentTenantContext(invalidContext)
            }
        }
    }

    @Nested
    inner class MetadataManagement {
        @Test
        fun `getTenantMetadata should return metadata value`() {
            // Given
            val context =
                TenantContext(
                    tenantId = "test-tenant",
                    metadata = mapOf("environment" to "production", "version" to "1.0"),
                )
            TenantContextHolder.setCurrentTenantContext(context)

            // When/Then
            assertEquals("production", TenantContextHolder.getTenantMetadata("environment"))
            assertEquals("1.0", TenantContextHolder.getTenantMetadata("version"))
            assertNull(TenantContextHolder.getTenantMetadata("nonexistent"))
        }

        @Test
        fun `getTenantMetadata should return null when no context set`() {
            // When/Then
            assertNull(TenantContextHolder.getTenantMetadata("any-key"))
        }

        @Test
        fun `addTenantMetadata should add metadata to existing context`() {
            // Given
            val context =
                TenantContext(tenantId = "test-tenant", metadata = mapOf("existing" to "value"))
            TenantContextHolder.setCurrentTenantContext(context)

            // When
            TenantContextHolder.addTenantMetadata("new-key", "new-value")

            // Then
            val updatedContext = TenantContextHolder.getCurrentTenantContext()
            assertNotNull(updatedContext)
            assertEquals("value", updatedContext!!.metadata["existing"])
            assertEquals("new-value", updatedContext.metadata["new-key"])
        }

        @Test
        fun `addTenantMetadata should throw exception when no context set`() {
            // When/Then
            val exception =
                assertThrows<TenantContextException> {
                    TenantContextHolder.addTenantMetadata("key", "value")
                }
            assertTrue(exception.message?.contains("Cannot add metadata") == true)
        }

        @Test
        fun `addTenantMetadata should update existing metadata key`() {
            // Given
            val context =
                TenantContext(tenantId = "test-tenant", metadata = mapOf("key" to "old-value"))
            TenantContextHolder.setCurrentTenantContext(context)

            // When
            TenantContextHolder.addTenantMetadata("key", "new-value")

            // Then
            assertEquals("new-value", TenantContextHolder.getTenantMetadata("key"))
        }
    }

    @Nested
    inner class EnhancedScopedExecution {
        @Test
        fun `executeInTenantContext with TenantContext should work correctly`() {
            // Given
            val context =
                TenantContext(
                    tenantId = "scoped-tenant",
                    tenantName = "Scoped Tenant",
                    metadata = mapOf("scope" to "test"),
                )

            // When
            val result =
                TenantContextHolder.executeInTenantContext(context) {
                    val currentContext = TenantContextHolder.getCurrentTenantContext()
                    assertNotNull(currentContext)
                    assertEquals("scoped-tenant", currentContext!!.tenantId)
                    assertEquals("Scoped Tenant", currentContext.tenantName)
                    assertEquals("test", currentContext.metadata["scope"])
                    "execution-result"
                }

            // Then
            assertEquals("execution-result", result)
            assertNull(TenantContextHolder.getCurrentTenantContext())
        }

        @Test
        fun `executeInTenantContext should restore previous full context`() {
            // Given
            val originalContext =
                TenantContext(
                    tenantId = "original-tenant",
                    tenantName = "Original",
                    metadata = mapOf("type" to "original"),
                )
            val scopedContext =
                TenantContext(
                    tenantId = "scoped-tenant",
                    tenantName = "Scoped",
                    metadata = mapOf("type" to "scoped"),
                )
            TenantContextHolder.setCurrentTenantContext(originalContext)

            // When
            TenantContextHolder.executeInTenantContext(scopedContext) {
                val current = TenantContextHolder.getCurrentTenantContext()
                assertEquals("scoped-tenant", current?.tenantId)
                assertEquals("scoped", current?.metadata?.get("type"))
            }

            // Then
            val restored = TenantContextHolder.getCurrentTenantContext()
            assertEquals(originalContext, restored)
        }

        @Test
        fun `executeInTenantContext with full context should cleanup on exception`() {
            // Given
            val originalContext =
                TenantContext(
                    tenantId = "original-tenant",
                    metadata = mapOf("original" to "true"),
                )
            val scopedContext =
                TenantContext(tenantId = "scoped-tenant", metadata = mapOf("scoped" to "true"))
            TenantContextHolder.setCurrentTenantContext(originalContext)

            // When/Then
            assertThrows<RuntimeException> {
                TenantContextHolder.executeInTenantContext(scopedContext) {
                    throw RuntimeException("Test exception")
                }
            }

            // Verify cleanup occurred
            assertEquals(originalContext, TenantContextHolder.getCurrentTenantContext())
        }
    }

    @Nested
    inner class ContextInheritance {
        @Test
        fun `inheritTenantContext should create inheritance function`() {
            // Given
            val context =
                TenantContext(
                    tenantId = "parent-tenant",
                    tenantName = "Parent",
                    metadata = mapOf("inherited" to "true"),
                )
            TenantContextHolder.setCurrentTenantContext(context)

            // When
            val inheritanceFunction = TenantContextHolder.inheritTenantContext()

            // Clear current context to simulate different thread
            TenantContextHolder.clear()
            assertNull(TenantContextHolder.getCurrentTenantContext())

            // Apply inheritance in "child thread"
            inheritanceFunction()

            // Then
            val inheritedContext = TenantContextHolder.getCurrentTenantContext()
            assertEquals(context, inheritedContext)
        }

        @Test
        fun `inheritTenantContext should handle no context gracefully`() {
            // Given - no tenant context set

            // When
            val inheritanceFunction = TenantContextHolder.inheritTenantContext()
            inheritanceFunction()

            // Then - should not fail and context should remain null
            assertNull(TenantContextHolder.getCurrentTenantContext())
        }
    }

    @Nested
    inner class TenantContextDataClass {
        @Test
        fun `TenantContext validation should pass for valid context`() {
            // Given
            val validContext =
                TenantContext(tenantId = "valid-tenant-123", tenantName = "Valid Tenant")

            // When/Then - should not throw
            assertDoesNotThrow { validContext.validate() }
        }

        @Test
        fun `TenantContext validation should reject blank tenant ID`() {
            // Given
            val invalidContext = TenantContext(tenantId = "")

            // When/Then
            assertThrows<IllegalArgumentException> { invalidContext.validate() }
        }

        @Test
        fun `TenantContext validation should reject too long tenant ID`() {
            // Given
            val invalidContext = TenantContext(tenantId = "a".repeat(100))

            // When/Then
            assertThrows<IllegalArgumentException> { invalidContext.validate() }
        }

        @Test
        fun `TenantContext validation should reject invalid characters`() {
            // Given
            val invalidContext = TenantContext(tenantId = "invalid@tenant#id")

            // When/Then
            assertThrows<IllegalArgumentException> { invalidContext.validate() }
        }

        @Test
        fun `TenantContext withMetadata should add single metadata`() {
            // Given
            val context = TenantContext("test-tenant")

            // When
            val updated = context.withMetadata("key", "value")

            // Then
            assertEquals("value", updated.metadata["key"])
            assertEquals("test-tenant", updated.tenantId)
        }

        @Test
        fun `TenantContext withMetadata should add multiple metadata`() {
            // Given
            val context =
                TenantContext(tenantId = "test-tenant", metadata = mapOf("existing" to "value"))
            val additionalMetadata = mapOf("new1" to "value1", "new2" to "value2")

            // When
            val updated = context.withMetadata(additionalMetadata)

            // Then
            assertEquals("value", updated.metadata["existing"])
            assertEquals("value1", updated.metadata["new1"])
            assertEquals("value2", updated.metadata["new2"])
        }

        @Test
        fun `TenantContext constructor with just tenantId should work`() {
            // When
            val context = TenantContext("simple-tenant")

            // Then
            assertEquals("simple-tenant", context.tenantId)
            assertNull(context.tenantName)
            assertNull(context.organizationName)
            assertTrue(context.metadata.isEmpty())
            assertTrue(context.establishedAt.isBefore(Instant.now().plusSeconds(1)))
        }
    }
}

package com.axians.eaf.core.security

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl

class EafCoroutineContextTest {
    @BeforeEach
    fun setup() {
        SecurityContextHolder.clearContext()
        CorrelationIdManager.clearCorrelationId()
    }

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
        CorrelationIdManager.clearCorrelationId()
    }

    @Test
    fun `SecurityContextElement should propagate security context to coroutines`() =
        runBlocking {
            // Given
            val securityContext = SecurityContextImpl()
            SecurityContextHolder.setContext(securityContext)
            val element = SecurityContextElement()

            // When
            withContext<Unit>(element) {
                // Then
                assertSame(securityContext, SecurityContextHolder.getContext())
            }
        }

    @Test
    fun `CorrelationIdElement should propagate correlation ID to coroutines`() =
        runBlocking {
            // Given
            val correlationId = "test-correlation-id"
            CorrelationIdManager.setCorrelationId(correlationId)
            val element = CorrelationIdElement()

            // When
            withContext<Unit>(element) {
                // Then
                assertEquals(correlationId, CorrelationIdManager.getCurrentCorrelationId())
            }
        }

    @Test
    fun `EafContextElement should propagate both security context and correlation ID`() =
        runBlocking {
            // Given
            val securityContext = SecurityContextImpl()
            val correlationId = "test-correlation-id"
            SecurityContextHolder.setContext(securityContext)
            CorrelationIdManager.setCorrelationId(correlationId)
            val element = EafContextElement()

            // When
            withContext<Unit>(element) {
                // Then
                assertSame(securityContext, SecurityContextHolder.getContext())
                assertEquals(correlationId, CorrelationIdManager.getCurrentCorrelationId())
            }
        }

    @Test
    fun `withEafContext should propagate current context`() =
        runBlocking {
            // Given
            val securityContext = SecurityContextImpl()
            val correlationId = "test-correlation-id"
            SecurityContextHolder.setContext(securityContext)
            CorrelationIdManager.setCorrelationId(correlationId)

            // When
            withEafContext {
                // Then
                assertSame(securityContext, SecurityContextHolder.getContext())
                assertEquals(correlationId, CorrelationIdManager.getCurrentCorrelationId())
            }
        }

    @Test
    fun `context should be restored after coroutine completion`() =
        runBlocking {
            // Given
            val originalSecurityContext = SecurityContextImpl()
            val originalCorrelationId = "original-correlation-id"
            SecurityContextHolder.setContext(originalSecurityContext)
            CorrelationIdManager.setCorrelationId(originalCorrelationId)

            val newSecurityContext = SecurityContextImpl()
            val newCorrelationId = "new-correlation-id"

            // When
            withContext<Unit>(EafContextElement(newSecurityContext, newCorrelationId)) {
                // Verify new context is active
                assertSame(newSecurityContext, SecurityContextHolder.getContext())
                assertEquals(newCorrelationId, CorrelationIdManager.getCurrentCorrelationId())
            }

            // Then - original context should be restored
            assertSame(originalSecurityContext, SecurityContextHolder.getContext())
            assertEquals(originalCorrelationId, CorrelationIdManager.getCurrentCorrelationId())
        }

    @Test
    fun `context should be restored after exception in coroutine`() =
        runBlocking {
            // Given
            val originalSecurityContext = SecurityContextImpl()
            val originalCorrelationId = "original-correlation-id"
            SecurityContextHolder.setContext(originalSecurityContext)
            CorrelationIdManager.setCorrelationId(originalCorrelationId)

            val newSecurityContext = SecurityContextImpl()
            val newCorrelationId = "new-correlation-id"

            // When/Then
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    withContext<Unit>(EafContextElement(newSecurityContext, newCorrelationId)) {
                        throw IllegalStateException("Test exception")
                    }
                }
            }

            // Then - original context should be restored
            assertSame(originalSecurityContext, SecurityContextHolder.getContext())
            assertEquals(originalCorrelationId, CorrelationIdManager.getCurrentCorrelationId())
        }

    @Test
    fun `should handle null original context gracefully`() =
        runBlocking {
            // Given - no original context
            val newSecurityContext = SecurityContextImpl()
            val newCorrelationId = "new-correlation-id"

            // When
            withContext<Unit>(EafContextElement(newSecurityContext, newCorrelationId)) {
                // Verify new context is active
                assertSame(newSecurityContext, SecurityContextHolder.getContext())
                assertEquals(newCorrelationId, CorrelationIdManager.getCurrentCorrelationId())
            }

            // Then - context should be cleared
            assertNull(SecurityContextHolder.getContext().authentication)
            assertNull(CorrelationIdManager.getCurrentCorrelationIdOrNull())
        }

    @Test
    fun `currentEafContextElement should capture current context`() {
        // Given
        val securityContext = SecurityContextImpl()
        val correlationId = "test-correlation-id"
        SecurityContextHolder.setContext(securityContext)
        CorrelationIdManager.setCorrelationId(correlationId)

        // When
        val element = currentEafContextElement()

        // Then
        runBlocking {
            withContext<Unit>(element) {
                assertSame(securityContext, SecurityContextHolder.getContext())
                assertEquals(correlationId, CorrelationIdManager.getCurrentCorrelationId())
            }
        }
    }

    @Test
    fun `extension functions should work correctly`() =
        runBlocking {
            // Given
            val securityContext = SecurityContextImpl()
            val correlationId = "test-correlation-id"
            SecurityContextHolder.setContext(securityContext)
            CorrelationIdManager.setCorrelationId(correlationId)

            // When/Then
            withContext<Unit>(coroutineContext.withEafContext()) {
                assertSame(securityContext, SecurityContextHolder.getContext())
                assertEquals(correlationId, CorrelationIdManager.getCurrentCorrelationId())
            }
        }
}

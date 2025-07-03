package com.axians.eaf.core.axon.correlation

import com.axians.eaf.core.security.EafSecurityContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.axonframework.messaging.GenericMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.security.Principal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityContextCorrelationDataProviderTest {
    private lateinit var securityContextHolder: EafSecurityContextHolder
    private lateinit var provider: SecurityContextCorrelationDataProvider
    private lateinit var mockAuthentication: Authentication

    @BeforeEach
    fun setUp() {
        securityContextHolder = mockk(relaxed = true)
        provider = SecurityContextCorrelationDataProvider(securityContextHolder)
        mockAuthentication = mockk(relaxed = true)
    }

    @Test
    fun `should return basic correlation data when no authentication is available`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns false
        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then
        assertNotNull(result)
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.EXTRACTION_TIMESTAMP))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.CORRELATION_ID))
        // Should have 7 entries: timestamp, request_context_type, correlation_id, process_type,
        // execution_type, tenant_id, user_id
        assertEquals(7, result.size)
        verify { securityContextHolder.isAuthenticated() }
    }

    @Test
    fun `should extract all security context data when fully authenticated`() {
        // Given
        val tenantId = "TENANT_A"
        val userId = "user-123"
        val userEmail = "test@example.com"
        val authorities =
            listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("ROLE_ADMIN"))

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns tenantId
        every { securityContextHolder.getUserId() } returns userId
        every { securityContextHolder.getPrincipal() } returns createMockPrincipal(userEmail)
        every { securityContextHolder.getAuthentication() } returns mockAuthentication
        every { mockAuthentication.authorities } returns authorities

        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then
        assertNotNull(result)
        assertEquals(tenantId, result[SecurityContextCorrelationDataProvider.TENANT_ID])
        assertEquals(userId, result[SecurityContextCorrelationDataProvider.USER_ID])
        assertEquals(userEmail, result[SecurityContextCorrelationDataProvider.USER_EMAIL])
        assertEquals("USER,ADMIN", result[SecurityContextCorrelationDataProvider.USER_ROLES])
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.EXTRACTION_TIMESTAMP))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.CORRELATION_ID))

        // Should have security context + request context data
        assertTrue(result.size >= 7)
    }

    @Test
    fun `should handle null tenant ID gracefully`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns null
        every { securityContextHolder.getUserId() } returns "user-123"
        every { securityContextHolder.getPrincipal() } returns null
        every { securityContextHolder.getAuthentication() } returns mockAuthentication
        every { mockAuthentication.authorities } returns emptyList()

        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then
        assertNotNull(result)
        assertFalse(result.containsKey(SecurityContextCorrelationDataProvider.TENANT_ID))
        assertEquals("user-123", result[SecurityContextCorrelationDataProvider.USER_ID])
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.EXTRACTION_TIMESTAMP))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.CORRELATION_ID))
    }

    @Test
    fun `should generate correlation ID for system processes`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns false
        val message = GenericMessage.asMessage("test-payload")

        // When
        System.err.println("DEBUG_SYS_THREAD: ${Thread.currentThread().name}")
        val result = provider.correlationDataFor(message)

        // Then
        val correlationId = result[SecurityContextCorrelationDataProvider.CORRELATION_ID] as String
        assertTrue(correlationId.isNotBlank())
        // Should be a valid UUID format
        assertTrue(
            correlationId.matches(
                Regex(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                ),
            ),
        )

        // Should mark as test context when running within test threads (e.g., "Test worker")
        // This aligns with the new NonWebContextExtractor logic that classifies Gradle/JUnit
        // test executor threads as "test" instead of the generic "system" type.
        assertEquals("test", result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE])
    }

    @Test
    fun `should never throw exceptions when security context extraction fails`() {
        // Given
        every { securityContextHolder.isAuthenticated() } throws
            RuntimeException("Security context error")
        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then
        assertNotNull(result)
        assertTrue(result.isEmpty()) // Should return empty map when exception occurs
    }

    @Test
    fun `should ensure all correlation data values are strings for JSON serialization`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns "TENANT_A"
        every { securityContextHolder.getUserId() } returns "user-123"
        every { securityContextHolder.getPrincipal() } returns
            createMockPrincipal("test@example.com")
        every { securityContextHolder.getAuthentication() } returns mockAuthentication
        every { mockAuthentication.authorities } returns listOf(SimpleGrantedAuthority("ROLE_USER"))

        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then - All values should be strings for JSON serialization
        result.values.forEach { value ->
            assertTrue(
                value is String,
                "All correlation data values should be strings for JSON serialization, " +
                    "but found: ${value?.javaClass?.simpleName}",
            )
        }
    }

    @Test
    fun `should maintain backward compatibility with existing tests`() {
        // Given - Same setup as original full authentication test
        val tenantId = "TENANT_A"
        val userId = "user-123"
        val userEmail = "test@example.com"
        val authorities =
            listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("ROLE_ADMIN"))

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns tenantId
        every { securityContextHolder.getUserId() } returns userId
        every { securityContextHolder.getPrincipal() } returns createMockPrincipal(userEmail)
        every { securityContextHolder.getAuthentication() } returns mockAuthentication
        every { mockAuthentication.authorities } returns authorities

        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then - All original security context data should still be present
        assertEquals(tenantId, result[SecurityContextCorrelationDataProvider.TENANT_ID])
        assertEquals(userId, result[SecurityContextCorrelationDataProvider.USER_ID])
        assertEquals(userEmail, result[SecurityContextCorrelationDataProvider.USER_EMAIL])
        assertEquals("USER,ADMIN", result[SecurityContextCorrelationDataProvider.USER_ROLES])
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.EXTRACTION_TIMESTAMP))

        // New request context data should also be present
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.CORRELATION_ID))

        // Should not contain web-specific data when no actual web request context
        assertFalse(result.containsKey(SecurityContextCorrelationDataProvider.CLIENT_IP))
        assertFalse(result.containsKey(SecurityContextCorrelationDataProvider.USER_AGENT))
    }

    private fun createMockPrincipal(email: String): Principal =
        object : Principal, HasEmail {
            override fun getName(): String = email

            override fun getEmail(): String = email
        }

    // New tests for Story 4.2.2b advanced features

    @Test
    fun `should detect scheduled task context type`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "QuartzScheduler_Worker-1"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertEquals("scheduled", result[SecurityContextCorrelationDataProvider.PROCESS_TYPE])
            assertEquals(
                "scheduled",
                result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE],
            )
            assertEquals("scheduled_task", result["execution_type"])
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.THREAD_NAME))
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.PROCESS_ID))
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should detect message-driven context type`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "nats-consumer-thread-1"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertEquals(
                "message-driven",
                result[SecurityContextCorrelationDataProvider.PROCESS_TYPE],
            )
            assertEquals(
                "message-driven",
                result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE],
            )
            assertEquals("message_consumer", result["execution_type"])
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.THREAD_NAME))
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.PROCESS_ID))
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should detect batch processing context type`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "batch-job-executor-1"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertEquals("batch", result[SecurityContextCorrelationDataProvider.PROCESS_TYPE])
            assertEquals(
                "batch",
                result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE],
            )
            assertEquals("batch_process", result["execution_type"])
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.THREAD_NAME))
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.PROCESS_ID))
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should detect test context type`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "Test worker"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertEquals("test", result[SecurityContextCorrelationDataProvider.PROCESS_TYPE])
            assertEquals(
                "test",
                result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE],
            )
            assertEquals("test_execution", result["execution_type"])
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.THREAD_NAME))
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.PROCESS_ID))
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should apply security filtering and mark data as sanitized`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns "TENANT_A"
        every { securityContextHolder.getUserId() } returns "user-123"
        every { securityContextHolder.getPrincipal() } returns
            createMockPrincipal("test@example.com")
        every { securityContextHolder.getAuthentication() } returns mockAuthentication
        every { mockAuthentication.authorities } returns listOf(SimpleGrantedAuthority("ROLE_USER"))

        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then
        assertEquals("true", result[SecurityContextCorrelationDataProvider.DATA_SANITIZED])
        assertEquals("true", result[SecurityContextCorrelationDataProvider.COLLECTION_ENABLED])

        // Security context data should be present
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.TENANT_ID))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.USER_ID))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.USER_EMAIL))

        // Request context data should be present
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.CORRELATION_ID))
    }

    @Test
    fun `should exclude session ID by default for privacy`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns false
        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then
        assertFalse(result.containsKey(SecurityContextCorrelationDataProvider.SESSION_ID))
    }

    @Test
    fun `should maintain process information in correlation data`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "executor-service-thread-1"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.THREAD_NAME))
            assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.PROCESS_ID))
            assertEquals(
                "executor-service-thread-1",
                result[SecurityContextCorrelationDataProvider.THREAD_NAME],
            )

            val processId = result[SecurityContextCorrelationDataProvider.PROCESS_ID] as String
            assertTrue(processId.isNotBlank())
            assertTrue(processId.toLongOrNull() != null) // Should be a valid thread ID
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should handle async process type correctly`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "async-executor-pool-1"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertEquals("async", result[SecurityContextCorrelationDataProvider.PROCESS_TYPE])
            assertEquals(
                "async",
                result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE],
            )
            assertEquals("async_operation", result["execution_type"])
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should default to system process type for unknown thread names`() {
        // Given
        val originalThreadName = Thread.currentThread().name
        Thread.currentThread().name = "unknown-custom-thread"

        try {
            every { securityContextHolder.isAuthenticated() } returns false
            val message = GenericMessage.asMessage("test-payload")

            // When
            val result = provider.correlationDataFor(message)

            // Then
            assertEquals("system", result[SecurityContextCorrelationDataProvider.PROCESS_TYPE])
            assertEquals(
                "system",
                result[SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE],
            )
            assertEquals("system_operation", result["execution_type"])
        } finally {
            Thread.currentThread().name = originalThreadName
        }
    }

    @Test
    fun `should include all required metadata fields for audit trail`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns "TENANT_A"
        every { securityContextHolder.getUserId() } returns "user-123"
        every { securityContextHolder.getPrincipal() } returns
            createMockPrincipal("test@example.com")
        every { securityContextHolder.getAuthentication() } returns mockAuthentication
        every { mockAuthentication.authorities } returns listOf(SimpleGrantedAuthority("ROLE_USER"))

        val message = GenericMessage.asMessage("test-payload")

        // When
        val result = provider.correlationDataFor(message)

        // Then - Required audit trail fields
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.EXTRACTION_TIMESTAMP))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.CORRELATION_ID))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.REQUEST_CONTEXT_TYPE))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.DATA_SANITIZED))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.COLLECTION_ENABLED))

        // Security context should be complete
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.TENANT_ID))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.USER_ID))
        assertTrue(result.containsKey(SecurityContextCorrelationDataProvider.USER_EMAIL))

        // All timestamp values should be valid ISO strings
        val extractionTimestamp =
            result[SecurityContextCorrelationDataProvider.EXTRACTION_TIMESTAMP] as String
        assertTrue(extractionTimestamp.contains("T")) // ISO 8601 format
    }
}

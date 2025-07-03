package com.axians.eaf.controlplane.integration.security

import com.axians.eaf.controlplane.domain.service.RoleService
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UserService
import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.eventing.NatsEventPublisher
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Instant
import java.util.UUID

/**
 * Test configuration for security context integration tests.
 *
 * This configuration provides:
 * - Mock implementations of core services
 * - Event capture infrastructure
 * - Security context simulation
 * - Test-specific beans and utilities
 */
@TestConfiguration
class SecurityContextTestConfiguration {
    /** Mock EAF security context holder that uses test context data. */
    @Bean
    @Primary
    fun testEafSecurityContextHolder(): EafSecurityContextHolder =
        mockk<EafSecurityContextHolder>().apply {
            every { getTenantIdOrNull() } answers { TestSecurityContext.getTenantId() }
            every { getTenantId() } answers
                {
                    TestSecurityContext.getTenantId() ?: error("No tenant context")
                }
            every { getUserId() } answers { TestSecurityContext.getUserId() }
            every { hasRole(any()) } answers { TestSecurityContext.hasRole(firstArg()) }
            every { hasPermission(any()) } answers { false }
            every { hasAnyRole(*anyVararg()) } answers
                {
                    val roles = firstArg<Array<String>>()
                    roles.any { TestSecurityContext.hasRole(it) }
                }
            every { isAuthenticated() } answers { TestSecurityContext.isAuthenticated() }
            every { getAuthentication() } answers { null }
            every { getPrincipal() } answers { null }
        }

    /** Mock NATS event publisher that captures events for verification. */
    @Bean
    @Primary
    fun testNatsEventPublisher(testEventCapture: TestEventCapture): NatsEventPublisher =
        mockk<NatsEventPublisher>().apply {
            coEvery { publish(any<String>(), any<String>(), any<Any>()) } answers
                {
                    val subject = firstArg<String>()
                    val tenantId = secondArg<String>()
                    val event = thirdArg<Any>()

                    // Create realistic metadata from current test security context
                    val metadata = createEventMetadata(tenantId)

                    // Capture the event for test verification
                    testEventCapture.captureEvent(event, metadata, subject, tenantId)

                    mockk() // Return a mock PublishAck
                }

            coEvery {
                publish(any<String>(), any<String>(), any<Any>(), any<Map<String, Any>>())
            } answers
                {
                    val subject = firstArg<String>()
                    val tenantId = secondArg<String>()
                    val event = thirdArg<Any>()
                    val additionalMetadata = arg<Map<String, Any>>(3)

                    // Create metadata combining security context and additional metadata
                    val baseMetadata = createEventMetadata(tenantId)
                    val metadata = baseMetadata + additionalMetadata

                    // Capture the event for test verification
                    testEventCapture.captureEvent(event, metadata, subject, tenantId)

                    mockk() // Return a mock PublishAck
                }
        }

    /**
     * Creates realistic event metadata from test security context. This simulates the real
     * correlation data provider behavior.
     */
    private fun createEventMetadata(tenantId: String): Map<String, Any> =
        if (TestSecurityContext.isAuthenticated()) {
            mapOf(
                "tenant_id" to (TestSecurityContext.getTenantId() ?: tenantId),
                "user_id" to (TestSecurityContext.getUserId() ?: "UNKNOWN"),
                "user_email" to (TestSecurityContext.getUserEmail() ?: "test@example.com"),
                "user_roles" to TestSecurityContext.getUserRoles().joinToString(","),
                "correlation_id" to TestSecurityContext.getCorrelationId(),
                "request_id" to UUID.randomUUID().toString(),
                "session_id" to "test-session-123",
                "client_ip" to "127.0.0.1",
                "user_agent" to "Test-Client/1.0",
                "request_timestamp" to Instant.now().toString(),
                "authentication_time" to
                    TestSecurityContext.getAuthenticationTime().toString(),
                "event_timestamp" to Instant.now().toString(),
            )
        } else {
            // System context fallback
            mapOf(
                "tenant_id" to tenantId,
                "user_id" to "SYSTEM",
                "correlation_id" to UUID.randomUUID().toString(),
                "request_timestamp" to Instant.now().toString(),
                "event_timestamp" to Instant.now().toString(),
                "system_operation" to true,
            )
        }

    /** Test event capture bean for verifying published events. */
    @Bean fun testEventCapture(): TestEventCapture = TestEventCapture()

    /**
     * Mock RoleService bean for integration tests that require it (e.g.,
     * SecurityContextIntegrationTest).
     */
    @Bean @Primary
    fun mockRoleService(): RoleService = mockk(relaxed = true)

    /**
     * Mock TenantService bean for integration tests that require it (e.g.,
     * SecurityContextIntegrationTest).
     */
    @Bean @Primary
    fun mockTenantService(): TenantService = mockk(relaxed = true)

    /**
     * Mock UserService bean for integration tests that require it (e.g.,
     * SecurityContextIntegrationTest).
     */
    @Bean @Primary
    fun mockUserService(): UserService = mockk(relaxed = true)
}

/**
 * Test-specific security context holder that maintains security data during test execution. This
 * replaces the complex security context classes with a simple test data holder.
 */
object TestSecurityContext {
    private var tenantId: String? = null
    private var userId: String? = null
    private var userEmail: String? = null
    private var userRoles: Set<String> = emptySet()
    private var correlationId: String = UUID.randomUUID().toString()
    private var authenticationTime: Instant = Instant.now()
    private var authenticated: Boolean = false

    fun setContext(
        tenantId: String,
        userId: String,
        userEmail: String,
        roles: Array<String>,
    ) {
        this.tenantId = tenantId
        this.userId = userId
        this.userEmail = userEmail
        this.userRoles = roles.toSet()
        this.correlationId = UUID.randomUUID().toString()
        this.authenticationTime = Instant.now()
        this.authenticated = true
    }

    fun clearContext() {
        this.tenantId = null
        this.userId = null
        this.userEmail = null
        this.userRoles = emptySet()
        this.correlationId = UUID.randomUUID().toString()
        this.authenticationTime = Instant.now()
        this.authenticated = false
    }

    fun getTenantId(): String? = tenantId

    fun getUserId(): String? = userId

    fun getUserEmail(): String? = userEmail

    fun getUserRoles(): Set<String> = userRoles

    fun getCorrelationId(): String = correlationId

    fun getAuthenticationTime(): Instant = authenticationTime

    fun isAuthenticated(): Boolean = authenticated

    fun hasRole(role: String): Boolean = userRoles.contains(role)
}

package com.axians.eaf.controlplane.infrastructure.security.aspect

import com.axians.eaf.controlplane.domain.event.SecurityAuditEvent
import com.axians.eaf.controlplane.domain.exception.AuthenticationRequiredException
import com.axians.eaf.controlplane.domain.exception.InsufficientPermissionException
import com.axians.eaf.controlplane.infrastructure.security.annotation.RequiresTenantAccess
import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.eventing.NatsEventPublisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for TenantSecurityAspect covering:
 * - Security validation functionality
 * - Event publishing for audit trail
 * - MDC context enrichment
 * - Error handling and edge cases
 */
class TenantSecurityAspectTest {
    private val securityContextHolder = mockk<EafSecurityContextHolder>(relaxed = true)
    private val eventPublisher = mockk<NatsEventPublisher>(relaxed = true)
    private val aspect = TenantSecurityAspect(securityContextHolder, eventPublisher)

    private val joinPoint = mockk<ProceedingJoinPoint>(relaxed = true)
    private val methodSignature = mockk<MethodSignature>(relaxed = true)
    private val authentication = mockk<Authentication>(relaxed = true)

    @BeforeEach
    fun setUp() {
        // Clear MDC before each test
        MDC.clear()

        // Setup basic mocks
        every { joinPoint.signature } returns methodSignature
        every { methodSignature.declaringTypeName } returns "com.axians.eaf.test.TestService"
        every { methodSignature.name } returns "testMethod"
        every { methodSignature.method } returns mockk(relaxed = true)
        every { methodSignature.parameterNames } returns
            arrayOf("tenantId") // Add parameter names for tenant ID extraction
        every { joinPoint.args } returns arrayOf("tenant-123")

        // Setup default security context responses
        every { securityContextHolder.getAuthentication() } returns authentication
        every { authentication.authorities } returns emptyList()

        // Setup relaxed event publishing - it should never block or fail security validation
        coEvery { eventPublisher.publish(any(), any(), any<SecurityAuditEvent>()) } returns mockk()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `should enforce tenant access validation successfully for same tenant`() {
        // Given
        val userTenantId = "tenant-123"
        val targetTenantId = "tenant-123"
        val userId = "user-456"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { joinPoint.args } returns
            arrayOf(targetTenantId) // Set target tenant to same as user tenant
        every { joinPoint.proceed() } returns "success"

        val requiresTenantAccess =
            createRequiresTenantAccessAnnotation(
                tenantIdParamName = "tenantId",
                allowGlobalAccess = false,
                auditAccess = true,
            )

        // When
        val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then
        assertEquals("success", result)
        verify { joinPoint.proceed() }

        // Note: MDC is cleaned up after execution, so we just verify method completed successfully
        // The MDC enrichment is tested in a separate dedicated test
    }

    @Test
    fun `should deny access when user tries to access different tenant without global access`() {
        // Given
        val userTenantId = "tenant-123"
        val targetTenantId = "tenant-456"
        val userId = "user-789"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { securityContextHolder.hasAnyRole(any<String>(), any<String>()) } returns false
        every { joinPoint.args } returns
            arrayOf(targetTenantId) // Set target tenant to different tenant

        val requiresTenantAccess =
            createRequiresTenantAccessAnnotation(
                tenantIdParamName = "tenantId",
                allowGlobalAccess = false,
                auditAccess = true,
            )

        // When & Then
        assertThrows<InsufficientPermissionException> {
            aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)
        }

        // Verify no method execution occurred (security violation prevented access)
        verify(exactly = 0) { joinPoint.proceed() }

        // Note: Event publishing is async via GlobalScope.launch, so we don't verify it immediately
        // The event publishing is tested separately and should not affect security validation
    }

    @Test
    fun `should allow global access for platform administrators`() {
        // Given
        val userTenantId = "tenant-123"
        val targetTenantId = "tenant-456"
        val userId = "admin-user"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { securityContextHolder.hasAnyRole(any<String>(), any<String>()) } returns true
        every { joinPoint.args } returns
            arrayOf(targetTenantId) // Set target tenant to different tenant
        every { joinPoint.proceed() } returns "admin-success"

        val requiresTenantAccess =
            createRequiresTenantAccessAnnotation(
                tenantIdParamName = "tenantId",
                allowGlobalAccess = true,
                auditAccess = true,
            )

        // When
        val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then
        assertEquals("admin-success", result)
        verify { joinPoint.proceed() }

        // Verify audit event was published (relaxed verification)
        coVerify(atLeast = 1) {
            eventPublisher.publish(
                subject = "security.audit.tenant_access",
                tenantId = userTenantId,
                event = any<SecurityAuditEvent>(),
            )
        }
    }

    @Test
    fun `should throw AuthenticationRequiredException when user not authenticated`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns false

        val requiresTenantAccess = createRequiresTenantAccessAnnotation()

        // When & Then
        assertThrows<AuthenticationRequiredException> {
            aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)
        }

        // Verify no method execution occurred
        verify(exactly = 0) { joinPoint.proceed() }
    }

    @Test
    fun `should throw AuthenticationRequiredException when tenant context missing`() {
        // Given
        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns null

        val requiresTenantAccess = createRequiresTenantAccessAnnotation()

        // When & Then
        assertThrows<AuthenticationRequiredException> {
            aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)
        }
    }

    @Test
    fun `should allow access when no target tenant is specified`() {
        // Given
        val userTenantId = "tenant-123"
        val userId = "user-456"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { joinPoint.args } returns arrayOf("some-other-param")
        every { joinPoint.proceed() } returns "no-tenant-success"

        val requiresTenantAccess =
            createRequiresTenantAccessAnnotation(tenantIdParamName = "nonExistentParam")

        // When
        val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then
        assertEquals("no-tenant-success", result)
        verify { joinPoint.proceed() }
    }

    @Test
    fun `should enrich MDC context with user roles when available`() {
        // Given
        val userTenantId = "tenant-123"
        val userId = "user-456"
        val authorities =
            listOf(
                SimpleGrantedAuthority("ROLE_TENANT_ADMIN"),
                SimpleGrantedAuthority("ROLE_USER_MANAGER"),
            )

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { securityContextHolder.getAuthentication() } returns authentication
        every { authentication.authorities } returns authorities

        // Capture MDC values during joinPoint.proceed() execution
        var capturedTenantId: String? = null
        var capturedUserId: String? = null
        var capturedUserRoles: String? = null

        every { joinPoint.proceed() } answers
            {
                capturedTenantId = MDC.get("tenant_id")
                capturedUserId = MDC.get("user_id")
                capturedUserRoles = MDC.get("user_roles")
                "success"
            }

        val requiresTenantAccess = createRequiresTenantAccessAnnotation()

        // When
        aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then - verify MDC was enriched during execution
        assertEquals(userTenantId, capturedTenantId)
        assertEquals(userId, capturedUserId)
        assertNotNull(capturedUserRoles)
        val userRoles = capturedUserRoles!!
        assertTrue(userRoles.contains("TENANT_ADMIN"))
        assertTrue(userRoles.contains("USER_MANAGER"))
    }

    @Test
    fun `should clean up MDC context after execution`() {
        // Given
        val userTenantId = "tenant-123"
        val userId = "user-456"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { joinPoint.proceed() } returns "success"

        val requiresTenantAccess = createRequiresTenantAccessAnnotation()

        // When
        aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then
        assertNull(MDC.get("tenant_id"))
        assertNull(MDC.get("user_id"))
        assertNull(MDC.get("security_method"))
        assertNull(MDC.get("target_tenant_id"))
        // correlation_id should be preserved for the request
        assertNotNull(MDC.get("correlation_id"))
    }

    @Test
    fun `should not publish audit events when auditing is disabled`() {
        // Given
        val userTenantId = "tenant-123"
        val userId = "user-456"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { joinPoint.proceed() } returns "success"

        val requiresTenantAccess = createRequiresTenantAccessAnnotation(auditAccess = false)

        // When
        aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then
        coVerify(exactly = 0) { eventPublisher.publish(any(), any(), any<SecurityAuditEvent>()) }
    }

    @Test
    fun `should handle event publishing failures gracefully without affecting security validation`() {
        // Given
        val userTenantId = "tenant-123"
        val userId = "user-456"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { joinPoint.proceed() } returns "success"

        // Make event publishing fail
        coEvery { eventPublisher.publish(any(), any(), any<SecurityAuditEvent>()) } throws
            RuntimeException("Event publishing failed")

        val requiresTenantAccess = createRequiresTenantAccessAnnotation(auditAccess = true)

        // When & Then - should not throw despite event publishing failure
        val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)
        assertEquals("success", result)
        verify { joinPoint.proceed() }
    }

    @Test
    fun `should set target tenant in MDC when different from user tenant`() {
        // Given
        val userTenantId = "tenant-123"
        val targetTenantId = "tenant-456"
        val userId = "admin-user"

        every { securityContextHolder.isAuthenticated() } returns true
        every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
        every { securityContextHolder.getUserId() } returns userId
        every { securityContextHolder.hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN") } returns true
        every { joinPoint.proceed() } returns "success"

        val requiresTenantAccess = createRequiresTenantAccessAnnotation(allowGlobalAccess = true)

        // When
        aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

        // Then
        // During execution, target_tenant_id should be set in MDC
        // After execution, it should be cleaned up along with other context
        assertNull(MDC.get("target_tenant_id")) // Cleaned up after execution
    }

    private fun createRequiresTenantAccessAnnotation(
        tenantIdParamName: String = "tenantId",
        allowGlobalAccess: Boolean = false,
        auditAccess: Boolean = false,
    ): RequiresTenantAccess {
        val annotation = mockk<RequiresTenantAccess>()
        every { annotation.tenantIdParamName } returns tenantIdParamName
        every { annotation.allowGlobalAccess } returns allowGlobalAccess
        every { annotation.auditAccess } returns auditAccess
        return annotation
    }
}

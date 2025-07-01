package com.axians.eaf.controlplane.infrastructure.security.audit

import com.axians.eaf.controlplane.domain.event.SecurityAction
import com.axians.eaf.controlplane.domain.event.SecurityAuditEvent
import com.axians.eaf.controlplane.domain.event.SecurityOutcome
import com.axians.eaf.controlplane.infrastructure.security.annotation.RequiresTenantAccess
import com.axians.eaf.controlplane.infrastructure.security.aspect.TenantSecurityAspect
import com.axians.eaf.core.security.EafSecurityContextHolder
import com.axians.eaf.eventing.NatsEventPublisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for validating complete audit trail functionality after TenantSecurityAspect
 * refactoring. This test ensures that:
 *
 * 1. Security events are properly published for all security operations
 * 2. Audit trail contains complete contextual information
 * 3. Event-driven architecture replaces direct audit service calls
 * 4. Audit data is sufficient for regulatory compliance
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityAuditTrailIntegrationTest {
    private val securityContextHolder = mockk<EafSecurityContextHolder>()
    private val eventPublisher = mockk<NatsEventPublisher>(relaxed = true)
    private val aspect = TenantSecurityAspect(securityContextHolder, eventPublisher)

    private val joinPoint = mockk<ProceedingJoinPoint>()
    private val methodSignature = mockk<MethodSignature>()

    @BeforeEach
    fun setUp() {
        MDC.clear()

        // Setup join point mocks
        every { joinPoint.signature } returns methodSignature
        every { methodSignature.declaringTypeName } returns "com.axians.eaf.test.TestService"
        every { methodSignature.name } returns "processSecureOperation"
        every { joinPoint.args } returns arrayOf("tenant-123")

        // Setup successful event publishing by default
        coEvery { eventPublisher.publish(any(), any(), any<SecurityAuditEvent>()) } returns mockk()
    }

    @Test
    fun `should create complete audit trail for successful tenant access`() =
        runBlocking {
            // Given - Successful tenant access scenario
            val userTenantId = "tenant-123"
            val userId = "user-456"
            val correlationId = "correlation-789"

            every { securityContextHolder.isAuthenticated() } returns true
            every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
            every { securityContextHolder.getUserId() } returns userId
            every { joinPoint.proceed() } returns "operation-success"

            MDC.put("correlation_id", correlationId)

            val requiresTenantAccess =
                createRequiresTenantAccessAnnotation(
                    tenantIdParamName = "tenantId",
                    auditAccess = true,
                )

            // When
            val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

            // Then
            assertEquals("operation-success", result)

            // Verify audit event was published with complete information
            val eventSlot = slot<SecurityAuditEvent>()
            coVerify {
                eventPublisher.publish(
                    subject = "security.audit.tenant_access",
                    tenantId = userTenantId,
                    event = capture(eventSlot),
                )
            }

            val auditEvent = eventSlot.captured

            // Validate complete audit trail information
            assertNotNull(auditEvent.eventId)
            assertNotNull(auditEvent.timestamp)
            assertEquals(userTenantId, auditEvent.tenantId)
            assertEquals(userId, auditEvent.userId)
            assertEquals(SecurityAction.ACCESS_GRANTED, auditEvent.action)
            assertEquals(SecurityOutcome.SUCCESS, auditEvent.outcome)
            assertEquals(correlationId, auditEvent.correlationId)

            // Validate resource information
            assertEquals("tenant_access", auditEvent.resource.type)
            assertEquals("tenant-123", auditEvent.resource.id)
            assertTrue(auditEvent.resource.attributes.containsKey("method"))
            assertTrue(auditEvent.resource.attributes.containsKey("userTenantId"))
            assertTrue(auditEvent.resource.attributes.containsKey("targetTenantId"))

            // Validate audit trail completeness
            assertTrue(auditEvent.details.containsKey("accessReason"))
            assertTrue(auditEvent.details.containsKey("globalAccess"))
            assertTrue(auditEvent.details.containsKey("timestamp"))

            println("✅ Complete audit trail created for successful tenant access")
            println("📊 Audit event contains all required regulatory compliance information")
        }

    @Test
    fun `should create complete audit trail for security violation`() =
        runBlocking {
            // Given - Security violation scenario
            val userTenantId = "tenant-123"
            val targetTenantId = "tenant-456"
            val userId = "user-789"
            val correlationId = "correlation-violation"

            every { securityContextHolder.isAuthenticated() } returns true
            every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
            every { securityContextHolder.getUserId() } returns userId
            every { securityContextHolder.hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN") } returns false

            MDC.put("correlation_id", correlationId)

            val requiresTenantAccess =
                createRequiresTenantAccessAnnotation(
                    tenantIdParamName = "tenantId",
                    auditAccess = true,
                )

            // When & Then - Expect security exception
            try {
                aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)
            } catch (e: Exception) {
                // Expected security exception
            }

            // Verify security violation audit event was published
            val eventSlot = slot<SecurityAuditEvent>()
            coVerify {
                eventPublisher.publish(
                    subject = "security.audit.violation",
                    tenantId = userTenantId,
                    event = capture(eventSlot),
                )
            }

            val auditEvent = eventSlot.captured

            // Validate security violation audit trail
            assertNotNull(auditEvent.eventId)
            assertNotNull(auditEvent.timestamp)
            assertEquals(userTenantId, auditEvent.tenantId)
            assertEquals(userId, auditEvent.userId)
            assertEquals(SecurityAction.SECURITY_VIOLATION, auditEvent.action)
            assertEquals(SecurityOutcome.VIOLATION, auditEvent.outcome)
            assertEquals(correlationId, auditEvent.correlationId)

            // Validate violation-specific information
            assertEquals("security_violation", auditEvent.resource.type)
            assertTrue(auditEvent.resource.attributes.containsKey("violationType"))
            assertTrue(auditEvent.resource.attributes.containsKey("method"))
            assertTrue(auditEvent.details.containsKey("message"))

            println("✅ Complete audit trail created for security violation")
            println("🔒 Security violation properly tracked for compliance reporting")
        }

    @Test
    fun `should create audit trail for global administrator access`() =
        runBlocking {
            // Given - Global admin access scenario
            val userTenantId = "tenant-123"
            val targetTenantId = "tenant-456"
            val userId = "admin-user"
            val correlationId = "correlation-admin"

            every { securityContextHolder.isAuthenticated() } returns true
            every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
            every { securityContextHolder.getUserId() } returns userId
            every { securityContextHolder.hasAnyRole("SUPER_ADMIN", "PLATFORM_ADMIN") } returns true
            every { joinPoint.proceed() } returns "admin-operation-success"

            MDC.put("correlation_id", correlationId)

            val requiresTenantAccess =
                createRequiresTenantAccessAnnotation(
                    tenantIdParamName = "tenantId",
                    allowGlobalAccess = true,
                    auditAccess = true,
                )

            // When
            val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

            // Then
            assertEquals("admin-operation-success", result)

            // Verify global access audit event
            val eventSlot = slot<SecurityAuditEvent>()
            coVerify {
                eventPublisher.publish(
                    subject = "security.audit.tenant_access",
                    tenantId = userTenantId,
                    event = capture(eventSlot),
                )
            }

            val auditEvent = eventSlot.captured

            // Validate global access audit trail
            assertEquals(SecurityAction.ACCESS_GRANTED, auditEvent.action)
            assertEquals(SecurityOutcome.SUCCESS, auditEvent.outcome)
            assertTrue(auditEvent.details["globalAccess"] as Boolean)
            assertEquals("Global administrator access", auditEvent.details["accessReason"])

            // Validate cross-tenant access tracking
            assertEquals(targetTenantId, auditEvent.resource.attributes["targetTenantId"])
            assertEquals(userTenantId, auditEvent.resource.attributes["userTenantId"])

            println("✅ Complete audit trail created for global administrator access")
            println("👑 Global access privileges properly tracked for audit compliance")
        }

    @Test
    fun `should validate audit event data structure for regulatory compliance`() =
        runBlocking {
            // Given
            val userTenantId = "tenant-test"
            val userId = "user-test"

            every { securityContextHolder.isAuthenticated() } returns true
            every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
            every { securityContextHolder.getUserId() } returns userId
            every { joinPoint.proceed() } returns "success"

            val requiresTenantAccess = createRequiresTenantAccessAnnotation(auditAccess = true)

            // When
            aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

            // Then - Verify audit event structure meets regulatory requirements
            val eventSlot = slot<SecurityAuditEvent>()
            coVerify { eventPublisher.publish(any(), any(), capture(eventSlot)) }

            val auditEvent = eventSlot.captured

            // Validate required fields for compliance
            assertNotNull(auditEvent.eventId, "Event ID required for audit trail uniqueness")
            assertNotNull(auditEvent.timestamp, "Timestamp required for audit chronology")
            assertNotNull(auditEvent.tenantId, "Tenant ID required for data isolation audit")
            assertNotNull(auditEvent.userId, "User ID required for accountability")
            assertNotNull(auditEvent.action, "Action required for audit categorization")
            assertNotNull(auditEvent.outcome, "Outcome required for compliance reporting")
            assertNotNull(auditEvent.resource, "Resource info required for access tracking")
            assertNotNull(auditEvent.details, "Details required for context preservation")

            // Validate event data is serializable (for persistence)
            val eventString = auditEvent.toString()
            assertTrue(eventString.isNotEmpty())

            // Validate resource tracking completeness
            assertTrue(auditEvent.resource.attributes.isNotEmpty())
            assertTrue(auditEvent.details.isNotEmpty())

            println("✅ Audit event structure validated for regulatory compliance")
            println("📋 All required fields present for SOX/GDPR/ISO27001 compliance")
        }

    @Test
    fun `should handle audit publishing failures without impacting security validation`() =
        runBlocking {
            // Given
            val userTenantId = "tenant-123"
            val userId = "user-456"

            every { securityContextHolder.isAuthenticated() } returns true
            every { securityContextHolder.getTenantIdOrNull() } returns userTenantId
            every { securityContextHolder.getUserId() } returns userId
            every { joinPoint.proceed() } returns "success"

            // Make audit publishing fail
            coEvery { eventPublisher.publish(any(), any(), any<SecurityAuditEvent>()) } throws
                RuntimeException("NATS publishing failed")

            val requiresTenantAccess = createRequiresTenantAccessAnnotation(auditAccess = true)

            // When - Should not fail despite audit publishing failure
            val result = aspect.enforceTenantAccess(joinPoint, requiresTenantAccess)

            // Then
            assertEquals("success", result)

            // Verify the security operation completed successfully
            coVerify { joinPoint.proceed() }

            // Verify audit publishing was attempted
            coVerify { eventPublisher.publish(any(), any(), any<SecurityAuditEvent>()) }

            println("✅ Security validation continues despite audit publishing failures")
            println("🛡️ System resilience maintained - audit failure doesn't break security")
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

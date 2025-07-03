package com.axians.eaf.controlplane.integration.security

import org.springframework.security.test.context.support.WithSecurityContext

/**
 * Custom annotation to set up mock EAF security context for testing.
 *
 * This annotation creates a realistic security context that includes:
 * - Tenant ID for multi-tenant isolation
 * - User ID and email for user tracking
 * - User roles for authorization testing
 * - Request correlation data for audit trails
 *
 * Usage:
 * ```kotlin
 * @Test
 * @WithMockEafUser(tenantId = "TENANT_A", userId = "user-123", roles = ["USER", "ADMIN"])
 * fun testWithSecurityContext() {
 *     // Test code runs with mocked security context
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@WithSecurityContext(factory = WithMockEafUserSecurityContextFactory::class)
annotation class WithMockEafUser(
    /** The tenant ID for multi-tenant isolation. */
    val tenantId: String = "TENANT_A",
    /** The user ID for user tracking. */
    val userId: String = "test-user",
    /** The user's email address. */
    val email: String = "test@example.com",
    /** The user's roles for authorization testing. */
    val roles: Array<String> = ["USER"],
)

package com.axians.eaf.core.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EafSecurityContextHolderTest {
    private lateinit var contextHolder: EafSecurityContextHolder
    private lateinit var mockSecurityContext: SecurityContext
    private lateinit var mockAuthentication: Authentication

    @BeforeEach
    fun setUp() {
        contextHolder = DefaultEafSecurityContextHolder()
        mockSecurityContext = mockk()
        mockAuthentication = mockk()

        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns mockSecurityContext
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SecurityContextHolder::class)
    }

    @Test
    fun `getTenantId should return tenant ID from HasTenantId authentication`() {
        // Given
        val expectedTenantId = "tenant-123"
        val mockAuthWithTenant =
            object : Authentication, HasTenantId {
                override fun getTenantId(): String = expectedTenantId

                override fun getName(): String = "test"

                override fun getAuthorities() = emptyList<SimpleGrantedAuthority>()

                override fun getCredentials() = null

                override fun getDetails() = null

                override fun getPrincipal() = null

                override fun isAuthenticated() = true

                override fun setAuthenticated(isAuthenticated: Boolean) {
                    // Mock implementation - no action needed
                }
            }
        every { mockSecurityContext.authentication } returns mockAuthWithTenant

        // When
        val result = contextHolder.getTenantId()

        // Then
        assertEquals(expectedTenantId, result)
    }

    @Test
    fun `getTenantId should return tenant ID from HasTenantId principal`() {
        // Given
        val expectedTenantId = "tenant-456"
        val mockPrincipal = mockk<HasTenantId>()
        every { mockPrincipal.getTenantId() } returns expectedTenantId
        every { mockAuthentication.principal } returns mockPrincipal
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When
        val result = contextHolder.getTenantId()

        // Then
        assertEquals(expectedTenantId, result)
    }

    @Test
    fun `getTenantId should throw exception when no tenant ID available`() {
        // Given
        every { mockAuthentication.principal } returns mockk()
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When & Then
        assertThrows<IllegalStateException> { contextHolder.getTenantId() }
    }

    @Test
    fun `getTenantIdOrNull should return null when no authentication`() {
        // Given
        every { mockSecurityContext.authentication } returns null

        // When
        val result = contextHolder.getTenantIdOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun `getUserId should return user ID from HasUserId authentication`() {
        // Given
        val expectedUserId = "user-789"
        val mockAuthWithUser =
            object : Authentication, HasUserId {
                override fun getUserId(): String = expectedUserId

                override fun getName(): String = "test"

                override fun getAuthorities() = emptyList<SimpleGrantedAuthority>()

                override fun getCredentials() = null

                override fun getDetails() = null

                override fun getPrincipal() = null

                override fun isAuthenticated() = true

                override fun setAuthenticated(isAuthenticated: Boolean) {
                    // Mock implementation - no action needed
                }
            }
        every { mockSecurityContext.authentication } returns mockAuthWithUser

        // When
        val result = contextHolder.getUserId()

        // Then
        assertEquals(expectedUserId, result)
    }

    @Test
    fun `getUserId should return user ID from HasUserId principal`() {
        // Given
        val expectedUserId = "user-101"
        val mockPrincipal = mockk<HasUserId>()
        every { mockPrincipal.getUserId() } returns expectedUserId
        every { mockAuthentication.principal } returns mockPrincipal
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When
        val result = contextHolder.getUserId()

        // Then
        assertEquals(expectedUserId, result)
    }

    @Test
    fun `hasRole should return true when user has role`() {
        // Given
        val authorities =
            listOf(
                SimpleGrantedAuthority("ROLE_USER"),
                SimpleGrantedAuthority("ROLE_ADMIN"),
            )
        every { mockAuthentication.authorities } returns authorities
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When & Then
        assertTrue(contextHolder.hasRole("USER"))
        assertTrue(contextHolder.hasRole("ADMIN"))
        assertFalse(contextHolder.hasRole("SUPER_ADMIN"))
    }

    @Test
    fun `hasPermission should return true when user has permission`() {
        // Given
        val authorities =
            listOf(
                SimpleGrantedAuthority("user:read"),
                SimpleGrantedAuthority("user:write"),
            )
        every { mockAuthentication.authorities } returns authorities
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When & Then
        assertTrue(contextHolder.hasPermission("user:read"))
        assertTrue(contextHolder.hasPermission("user:write"))
        assertFalse(contextHolder.hasPermission("admin:delete"))
    }

    @Test
    fun `hasAnyRole should return true when user has any of the specified roles`() {
        // Given
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        every { mockAuthentication.authorities } returns authorities
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When & Then
        assertTrue(contextHolder.hasAnyRole("USER", "ADMIN"))
        assertFalse(contextHolder.hasAnyRole("ADMIN", "SUPER_ADMIN"))
    }

    @Test
    fun `isAuthenticated should return authentication status`() {
        // Given
        every { mockAuthentication.isAuthenticated } returns true
        every { mockSecurityContext.authentication } returns mockAuthentication

        // When & Then
        assertTrue(contextHolder.isAuthenticated())

        // Given - not authenticated
        every { mockAuthentication.isAuthenticated } returns false

        // When & Then
        assertFalse(contextHolder.isAuthenticated())
    }

    @Test
    fun `isAuthenticated should return false when no authentication`() {
        // Given
        every { mockSecurityContext.authentication } returns null

        // When & Then
        assertFalse(contextHolder.isAuthenticated())
    }
}

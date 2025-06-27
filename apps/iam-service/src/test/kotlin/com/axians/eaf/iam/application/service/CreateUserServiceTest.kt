package com.axians.eaf.iam.application.service

import com.axians.eaf.iam.application.port.inbound.CreateUserCommand
import com.axians.eaf.iam.application.port.outbound.FindUsersByTenantIdPort
import com.axians.eaf.iam.application.port.outbound.SaveTenantPort
import com.axians.eaf.iam.domain.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CreateUserServiceTest {
    private lateinit var findUsersByTenantIdPort: FindUsersByTenantIdPort
    private lateinit var saveTenantPort: SaveTenantPort
    private lateinit var createUserService: CreateUserService

    @BeforeEach
    fun setUp() {
        findUsersByTenantIdPort = mockk()
        saveTenantPort = mockk()
        createUserService = CreateUserService(findUsersByTenantIdPort, saveTenantPort)
    }

    @Test
    fun `should create user successfully when valid command provided`() {
        // Given
        val command =
            CreateUserCommand(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )
        val expectedUser =
            User.createUser(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )

        every { saveTenantPort.existsByEmail("user@example.com") } returns false
        every { findUsersByTenantIdPort.saveUser(any()) } returns expectedUser

        // When
        val result = createUserService.createUser(command)

        // Then
        assertEquals("tenant-123", result.tenantId)
        assertEquals("user@example.com", result.email)
        assertEquals("testuser", result.username)
        assertEquals("PENDING_ACTIVATION", result.status)

        verify { saveTenantPort.existsByEmail("user@example.com") }
        verify { findUsersByTenantIdPort.saveUser(any()) }
    }

    @Test
    fun `should throw exception when email already exists`() {
        // Given
        val command =
            CreateUserCommand(
                tenantId = "tenant-123",
                email = "existing@example.com",
                username = "testuser",
            )

        every { saveTenantPort.existsByEmail("existing@example.com") } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createUserService.createUser(command) }

        assertEquals("User with email existing@example.com already exists", exception.message)
        verify { saveTenantPort.existsByEmail("existing@example.com") }
        verify(exactly = 0) { findUsersByTenantIdPort.saveUser(any()) }
    }

    @Test
    fun `should throw exception when tenant id is blank`() {
        // Given
        val command =
            CreateUserCommand(
                tenantId = "",
                email = "user@example.com",
                username = "testuser",
            )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createUserService.createUser(command) }

        assertEquals("Tenant ID cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when email is blank`() {
        // Given
        val command =
            CreateUserCommand(
                tenantId = "tenant-123",
                email = "",
                username = "testuser",
            )

        // When & Then
        val exception = assertThrows<IllegalArgumentException> { createUserService.createUser(command) }

        assertEquals("Email cannot be blank", exception.message)
    }
}

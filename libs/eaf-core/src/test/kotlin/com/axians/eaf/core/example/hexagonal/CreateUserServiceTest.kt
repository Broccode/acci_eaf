package com.axians.eaf.core.example.hexagonal

import com.axians.eaf.core.example.hexagonal.application.port.inbound.CreateUserCommand
import com.axians.eaf.core.example.hexagonal.application.service.CreateUserService
import com.axians.eaf.core.example.hexagonal.domain.model.User
import com.axians.eaf.core.example.hexagonal.domain.port.out.UserRepository
import com.axians.eaf.core.example.hexagonal.infrastructure.adapter.out.persistence.InMemoryUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CreateUserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var createUserService: CreateUserService

    @BeforeEach
    fun setUp() {
        userRepository = InMemoryUserRepository()
        createUserService = CreateUserService(userRepository)
    }

    @Test
    fun `should create user when valid command is provided`() {
        // Given
        val command =
            CreateUserCommand(
                username = "john.doe",
                email = "john.doe@example.com",
                tenantId = "tenant-1",
            )

        // When
        val result = createUserService.handle(command)

        // Then
        assertNotNull(result.userId)
        assertEquals("john.doe", result.username)
        assertEquals("john.doe@example.com", result.email)
        assertEquals("tenant-1", result.tenantId)

        // Verify user was saved
        val savedUser = userRepository.findById(result.userId, "tenant-1")
        assertNotNull(savedUser)
        assertEquals("john.doe", savedUser!!.username)
    }

    @Test
    fun `should throw exception when username is blank`() {
        // Given
        val command =
            CreateUserCommand(
                username = "",
                email = "john.doe@example.com",
                tenantId = "tenant-1",
            )

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                createUserService.handle(command)
            }
        assertEquals("Username cannot be blank", exception.message)
    }

    @Test
    fun `should throw exception when email is invalid`() {
        // Given
        val command =
            CreateUserCommand(
                username = "john.doe",
                email = "invalid-email",
                tenantId = "tenant-1",
            )

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                createUserService.handle(command)
            }
        assertEquals("Email must be valid", exception.message)
    }

    @Test
    fun `should throw exception when user already exists`() {
        // Given
        val existingUser =
            User(
                id = "existing-id",
                username = "john.doe",
                email = "john.doe@example.com",
                tenantId = "tenant-1",
            )
        userRepository.save(existingUser)

        val command =
            CreateUserCommand(
                username = "john.doe",
                email = "different@example.com",
                tenantId = "tenant-1",
            )

        // When & Then
        val exception =
            assertThrows<IllegalArgumentException> {
                createUserService.handle(command)
            }
        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `should allow same username in different tenants`() {
        // Given
        val existingUser =
            User(
                id = "existing-id",
                username = "john.doe",
                email = "john.doe@example.com",
                tenantId = "tenant-1",
            )
        userRepository.save(existingUser)

        val command =
            CreateUserCommand(
                username = "john.doe",
                email = "john.doe@example.com",
                tenantId = "tenant-2",
            )

        // When
        val result = createUserService.handle(command)

        // Then
        assertNotNull(result.userId)
        assertEquals("john.doe", result.username)
        assertEquals("tenant-2", result.tenantId)
    }
}

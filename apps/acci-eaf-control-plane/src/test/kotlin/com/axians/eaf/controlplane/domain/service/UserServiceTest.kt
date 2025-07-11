package com.axians.eaf.controlplane.domain.service

import com.axians.eaf.controlplane.application.dto.user.UserFilter
import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.User
import com.axians.eaf.controlplane.domain.model.user.UserId
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.port.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    private val testTenantId = TenantId.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val testUserId = UserId.fromString("660e8400-e29b-41d4-a716-446655440000")
    private val testEmail = "test@example.com"
    private val testFirstName = "John"
    private val testLastName = "Doe"

    @BeforeEach
    fun setUp() {
        userRepository = mockk(relaxed = true)
        userService = UserService(userRepository)
    }

    @Test
    fun `should create user successfully when email is unique`() =
        runTest {
            // Given
            coEvery { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) } returns false
            val createdUser =
                User.createPending(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )
            coEvery { userRepository.save(createdUser) } returns createdUser

            // When
            val result =
                userService.createUser(
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                    tenantId = testTenantId.value,
                    activateImmediately = false,
                )

            // Then
            assertTrue(result is CreateUserResult.Success)
            val success = result as CreateUserResult.Success
            assertEquals(testEmail, success.email)
            assertEquals("John Doe", success.fullName)
            assertEquals(UserStatus.PENDING, success.status)

            coVerify { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) }
        }

    @Test
    fun `should create active user when activateImmediately is true`() =
        runTest {
            // Given
            coEvery { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) } returns false
            val activeUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )
            coEvery { userRepository.save(activeUser) } returns activeUser

            // When
            val result =
                userService.createUser(
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                    tenantId = testTenantId.value,
                    activateImmediately = true,
                )

            // Then
            assertTrue(result is CreateUserResult.Success)
            val success = result as CreateUserResult.Success
            assertEquals(UserStatus.ACTIVE, success.status)
        }

    @Test
    fun `should fail to create user when email already exists`() =
        runTest {
            // Given
            coEvery { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) } returns true

            // When
            val result =
                userService.createUser(
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                    tenantId = testTenantId.value,
                )

            // Then
            assertTrue(result is CreateUserResult.Failure)
            val failure = result as CreateUserResult.Failure
            assertContains(failure.message, "already exists")

            coVerify { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) }
        }

    @Test
    fun `should get user details successfully`() =
        runTest {
            // Given
            val user =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            coEvery { userRepository.findById(testUserId) } returns user

            // When
            val result = userService.getUserDetails(testUserId.value)

            // Then
            assertTrue(result is UserDetailsResult.Success)
            val success = result as UserDetailsResult.Success
            assertEquals(
                user.getId().value,
                success.details.user
                    .getId()
                    .value,
            )
            assertEquals(testEmail, success.details.user.getEmail())
            assertEquals("John Doe", success.details.user.getFullName())

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should return not found when user does not exist`() =
        runTest {
            // Given
            coEvery { userRepository.findById(testUserId) } returns null

            // When
            val result = userService.getUserDetails(testUserId.value)

            // Then
            assertTrue(result is UserDetailsResult.NotFound)
            val notFound = result as UserDetailsResult.NotFound
            assertContains(notFound.message, "not found")

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should activate user successfully`() =
        runTest {
            // Given
            val pendingUser =
                User.createPending(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            // Simulate the activated user - activate() is void, so we need to mock the repository
            // behavior
            val activatedUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            coEvery { userRepository.findById(testUserId) } returns pendingUser
            coEvery { userRepository.save(pendingUser) } returns activatedUser

            // When
            val result = userService.activateUser(testUserId.value)

            // Then
            assertTrue(result is UserStatusResult.Success)
            val success = result as UserStatusResult.Success
            assertEquals(testUserId.value, success.userId)
            assertEquals(UserStatus.PENDING, success.oldStatus)
            assertEquals(UserStatus.ACTIVE, success.newStatus)
            assertEquals("activated", success.action)

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should fail to activate already active user`() =
        runTest {
            // Given
            val activeUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            coEvery { userRepository.findById(testUserId) } returns activeUser

            // When
            val result = userService.activateUser(testUserId.value)

            // Then
            assertTrue(result is UserStatusResult.Failure)
            val failure = result as UserStatusResult.Failure
            assertContains(failure.message, "Cannot activate")

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should suspend user successfully`() =
        runTest {
            // Given
            val activeUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            // Simulate the suspended user - suspend() is void, so we create a new user with suspended
            // status
            val suspendedUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )
            // Note: We can't easily simulate suspended status without complex mocking

            coEvery { userRepository.findById(testUserId) } returns activeUser
            coEvery { userRepository.save(activeUser) } returns suspendedUser

            // When
            val result = userService.suspendUser(testUserId.value)

            // Then
            assertTrue(result is UserStatusResult.Success)
            val success = result as UserStatusResult.Success
            assertEquals(UserStatus.ACTIVE, success.oldStatus)
            assertEquals(UserStatus.SUSPENDED, success.newStatus)
            assertEquals("suspended", success.action)

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should fail to suspend pending user`() =
        runTest {
            // Given
            val pendingUser =
                User.createPending(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            coEvery { userRepository.findById(testUserId) } returns pendingUser

            // When
            val result = userService.suspendUser(testUserId.value)

            // Then
            assertTrue(result is UserStatusResult.Failure)
            val failure = result as UserStatusResult.Failure
            assertContains(failure.message, "Cannot suspend")

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should initiate password reset successfully`() =
        runTest {
            // Given
            val activeUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )

            coEvery { userRepository.findById(testUserId) } returns activeUser

            // When
            val result = userService.resetPassword(testUserId.value)

            // Then
            assertTrue(result is PasswordResetResult.Success)
            val success = result as PasswordResetResult.Success
            assertEquals(testUserId.value, success.userId)
            assertEquals(testEmail, success.email)
            assertTrue(success.resetToken.isNotBlank())
            assertTrue(success.expiresAt.isAfter(Instant.now()))

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should fail to reset password for deactivated user`() =
        runTest {
            // Given
            val activeUser =
                User.createActive(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )
            // Simulate deactivation by calling deactivate() method
            activeUser.deactivate()

            coEvery { userRepository.findById(testUserId) } returns activeUser

            // When
            val result = userService.resetPassword(testUserId.value)

            // Then
            assertTrue(result is PasswordResetResult.Failure)
            val failure = result as PasswordResetResult.Failure
            assertContains(failure.message, "Cannot reset password")

            coVerify { userRepository.findById(testUserId) }
        }

    @Test
    fun `should list users with filter`() =
        runTest {
            // Given
            val filter = UserFilter(tenantId = testTenantId.value, page = 0, size = 10)
            val mockPagedResponse =
                mockk<
                    com.axians.eaf.controlplane.application.dto.tenant.PagedResponse<
                        com.axians.eaf.controlplane.application.dto.user.UserSummary,
                    >,
                >()

            coEvery { userRepository.findAll(filter) } returns mockPagedResponse

            // When
            val result = userService.listUsers(filter)

            // Then
            assertEquals(mockPagedResponse, result)

            coVerify { userRepository.findAll(filter) }
        }

    @Test
    fun `should handle repository exceptions gracefully in createUser`() =
        runTest {
            // Given
            coEvery { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) } throws
                RuntimeException("Database error")

            // When
            val result =
                userService.createUser(
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                    tenantId = testTenantId.value,
                )

            // Then
            assertTrue(result is CreateUserResult.Failure)

            coVerify { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) }
        }

    @Test
    fun `should normalize email addresses`() =
        runTest {
            // Given
            val uppercaseEmail = "TEST@EXAMPLE.COM"
            coEvery {
                userRepository.existsByEmailAndTenantId(
                    uppercaseEmail.lowercase(),
                    testTenantId,
                )
            } returns false
            val createdUser =
                User.createPending(
                    tenantId = testTenantId,
                    email = uppercaseEmail.lowercase(),
                    firstName = testFirstName,
                    lastName = testLastName,
                )
            coEvery { userRepository.save(createdUser) } returns createdUser

            // When
            val result =
                userService.createUser(
                    email = uppercaseEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                    tenantId = testTenantId.value,
                )

            // Then
            assertTrue(result is CreateUserResult.Success)
            val success = result as CreateUserResult.Success
            assertEquals("test@example.com", success.email)

            coVerify { userRepository.existsByEmailAndTenantId("test@example.com", testTenantId) }
        }

    @Test
    fun `should trim whitespace from names`() =
        runTest {
            // Given
            val firstNameWithSpaces = "  John  "
            val lastNameWithSpaces = "  Doe  "
            coEvery { userRepository.existsByEmailAndTenantId(testEmail, testTenantId) } returns false
            val createdUser =
                User.createPending(
                    tenantId = testTenantId,
                    email = testEmail,
                    firstName = testFirstName,
                    lastName = testLastName,
                )
            coEvery { userRepository.save(createdUser) } returns createdUser

            // When
            val result =
                userService.createUser(
                    email = testEmail,
                    firstName = firstNameWithSpaces,
                    lastName = lastNameWithSpaces,
                    tenantId = testTenantId.value,
                )

            // Then
            assertTrue(result is CreateUserResult.Success)
            val success = result as CreateUserResult.Success
            assertEquals("John Doe", success.fullName)
        }
}

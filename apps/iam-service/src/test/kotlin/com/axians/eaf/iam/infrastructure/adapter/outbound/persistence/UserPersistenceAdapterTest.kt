package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity.toEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserPersistenceAdapterTest {
    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var userPersistenceAdapter: UserPersistenceAdapter

    @BeforeEach
    fun setUp() {
        userJpaRepository = mockk()
        userPersistenceAdapter = UserPersistenceAdapter(userJpaRepository)
    }

    @Test
    fun `should find users by tenant id successfully`() {
        // Given
        val tenantId = "tenant-123"
        val user1 =
            User.createUser(
                tenantId = tenantId,
                email = "user1@example.com",
                username = "user1",
            )
        val user2 =
            User.createTenantAdmin(
                tenantId = tenantId,
                email = "admin@example.com",
            )
        val userEntities = listOf(user1.toEntity(), user2.toEntity())

        every { userJpaRepository.findByTenantId(tenantId) } returns userEntities

        // When
        val result = userPersistenceAdapter.findUsersByTenantId(tenantId)

        // Then
        assertEquals(2, result.size)
        assertEquals("user1@example.com", result[0].email)
        assertEquals("admin@example.com", result[1].email)

        verify { userJpaRepository.findByTenantId(tenantId) }
    }

    @Test
    fun `should return empty list when no users found in tenant`() {
        // Given
        val tenantId = "empty-tenant"

        every { userJpaRepository.findByTenantId(tenantId) } returns emptyList()

        // When
        val result = userPersistenceAdapter.findUsersByTenantId(tenantId)

        // Then
        assertTrue(result.isEmpty())

        verify { userJpaRepository.findByTenantId(tenantId) }
    }

    @Test
    fun `should find user by id and tenant id successfully`() {
        // Given
        val userId = "user-123"
        val tenantId = "tenant-123"
        val user =
            User.createUser(
                tenantId = tenantId,
                email = "user@example.com",
                username = "testuser",
            )
        val userEntity = user.toEntity().copy(userId = userId)

        every { userJpaRepository.findById(userId) } returns java.util.Optional.of(userEntity)

        // When
        val result = userPersistenceAdapter.findUserByIdAndTenantId(userId, tenantId)

        // Then
        assertEquals(userId, result?.userId)
        assertEquals(tenantId, result?.tenantId)
        assertEquals("user@example.com", result?.email)

        verify { userJpaRepository.findById(userId) }
    }

    @Test
    fun `should return null when user not found by id`() {
        // Given
        val userId = "nonexistent-user"
        val tenantId = "tenant-123"

        every { userJpaRepository.findById(userId) } returns java.util.Optional.empty()

        // When
        val result = userPersistenceAdapter.findUserByIdAndTenantId(userId, tenantId)

        // Then
        assertNull(result)

        verify { userJpaRepository.findById(userId) }
    }

    @Test
    fun `should return null when user found but belongs to different tenant`() {
        // Given
        val userId = "user-123"
        val tenantId = "tenant-123"
        val differentTenantId = "different-tenant"
        val user =
            User.createUser(
                tenantId = differentTenantId,
                email = "user@example.com",
                username = "testuser",
            )
        val userEntity = user.toEntity().copy(userId = userId)

        every { userJpaRepository.findById(userId) } returns java.util.Optional.of(userEntity)

        // When
        val result = userPersistenceAdapter.findUserByIdAndTenantId(userId, tenantId)

        // Then
        assertNull(result)

        verify { userJpaRepository.findById(userId) }
    }

    @Test
    fun `should save user successfully`() {
        // Given
        val user =
            User.createUser(
                tenantId = "tenant-123",
                email = "user@example.com",
                username = "testuser",
            )
        val userEntity = user.toEntity()
        val savedEntity = userEntity.copy(userId = "saved-user-id")

        every { userJpaRepository.save(userEntity) } returns savedEntity

        // When
        val result = userPersistenceAdapter.saveUser(user)

        // Then
        assertEquals("saved-user-id", result.userId)
        assertEquals("tenant-123", result.tenantId)
        assertEquals("user@example.com", result.email)

        verify { userJpaRepository.save(userEntity) }
    }
}

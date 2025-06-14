package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence

import com.axians.eaf.iam.PostgresTestcontainerConfiguration
import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.domain.model.UserRole
import com.axians.eaf.iam.domain.model.UserStatus
import com.axians.eaf.iam.infrastructure.config.JpaConfig
import com.axians.eaf.iam.test.TestIamServiceApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@SpringBootTest(classes = [TestIamServiceApplication::class])
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import(PostgresTestcontainerConfiguration::class, JpaConfig::class)
class UserPersistenceAdapterTest {
    @Autowired
    private lateinit var userPersistenceAdapter: UserPersistenceAdapter

    @Test
    fun `should save and find user by id`() {
        // Given
        val user =
            User(
                userId = UUID.randomUUID().toString(),
                tenantId = "test-tenant",
                username = "test-user",
                email = "test@example.com",
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
            )

        // When
        val savedUser = userPersistenceAdapter.saveUser(user)
        val foundUser = userPersistenceAdapter.findUserByIdAndTenantId(savedUser.userId, savedUser.tenantId)

        // Then
        assertNotNull(foundUser)
        assertEquals(savedUser.userId, foundUser?.userId)
        assertEquals("test-user", foundUser?.username)
    }
}

package com.axians.eaf.controlplane.test

import com.axians.eaf.controlplane.domain.model.tenant.Tenant
import com.axians.eaf.controlplane.domain.model.tenant.TenantSettings
import com.axians.eaf.controlplane.domain.model.user.UserStatus
import com.axians.eaf.controlplane.domain.port.RoleRepository
import com.axians.eaf.controlplane.domain.port.TenantRepository
import com.axians.eaf.controlplane.domain.port.UserRepository
import com.axians.eaf.controlplane.domain.service.CreateTenantResult
import com.axians.eaf.controlplane.domain.service.CreateUserResult
import com.axians.eaf.controlplane.domain.service.RoleService
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UserService
import com.axians.eaf.iam.client.CreateTenantResponse
import com.axians.eaf.iam.client.IamServiceClient
import com.axians.eaf.iam.client.TenantResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import java.util.UUID

/** Minimal test configuration to provide missing repository and client beans for tests. */
@TestConfiguration
@Profile("test")
class TestMockServiceConfiguration {
    @Bean @Primary
    fun mockRoleRepository(): RoleRepository = mockk(relaxed = true)

    @Bean @Primary
    fun mockUserRepository(): UserRepository = mockk(relaxed = true)

    @Bean
    @Primary
    fun mockIamServiceClient(): IamServiceClient {
        val mockClient = mockk<IamServiceClient>(relaxed = true)

        // Mock createTenant to return a valid response
        every { mockClient.createTenant(any()) } answers
            {
                val request = firstArg<com.axians.eaf.iam.client.CreateTenantRequest>()
                CreateTenantResponse(
                    tenantId = UUID.randomUUID().toString(),
                    tenantName = request.tenantName,
                    tenantAdminUserId = UUID.randomUUID().toString(),
                    tenantAdminEmail = request.initialAdminEmail,
                    invitationDetails =
                        "Test invitation sent to ${request.initialAdminEmail}",
                )
            }

        // Mock getTenant to return a valid response
        every { mockClient.getTenant(any()) } answers
            {
                val tenantId = firstArg<String>()
                TenantResponse(
                    tenantId = tenantId,
                    tenantName = "Test Tenant",
                    status = "ACTIVE",
                    createdAt = "2024-01-01T00:00:00Z",
                )
            }

        return mockClient
    }

    @Bean @Primary
    fun mockTenantRepository(): TenantRepository = mockk(relaxed = true)

    @Bean
    @Primary
    fun mockRoleService(
        roleRepository: RoleRepository,
        tenantRepository: TenantRepository,
    ): RoleService = RoleService(roleRepository, tenantRepository)

    @Bean
    @Primary
    fun mockTenantService(): TenantService {
        val mock = mockk<TenantService>(relaxed = true)
        coEvery { mock.createTenant(any(), any(), any()) } answers
            {
                val name = firstArg<String>()
                val adminEmail = secondArg<String>()
                val settings = thirdArg<TenantSettings>()
                val tenant = Tenant.create(name, settings)
                CreateTenantResult.success(
                    tenantId = tenant.id.value,
                    tenantName = tenant.name,
                    adminEmail = adminEmail,
                )
            }
        return mock
    }

    @Bean
    @Primary
    fun mockUserService(): UserService {
        val mock = mockk<UserService>(relaxed = true)
        coEvery { mock.createUser(any(), any(), any(), any(), any()) } answers
            {
                val email = firstArg<String>()
                val firstName = secondArg<String>()
                val lastName = thirdArg<String>()
                CreateUserResult.success(
                    userId =
                        java.util.UUID
                            .randomUUID()
                            .toString(),
                    email = email,
                    fullName = "$firstName $lastName",
                    status = UserStatus.ACTIVE,
                )
            }
        return mock
    }

    @TestConfiguration
    class ActuatorTestSecurityConfig {
        @Bean
        fun actuatorTestSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
            http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests { it.anyRequest().permitAll() }
                .csrf { it.disable() }
                .build()
    }
}

package com.axians.eaf.controlplane.test

import com.axians.eaf.controlplane.domain.port.RoleRepository
import com.axians.eaf.controlplane.domain.port.TenantRepository
import com.axians.eaf.controlplane.domain.port.UserRepository
import com.axians.eaf.controlplane.domain.service.RoleService
import com.axians.eaf.controlplane.domain.service.TenantService
import com.axians.eaf.controlplane.domain.service.UserService
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

/**
 * Test configuration for domain services. Creates domain service beans with proper dependency
 * injection for integration tests.
 */
@TestConfiguration
@Profile("test")
class TestDomainServiceConfiguration {
    @Bean
    fun tenantService(tenantRepository: TenantRepository): TenantService = TenantService(tenantRepository)

    @Bean fun userService(userRepository: UserRepository): UserService = UserService(userRepository)

    @Bean
    fun roleService(
        roleRepository: RoleRepository,
        tenantRepository: TenantRepository,
    ): RoleService = RoleService(roleRepository, tenantRepository)
}

package com.axians.eaf.iam.web

import com.axians.eaf.iam.application.port.inbound.CreateUserUseCase
import com.axians.eaf.iam.application.port.inbound.ListUsersInTenantUseCase
import com.axians.eaf.iam.application.port.inbound.UpdateUserStatusUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class UserControllerTestConfiguration {
    @Bean
    @Primary
    fun createUserUseCase(): CreateUserUseCase = mockk(relaxed = true)

    @Bean
    @Primary
    fun listUsersInTenantUseCase(): ListUsersInTenantUseCase = mockk(relaxed = true)

    @Bean
    @Primary
    fun updateUserStatusUseCase(): UpdateUserStatusUseCase = mockk(relaxed = true)

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper = ObjectMapper()
}

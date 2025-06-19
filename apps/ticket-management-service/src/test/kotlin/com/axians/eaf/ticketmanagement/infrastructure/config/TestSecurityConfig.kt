package com.axians.eaf.ticketmanagement.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Test-specific security configuration that doesn't depend on Vaadin.
 *
 * This configuration is used when Vaadin auto-configuration is disabled in tests,
 * providing a basic security setup that works without Vaadin dependencies.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
@ConditionalOnMissingBean(name = ["securityFilterChain"])
class TestSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .permitAll() // For tests, allow all requests
            }.build()
}

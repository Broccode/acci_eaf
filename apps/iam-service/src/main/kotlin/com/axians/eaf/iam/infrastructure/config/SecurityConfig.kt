package com.axians.eaf.iam.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

/**
 * Spring Security configuration for the IAM service.
 * Provides basic authentication with hardcoded users for MVP testing.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val superAdmin: UserDetails =
            User
                .builder()
                .username("superadmin")
                .password(passwordEncoder.encode("superadmin123"))
                .roles("SUPERADMIN")
                .build()

        val testUser: UserDetails =
            User
                .builder()
                .username("testuser")
                .password(passwordEncoder.encode("testuser123"))
                .roles("USER")
                .build()

        return InMemoryUserDetailsManager(superAdmin, testUser)
    }

    @Bean
    fun filterChain(
        http: HttpSecurity,
        customAccessDeniedHandler: AccessDeniedHandler,
        customAuthenticationEntryPoint: AuthenticationEntryPoint,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated()
            }.httpBasic { basic ->
                basic.authenticationEntryPoint(customAuthenticationEntryPoint)
            }.exceptionHandling { exceptions ->
                exceptions
                    .accessDeniedHandler(customAccessDeniedHandler)
                    .authenticationEntryPoint(customAuthenticationEntryPoint)
            }.csrf { csrf ->
                // For development/testing - consider proper CSRF protection in production
                csrf.disable()
            }.headers { headers ->
                headers.frameOptions().deny()
            }

        return http.build()
    }

    /**
     * Custom AccessDeniedHandler that returns JSON responses for 403 Forbidden errors
     */
    @Bean
    fun customAccessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler {
                request: HttpServletRequest,
                response: HttpServletResponse,
                accessDeniedException: AccessDeniedException,
            ->
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"

            val objectMapper = ObjectMapper()
            val errorResponse = mapOf("error" to "Access denied")
            response.writer.write(objectMapper.writeValueAsString(errorResponse))
        }

    /**
     * Custom AuthenticationEntryPoint that returns JSON responses for 401 Unauthorized errors
     */
    @Bean
    fun customAuthenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint {
                request: HttpServletRequest,
                response: HttpServletResponse,
                authException: AuthenticationException,
            ->
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"

            val objectMapper = ObjectMapper()
            val errorResponse = mapOf("error" to "Authentication required")
            response.writer.write(objectMapper.writeValueAsString(errorResponse))
        }
}

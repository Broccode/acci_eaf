package com.axians.eaf.iam.client

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Configuration properties for EAF IAM client.
 */
@ConfigurationProperties(prefix = "eaf.iam")
data class EafIamProperties(
    val serviceUrl: String = "http://localhost:8080",
    val jwt: JwtProperties = JwtProperties(),
    val security: SecurityProperties = SecurityProperties(),
) {
    data class JwtProperties(
        val issuerUri: String = "http://localhost:8080",
        val audience: String = "eaf-service",
    )

    data class SecurityProperties(
        val enabled: Boolean = true,
        val permitAllPaths: List<String> =
            listOf(
                "/actuator/health",
                "/actuator/info",
            ),
    )
}

/**
 * Spring Security auto-configuration for EAF services.
 * Configures JWT-based authentication using the IAM service.
 *
 * This configuration is automatically enabled when the eaf-iam-client library
 * is included as a dependency and eaf.iam.service-url is configured.
 * It will only activate if no existing SecurityFilterChain bean is present,
 * allowing services to provide their own security configuration when needed.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "eaf.iam", name = ["service-url"])
@ConditionalOnMissingBean(name = ["securityFilterChain"])
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(EafIamProperties::class)
class EafSecurityConfiguration(
    private val eafIamProperties: EafIamProperties,
) {
    @Bean
    @ConditionalOnMissingBean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(eafIamProperties)

    @Bean
    @ConditionalOnMissingBean(name = ["securityFilterChain"])
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Allow access to health check endpoints
                eafIamProperties.security.permitAllPaths.forEach { path ->
                    auth.requestMatchers(path).permitAll()
                }

                if (eafIamProperties.security.enabled) {
                    // All other requests require authentication
                    auth.anyRequest().authenticated()
                } else {
                    // Security disabled - allow all requests (for development/testing)
                    auth.anyRequest().permitAll()
                }
            }

        if (eafIamProperties.security.enabled) {
            http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
        }

        return http.build()
    }
}

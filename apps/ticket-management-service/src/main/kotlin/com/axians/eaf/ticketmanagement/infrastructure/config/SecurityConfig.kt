package com.axians.eaf.ticketmanagement.infrastructure.config

import com.vaadin.flow.spring.security.VaadinWebSecurity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

/**
 * Spring Security configuration for the Ticket Management Service UI.
 *
 * This class configures a stateless, JWT-based security model for the Hilla application,
 * following modern best practices for SPAs. It disables server-side sessions and relies
 * on a JWT passed via secure cookies.
 */
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@Configuration
@ConditionalOnProperty(name = ["vaadin.enabled"], havingValue = "true", matchIfMissing = true)
class SecurityConfig : VaadinWebSecurity() {
    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${app.security.auth.secret:}")
    private lateinit var authSecret: String

    /**
     * Configures HTTP security rules.
     *
     * - Disables session creation, making the application stateless.
     * - Applies Vaadin's default security configuration for Hilla.
     * - Configures stateless authentication using a JWT.
     * - Sets up the form-based login view.
     */
    override fun configure(http: HttpSecurity) {
        super.configure(http)

        // Disable creating and using sessions in Spring Security
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        http.logout { logout ->
            logout.logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler())
        }

        // Register your login view to the view access checker mechanism
        setLoginView(http, "/login")

        if (authSecret.isNotBlank()) {
            logger.info("Configuring stateless JWT authentication with issuer: ticket-management-service")
            // Enable stateless authentication
            setStatelessAuthentication(
                http,
                SecretKeySpec(Base64.getDecoder().decode(authSecret), JwsAlgorithms.HS256),
                "ticket-management-service", // Simplified issuer name
            )
        } else {
            logger.warn("JWT secret is not configured - stateless authentication will not work!")
        }
    }
}

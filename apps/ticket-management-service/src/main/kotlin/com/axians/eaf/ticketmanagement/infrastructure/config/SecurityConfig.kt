package com.axians.eaf.ticketmanagement.infrastructure.config

import com.vaadin.flow.spring.security.VaadinWebSecurity
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
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
@Configuration
class SecurityConfig : VaadinWebSecurity() {
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

        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        http.logout { logout ->
            logout.logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler())
        }

        setLoginView(http, "/login")

        if (authSecret.isNotBlank()) {
            setStatelessAuthentication(
                http,
                SecretKeySpec(Base64.getDecoder().decode(authSecret), JwsAlgorithms.HS256),
                "com.axians.eaf.ticketmanagement",
            )
        }
    }
}

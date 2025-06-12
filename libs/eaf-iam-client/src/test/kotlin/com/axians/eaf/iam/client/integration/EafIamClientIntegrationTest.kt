package com.axians.eaf.iam.client.integration

import com.axians.eaf.iam.client.EafIamProperties
import com.axians.eaf.iam.client.EafSecurityConfiguration
import com.axians.eaf.iam.client.JwtAuthenticationFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.security.web.SecurityFilterChain

class EafIamClientIntegrationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EafSecurityConfiguration::class.java))

    @Test
    fun `should auto-configure when service-url is provided`() {
        contextRunner
            .withPropertyValues("eaf.iam.service-url=http://iam-service:8080")
            .run { context ->
                assertThat(context).hasSingleBean(EafIamProperties::class.java)
                assertThat(context).hasSingleBean(JwtAuthenticationFilter::class.java)
                assertThat(context).hasSingleBean(SecurityFilterChain::class.java)

                val properties = context.getBean(EafIamProperties::class.java)
                assertThat(properties.serviceUrl).isEqualTo("http://iam-service:8080")
                assertThat(properties.jwt.issuerUri).isEqualTo("http://localhost:8080") // default value
                assertThat(properties.jwt.audience).isEqualTo("eaf-service") // default value
            }
    }

    @Test
    fun `should configure custom JWT properties`() {
        contextRunner
            .withPropertyValues(
                "eaf.iam.service-url=http://iam-service:8080",
                "eaf.iam.jwt.issuer-uri=http://custom-issuer:8080",
                "eaf.iam.jwt.audience=custom-audience",
            ).run { context ->
                val properties = context.getBean(EafIamProperties::class.java)
                assertThat(properties.serviceUrl).isEqualTo("http://iam-service:8080")
                assertThat(properties.jwt.issuerUri).isEqualTo("http://custom-issuer:8080")
                assertThat(properties.jwt.audience).isEqualTo("custom-audience")
            }
    }

    @Test
    fun `should configure security settings`() {
        contextRunner
            .withPropertyValues(
                "eaf.iam.service-url=http://iam-service:8080",
                "eaf.iam.security.enabled=false",
                "eaf.iam.security.permit-all-paths[0]=/custom/health",
                "eaf.iam.security.permit-all-paths[1]=/custom/info",
            ).run { context ->
                val properties = context.getBean(EafIamProperties::class.java)
                assertThat(properties.security.enabled).isFalse()
                assertThat(properties.security.permitAllPaths).containsExactly("/custom/health", "/custom/info")
            }
    }

    @Test
    fun `should not auto-configure when service-url is missing`() {
        contextRunner
            .run { context ->
                assertThat(context).doesNotHaveBean(EafIamProperties::class.java)
                assertThat(context).doesNotHaveBean(JwtAuthenticationFilter::class.java)
                assertThat(context).doesNotHaveBean(SecurityFilterChain::class.java)
            }
    }

    @Test
    fun `should use default values when only service-url is provided`() {
        contextRunner
            .withPropertyValues("eaf.iam.service-url=http://iam-service:8080")
            .run { context ->
                val properties = context.getBean(EafIamProperties::class.java)

                // Verify default values
                assertThat(properties.serviceUrl).isEqualTo("http://iam-service:8080")
                assertThat(properties.jwt.issuerUri).isEqualTo("http://localhost:8080")
                assertThat(properties.jwt.audience).isEqualTo("eaf-service")
                assertThat(properties.security.enabled).isTrue()
                assertThat(properties.security.permitAllPaths).containsExactly(
                    "/actuator/health",
                    "/actuator/info",
                )
            }
    }
}

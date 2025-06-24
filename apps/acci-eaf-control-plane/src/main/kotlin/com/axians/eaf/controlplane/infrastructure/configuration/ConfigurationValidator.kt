package com.axians.eaf.controlplane.infrastructure.configuration

import com.axians.eaf.controlplane.infrastructure.configuration.config.ControlPlaneProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ConfigurationValidator(
    private val properties: ControlPlaneProperties,
    @Value("\${spring.profiles.active}") private val activeProfile: String,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun validateConfiguration() {
        when (activeProfile) {
            "prod" -> validateProductionConfiguration()
            "staging" -> validateStagingConfiguration()
            "local" -> validateLocalConfiguration()
        }
    }

    private fun validateProductionConfiguration() {
        require(properties.security.requireSsl) { "SSL must be enabled in production" }
        require(properties.security.sessionTimeout <= Duration.ofHours(8)) {
            "Session timeout too long for production"
        }
        // For now, we don't have secrets so we can't check them.
        // requireNotNull(System.getenv("DB_PASSWORD")) { "DB_PASSWORD not set" }
    }

    private fun validateStagingConfiguration() {
        // No specific staging rules yet
    }

    private fun validateLocalConfiguration() {
        // No specific local rules yet
    }
}

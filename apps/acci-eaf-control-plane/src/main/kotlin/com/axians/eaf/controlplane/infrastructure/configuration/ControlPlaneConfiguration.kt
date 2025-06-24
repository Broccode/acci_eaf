package com.axians.eaf.controlplane.infrastructure.configuration

import com.axians.eaf.controlplane.infrastructure.configuration.config.ControlPlaneProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ControlPlaneProperties::class)
class ControlPlaneConfiguration(
    private val properties: ControlPlaneProperties,
) {
    // Beans that depend on these properties will be defined here later.
}

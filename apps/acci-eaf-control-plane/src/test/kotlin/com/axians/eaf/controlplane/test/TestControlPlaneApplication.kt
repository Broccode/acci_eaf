package com.axians.eaf.controlplane.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Test application class for Control Plane integration tests. Placed in separate package to avoid
 * "Found multiple @SpringBootConfiguration" errors. Follows EAF integration testing best practices.
 */
@SpringBootApplication(
    scanBasePackages =
        [
            "com.axians.eaf.controlplane.application",
            "com.axians.eaf.controlplane.domain",
            "com.axians.eaf.controlplane.infrastructure",
            "com.axians.eaf.iam.client",
        ],
    exclude =
        [
            org.springframework.boot.autoconfigure.security.servlet
                .SecurityAutoConfiguration::class,
        ],
)
@EnableJpaRepositories(
    basePackages = ["com.axians.eaf.controlplane.infrastructure.adapter.outbound.repository"],
)
class TestControlPlaneApplication

fun main(args: Array<String>) {
    // ... existing code ...
}

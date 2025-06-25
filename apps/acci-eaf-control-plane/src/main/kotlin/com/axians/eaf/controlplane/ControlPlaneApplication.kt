@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.axians.eaf.controlplane

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages =
        [
            "com.axians.eaf.controlplane.application",
            "com.axians.eaf.controlplane.domain",
            "com.axians.eaf.controlplane.infrastructure",
            "com.axians.eaf.iam.client", // Add IAM client package for Spring component
            // scanning
        ],
)
class ControlPlaneApplication

fun main(args: Array<String>) {
    runApplication<ControlPlaneApplication>(*args)
}

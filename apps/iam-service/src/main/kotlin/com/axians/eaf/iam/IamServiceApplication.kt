@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.axians.eaf.iam

import com.axians.eaf.iam.infrastructure.config.SystemInitializationProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.axians.eaf.iam"])
@EnableConfigurationProperties(SystemInitializationProperties::class)
class IamServiceApplication

fun main(args: Array<String>) {
    runApplication<IamServiceApplication>(args = args)
}

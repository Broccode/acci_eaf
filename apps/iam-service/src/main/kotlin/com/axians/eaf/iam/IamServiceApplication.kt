@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.axians.eaf.iam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(basePackages = ["com.axians.eaf.iam"])
class IamServiceApplication

fun main(args: Array<String>) {
    runApplication<IamServiceApplication>(*args)
}

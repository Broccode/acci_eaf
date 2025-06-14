package com.axians.eaf.testservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(basePackages = ["com.axians.eaf.testservice"])
class TestServiceServiceApplication

fun main(args: Array<String>) {
    runApplication<TestServiceServiceApplication>(*args)
}
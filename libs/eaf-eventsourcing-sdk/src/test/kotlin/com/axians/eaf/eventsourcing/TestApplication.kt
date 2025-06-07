package com.axians.eaf.eventsourcing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Test application for running integration tests.
 *
 * This minimal Spring Boot application is used by the test framework
 * to set up the Spring context for integration testing.
 */
@SpringBootApplication
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}

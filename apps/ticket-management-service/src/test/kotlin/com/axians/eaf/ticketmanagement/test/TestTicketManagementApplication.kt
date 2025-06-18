package com.axians.eaf.ticketmanagement.test

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * A dedicated test application class for Spring Boot integration tests.
 *
 * Using a separate test application class in a different package (`.test`) from the main
 * application (`IamServiceApplication`) prevents component scanning conflicts and ensures
 * that only the required beans for the test context are loaded. This is a best practice
 * for complex Spring Boot applications with multiple configurations.
 *
 * It explicitly scans the necessary packages for the ticket management service to build
 * the correct application context for tests.
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.axians.eaf.ticketmanagement.application",
        "com.axians.eaf.ticketmanagement.domain",
        "com.axians.eaf.ticketmanagement.infrastructure",
        "com.axians.eaf.ticketmanagement.web",
    ],
)
class TestTicketManagementApplication

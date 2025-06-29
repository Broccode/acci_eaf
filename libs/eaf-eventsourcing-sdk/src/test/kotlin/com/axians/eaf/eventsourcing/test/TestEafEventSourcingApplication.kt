package com.axians.eaf.eventsourcing.test

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Test application for EAF Event Sourcing SDK integration tests.
 *
 * This application is specifically configured for integration testing with refined component
 * scanning to avoid conflicts with main application classes.
 */
@SpringBootApplication(
    scanBasePackages =
        [
            "com.axians.eaf.eventsourcing.axon",
            "com.axians.eaf.eventsourcing.adapter",
            "com.axians.eaf.eventsourcing.port",
            "com.axians.eaf.core.tenancy",
        ],
)
class TestEafEventSourcingApplication

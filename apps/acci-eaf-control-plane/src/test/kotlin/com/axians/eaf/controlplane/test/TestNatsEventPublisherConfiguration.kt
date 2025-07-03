package com.axians.eaf.controlplane.test

import com.axians.eaf.eventing.NatsEventPublisher
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration to provide mock beans for integration tests. Resolves missing bean
 * dependencies that cause Spring context loading failures.
 */
@TestConfiguration
class TestNatsEventPublisherConfiguration {
    @Bean("mockNatsEventPublisher")
    @Primary
    fun mockNatsEventPublisher(): NatsEventPublisher = mockk(relaxed = true)
}

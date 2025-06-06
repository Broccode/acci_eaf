package com.axians.eaf.eventing.consumer

import io.mockk.every
import io.mockk.mockk
import io.nats.client.Message
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class SimpleCompilationTest {
    @Test
    fun `should compile basic classes`() {
        // Given & When
        val mockMessage =
            mockk<Message> {
                every { subject } returns "test.subject"
                every { headers } returns null
            }
        val messageContext = DefaultMessageContext(mockMessage, "test-tenant")
        val retryableException = RetryableEventException("test")
        val poisonPillException = PoisonPillEventException("test")

        // Then
        assertNotNull(messageContext)
        assertNotNull(retryableException)
        assertNotNull(poisonPillException)
    }
}

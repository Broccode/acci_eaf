@file:Suppress("TooGenericExceptionThrown")

package com.axians.eaf.eventing.consumer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.nats.client.Message
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultMessageContextTest {
    private lateinit var message: Message
    private lateinit var context: DefaultMessageContext

    @BeforeEach
    fun setUp() {
        message = mockk<Message>()
        every { message.subject } returns "TENANT_A.events.test"
        every { message.headers } returns null
        context = DefaultMessageContext(message, "TENANT_A")
    }

    @Test
    fun `should return correct properties`() {
        assertEquals("TENANT_A", context.tenantId)
        assertEquals("TENANT_A.events.test", context.subject)
        assertEquals(message, context.message)
    }

    @Test
    fun `should return null when no headers exist`() {
        every { message.headers } returns null

        context = DefaultMessageContext(message, "TENANT_A")

        assertNull(context.getHeader("test-header"))
    }

    @Test
    fun `should return empty map when no headers exist`() {
        every { message.headers } returns null

        context = DefaultMessageContext(message, "TENANT_A")

        assertEquals(emptyMap(), context.getHeaders())
    }

    @Test
    fun `should acknowledge message`() {
        every { message.ack() } returns Unit

        context.ack()

        verify { message.ack() }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should negatively acknowledge message`() {
        every { message.nak() } returns Unit

        context.nak()

        verify { message.nak() }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should terminate message`() {
        every { message.term() } returns Unit

        context.term()

        verify { message.term() }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should acknowledge message with delay`() {
        every { message.nakWithDelay(any<Duration>()) } returns Unit

        context.nak(Duration.ofSeconds(5))

        verify { message.nakWithDelay(Duration.ofSeconds(5)) }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should not be acknowledged initially`() {
        assertFalse(context.isAcknowledged())
    }

    @Test
    fun `should not acknowledge twice`() {
        every { message.ack() } returns Unit

        context.ack()
        context.ack() // Second call should be ignored

        verify(exactly = 1) { message.ack() }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should not nak after ack`() {
        every { message.ack() } returns Unit
        every { message.nak() } returns Unit

        context.ack()
        context.nak() // Should be ignored since already acknowledged

        verify(exactly = 1) { message.ack() }
        verify(exactly = 0) { message.nak() }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should not term after ack`() {
        every { message.ack() } returns Unit
        every { message.term() } returns Unit

        context.ack()
        context.term() // Should be ignored since already acknowledged

        verify(exactly = 1) { message.ack() }
        verify(exactly = 0) { message.term() }
        assertTrue(context.isAcknowledged())
    }

    @Test
    fun `should not nak with delay after ack`() {
        every { message.ack() } returns Unit
        every { message.nakWithDelay(any<Duration>()) } returns Unit

        context.ack()
        context.nak(Duration.ofSeconds(5)) // Should be ignored

        verify(exactly = 1) { message.ack() }
        verify(exactly = 0) { message.nakWithDelay(any<Duration>()) }
        assertTrue(context.isAcknowledged())
    }
}

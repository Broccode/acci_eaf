package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.api.AckPolicy
import io.nats.client.api.DeliverPolicy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Service
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NatsJetStreamListenerProcessorTest {
    private lateinit var connection: Connection
    private lateinit var jetStream: JetStream
    private lateinit var objectMapper: ObjectMapper
    private lateinit var properties: NatsEventingProperties
    private lateinit var processor: NatsJetStreamListenerProcessor

    @BeforeEach
    fun setUp() {
        connection = mockk()
        jetStream = mockk()
        objectMapper = mockk()
        properties = mockk()

        every { connection.jetStream() } returns jetStream
        every { properties.defaultTenantId } returns "TENANT_A"

        processor = NatsJetStreamListenerProcessor(connection, jetStream, objectMapper, properties)
    }

    @Test
    fun `should create processor successfully`() {
        assertNotNull(processor)
    }

    @Test
    fun `should validate listener definition data class`() {
        // given
        val bean = TestEventHandler()
        val method = TestEventHandler::class.java.getMethod("handleEvent", String::class.java)

        // when
        val listenerDef =
            NatsJetStreamListenerProcessor.ListenerDefinition(
                bean = bean,
                method = method,
                beanName = "testEventHandler",
                subject = "test.events",
                durableName = "test-consumer",
                deliverPolicy = DeliverPolicy.All,
                ackPolicy = AckPolicy.Explicit,
                maxDeliver = 3,
                ackWait = 30000L,
                maxAckPending = 100,
                eventType = String::class.java,
                autoAck = true,
                configBean = "",
            )

        // then
        assertEquals(bean, listenerDef.bean)
        assertEquals(method, listenerDef.method)
        assertEquals("testEventHandler", listenerDef.beanName)
        assertEquals("test.events", listenerDef.subject)
        assertEquals("test-consumer", listenerDef.durableName)
        assertEquals(DeliverPolicy.All, listenerDef.deliverPolicy)
        assertEquals(AckPolicy.Explicit, listenerDef.ackPolicy)
        assertEquals(3, listenerDef.maxDeliver)
        assertEquals(30000L, listenerDef.ackWait)
        assertEquals(100, listenerDef.maxAckPending)
        assertEquals(String::class.java, listenerDef.eventType)
        assertEquals(true, listenerDef.autoAck)
        assertEquals("", listenerDef.configBean)
    }

    @Service
    class TestEventHandler {
        fun handleEvent(event: String) {
            // Test handler implementation
        }
    }
}

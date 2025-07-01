@file:Suppress("TooGenericExceptionThrown")

package com.axians.eaf.eventing.consumer

import com.axians.eaf.eventing.config.NatsEventingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.nats.client.JetStream
import io.nats.client.JetStreamSubscription
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.AckPolicy
import io.nats.client.api.DeliverPolicy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Service
import kotlin.test.assertNotNull

class NatsConsumerManagerTest {
    private lateinit var jetStream: JetStream
    private lateinit var objectMapper: ObjectMapper
    private lateinit var properties: NatsEventingProperties
    private lateinit var subscription: JetStreamSubscription
    private lateinit var consumerManager: NatsConsumerManager

    @BeforeEach
    fun setUp() {
        jetStream = mockk()
        objectMapper = jacksonObjectMapper()
        properties = mockk()
        subscription = mockk()

        every { properties.defaultTenantId } returns "TENANT_A"
    }

    @Test
    fun `should create consumer manager successfully`() {
        // given
        val listenerDef = createTestListenerDefinition()

        // when
        consumerManager =
            NatsConsumerManager(
                jetStream = jetStream,
                objectMapper = objectMapper,
                properties = properties,
                listenerDefinition = listenerDef,
            )

        // then
        assertNotNull(consumerManager)
    }

    @Test
    fun `should start consumer with proper subscription options`() {
        // given
        val listenerDef = createTestListenerDefinition()
        val subscribeOptionsSlot = slot<PushSubscribeOptions>()

        every { jetStream.subscribe(any<String>(), capture(subscribeOptionsSlot)) } returns
            subscription

        consumerManager =
            NatsConsumerManager(
                jetStream = jetStream,
                objectMapper = objectMapper,
                properties = properties,
                listenerDefinition = listenerDef,
            )

        // when
        consumerManager.start()

        // then
        verify { jetStream.subscribe("TENANT_A.test.events", any<PushSubscribeOptions>()) }
    }

    @Test
    fun `should stop consumer gracefully`() {
        // given
        val listenerDef = createTestListenerDefinition()
        every { jetStream.subscribe(any<String>(), any<PushSubscribeOptions>()) } returns
            subscription
        every { subscription.unsubscribe() } returns Unit

        consumerManager =
            NatsConsumerManager(
                jetStream = jetStream,
                objectMapper = objectMapper,
                properties = properties,
                listenerDefinition = listenerDef,
            )

        // when
        consumerManager.start()
        consumerManager.stop()

        // then
        verify { subscription.unsubscribe() }
    }

    private fun createTestListenerDefinition(): NatsJetStreamListenerProcessor.ListenerDefinition {
        val bean = TestEventHandler()
        val method = TestEventHandler::class.java.getMethod("handleEvent", String::class.java)

        return NatsJetStreamListenerProcessor.ListenerDefinition(
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
    }

    @Service
    class TestEventHandler {
        @NatsJetStreamListener("test.events.>")
        fun onTestEvent(
            @Suppress("UNUSED_PARAMETER") event: TestEvent,
        ) {
            // Test method - no implementation needed
        }

        fun handleEvent(
            @Suppress("UNUSED_PARAMETER") event: String,
        ) {
            // Test handler implementation
        }
    }

    /** Simple test event class for testing purposes. */
    data class TestEvent(
        val id: String,
        val name: String,
    )
}

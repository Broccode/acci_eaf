@file:Suppress("TooGenericExceptionThrown")

package com.axians.eaf.eventing

import com.axians.eaf.core.security.CorrelationIdManager
import com.axians.eaf.core.security.DefaultEafSecurityContextHolder
import com.axians.eaf.core.security.EafContextElement
import com.axians.eaf.core.security.HasTenantId
import com.axians.eaf.core.security.HasUserId
import com.axians.eaf.eventing.consumer.ContextAwareMessageProcessor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.nats.client.Message
import io.nats.client.impl.Headers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import kotlin.test.assertEquals

/**
 * Integration test for end-to-end context propagation through NATS messages.
 *
 * This test verifies that:
 * 1. Security context and correlation ID are properly propagated from publisher to consumer
 * 2. Context is correctly established in message handlers
 * 3. Context works across coroutine boundaries
 *
 * Requires NATS to be running via Docker Compose or enables when system property is set.
 */
class ContextPropagationIntegrationTest {
    private lateinit var publisher: ContextAwareNatsEventPublisher
    private lateinit var processor: ContextAwareMessageProcessor
    private lateinit var delegate: DefaultNatsEventPublisher
    private val securityContextHolder = DefaultEafSecurityContextHolder()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
        CorrelationIdManager.clearCorrelationId()

        delegate = mockk()
        publisher = ContextAwareNatsEventPublisher(delegate, securityContextHolder)
        processor = ContextAwareMessageProcessor()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        CorrelationIdManager.clearCorrelationId()
    }

    @Test
    fun `should propagate full context through NATS message flow`() =
        runBlocking {
            val originalTenantId = "integration-tenant-123"
            val originalUserId = "integration-user-456"
            val originalCorrelationId = "correlation-abc-123"
            val roles = listOf("USER", "INTEGRATION_TEST")

            setupSecurityContext(originalTenantId, originalUserId, roles)
            CorrelationIdManager.setCorrelationId(originalCorrelationId)

            val metadataSlot = slot<Map<String, Any>>()
            val mockMessage: Message = mockk()

            coEvery { delegate.publish(any(), any(), any(), capture(metadataSlot)) } returns mockk()

            withContext(EafContextElement()) {
                publisher.publish("test.subject", mapOf("eventType" to "IntegrationTest"))
            }

            val capturedHeaders = Headers()
            metadataSlot.captured.forEach { (key, value) -> capturedHeaders.add(key, value.toString()) }

            every { mockMessage.headers } returns capturedHeaders
            every { mockMessage.data } returns
                jacksonObjectMapper().writeValueAsBytes(mapOf("test" to "data"))
            every { mockMessage.subject } returns "test.subject"

            var capturedTenantId: String? = null
            var capturedUserId: String? = null
            var capturedCorrelationId: String? = null

            processor.processWithContext(mockMessage) {
                val auth = SecurityContextHolder.getContext().authentication
                val principal = auth?.principal
                capturedTenantId = (principal as? HasTenantId)?.getTenantId()
                capturedUserId = (principal as? HasUserId)?.getUserId()
                capturedCorrelationId = CorrelationIdManager.getCurrentCorrelationId()
            }

            assertEquals(originalTenantId, capturedTenantId)
            assertEquals(originalUserId, capturedUserId)
            assertEquals(originalCorrelationId, capturedCorrelationId)
        }

    private fun setupSecurityContext(
        tenantId: String,
        userId: String,
        roles: List<String>,
    ) {
        val principal =
            object : HasTenantId, HasUserId {
                override fun getTenantId(): String = tenantId

                override fun getUserId(): String = userId

                override fun toString(): String = "IntegrationTestPrincipal(tenant=$tenantId, user=$userId)"
            }

        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        val authentication = TestingAuthenticationToken(principal, "credentials", authorities)
        authentication.isAuthenticated = true

        val securityContext = SecurityContextImpl(authentication)
        SecurityContextHolder.setContext(securityContext)
    }
}

package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.axonframework.eventhandling.EventMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MetaData
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantAwareEventInterceptorTest {
    private lateinit var interceptor: TenantAwareEventInterceptor
    private lateinit var unitOfWork: UnitOfWork<out EventMessage<*>>
    private lateinit var interceptorChain: InterceptorChain
    private lateinit var eventMessage: EventMessage<*>

    @BeforeEach
    fun setUp() {
        interceptor = TenantAwareEventInterceptor()
        unitOfWork = mockk()
        interceptorChain = mockk()
        eventMessage = mockk()

        // Clear any existing tenant context
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clean up tenant context after each test
        TenantContextHolder.clear()
    }

    @Test
    fun `should set tenant context when tenant ID is present in event metadata`() {
        // Given
        val tenantId = "test-tenant-123"
        val metaData = MetaData.with("tenant_id", tenantId)
        val expectedResult = "event-result"

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }

        // Verify tenant context was cleaned up
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle event without tenant context when metadata is missing`() {
        // Given
        val metaData = MetaData.emptyInstance()
        val expectedResult = "system-event-result"

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { eventMessage.payloadType } returns TestEvent::class.java
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle event replay scenarios`() {
        // Given
        val tenantId = "replay-tenant-456"
        val metaData = MetaData.with("tenant_id", tenantId).and("_isReplay", true)
        val expectedResult = "replay-result"

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }

        // Verify tenant context was cleaned up
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should propagate exceptions while cleaning up tenant context`() {
        // Given
        val tenantId = "test-tenant-789"
        val metaData = MetaData.with("tenant_id", tenantId)
        val expectedException = RuntimeException("Event processing failed")

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { interceptorChain.proceed() } throws expectedException

        // When & Then
        val thrownException =
            assertThrows(RuntimeException::class.java) {
                interceptor.handle(unitOfWork, interceptorChain)
            }

        assertEquals(expectedException, thrownException)
        verify { interceptorChain.proceed() }

        // Verify tenant context was cleaned up even after exception
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle null tenant ID gracefully`() {
        // Given
        val metaData = MetaData.with("tenant_id", null)
        val expectedResult = "result"

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { eventMessage.payloadType } returns TestEvent::class.java
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle system events without tenant context`() {
        // Given
        val metaData = MetaData.emptyInstance()
        val expectedResult = "system-result"

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { eventMessage.payloadType } returns SystemEvent::class.java
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle concurrent event processing`() {
        // Given
        val tenantId1 = "tenant-1"
        val tenantId2 = "tenant-2"
        val metaData1 = MetaData.with("tenant_id", tenantId1)
        val metaData2 = MetaData.with("tenant_id", tenantId2)

        val eventMessage1 = mockk<EventMessage<*>>()
        val eventMessage2 = mockk<EventMessage<*>>()
        val unitOfWork1 = mockk<UnitOfWork<out EventMessage<*>>>()
        val unitOfWork2 = mockk<UnitOfWork<out EventMessage<*>>>()

        every { unitOfWork1.getMessage() } returns eventMessage1
        every { eventMessage1.metaData } returns metaData1
        every { unitOfWork2.getMessage() } returns eventMessage2
        every { eventMessage2.metaData } returns metaData2
        every { interceptorChain.proceed() } returns "result"

        // When
        interceptor.handle(unitOfWork1, interceptorChain)
        interceptor.handle(unitOfWork2, interceptorChain)

        // Then
        verify(exactly = 2) { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    // Test event classes
    data class TestEvent(
        val id: String,
        val data: String,
    )

    data class SystemEvent(
        val operation: String,
    )
}

package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.axonframework.commandhandling.CommandMessage
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
class TenantAwareCommandInterceptorTest {
    private lateinit var interceptor: TenantAwareCommandInterceptor
    private lateinit var unitOfWork: UnitOfWork<out CommandMessage<*>>
    private lateinit var interceptorChain: InterceptorChain
    private lateinit var commandMessage: CommandMessage<*>

    @BeforeEach
    fun setUp() {
        interceptor = TenantAwareCommandInterceptor()
        unitOfWork = mockk()
        interceptorChain = mockk()
        commandMessage = mockk()

        // Clear any existing tenant context
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clean up tenant context after each test
        TenantContextHolder.clear()
    }

    @Test
    fun `should set tenant context when tenant ID is present in metadata`() {
        // Given
        val tenantId = "test-tenant-123"
        val metaData = MetaData.with("tenant_id", tenantId)
        val expectedResult = "command-result"

        every { unitOfWork.getMessage() } returns commandMessage
        every { commandMessage.metaData } returns metaData
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
    fun `should handle command without tenant context when metadata is missing`() {
        // Given
        val metaData = MetaData.emptyInstance()
        val expectedResult = "system-command-result"

        every { unitOfWork.getMessage() } returns commandMessage
        every { commandMessage.metaData } returns metaData
        every { commandMessage.payloadType } returns TestCommand::class.java
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should propagate exceptions while cleaning up tenant context`() {
        // Given
        val tenantId = "test-tenant-456"
        val metaData = MetaData.with("tenant_id", tenantId)
        val expectedException = RuntimeException("Command processing failed")

        every { unitOfWork.getMessage() } returns commandMessage
        every { commandMessage.metaData } returns metaData
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

        every { unitOfWork.getMessage() } returns commandMessage
        every { commandMessage.metaData } returns metaData
        every { commandMessage.payloadType } returns TestCommand::class.java
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle multiple tenant context operations correctly`() {
        // Given
        val tenantId1 = "tenant-1"
        val tenantId2 = "tenant-2"
        val metaData1 = MetaData.with("tenant_id", tenantId1)
        val metaData2 = MetaData.with("tenant_id", tenantId2)

        val commandMessage1 = mockk<CommandMessage<*>>()
        val commandMessage2 = mockk<CommandMessage<*>>()
        val unitOfWork1 = mockk<UnitOfWork<out CommandMessage<*>>>()
        val unitOfWork2 = mockk<UnitOfWork<out CommandMessage<*>>>()

        every { unitOfWork1.getMessage() } returns commandMessage1
        every { commandMessage1.metaData } returns metaData1
        every { unitOfWork2.getMessage() } returns commandMessage2
        every { commandMessage2.metaData } returns metaData2
        every { interceptorChain.proceed() } returns "result"

        // When
        interceptor.handle(unitOfWork1, interceptorChain)
        interceptor.handle(unitOfWork2, interceptorChain)

        // Then
        verify(exactly = 2) { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    @Test
    fun `should handle system commands without tenant context gracefully`() {
        // Given
        val metaData = MetaData.emptyInstance()
        val expectedResult = "system-result"

        every { unitOfWork.getMessage() } returns commandMessage
        every { commandMessage.metaData } returns metaData
        every { commandMessage.payloadType } returns SystemCommand::class.java
        every { interceptorChain.proceed() } returns expectedResult

        // When
        val result = interceptor.handle(unitOfWork, interceptorChain)

        // Then
        assertEquals(expectedResult, result)
        verify { interceptorChain.proceed() }
        assertNull(TenantContextHolder.getCurrentTenantId())
    }

    // Test command classes
    data class TestCommand(
        val id: String,
        val data: String,
    )

    data class SystemCommand(
        val operation: String,
    )
}

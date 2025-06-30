package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.tenancy.TenantContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SecurityAwareTenantExecutorTest {
    private val bridge = mockk<SecurityTenantContextBridge>()
    private val mockExecutor = mockk<Executor>()
    private lateinit var executor: SecurityAwareTenantExecutor

    @BeforeEach
    fun setUp() {
        TenantContextHolder.clear()
        executor = SecurityAwareTenantExecutor(bridge, mockExecutor)
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Test
    fun `execute should wrap task with tenant context propagation`() {
        // Arrange
        val testTenantId = "test-tenant"
        every { bridge.getEffectiveTenantId() } returns testTenantId
        every { mockExecutor.execute(any()) } answers
            {
                // Execute the wrapped task immediately for testing
                val wrappedTask = firstArg<Runnable>()
                wrappedTask.run()
            }

        var executedTenantId: String? = null
        val task = Runnable { executedTenantId = TenantContextHolder.getCurrentTenantId() }

        // Act
        executor.execute(task)

        // Assert
        assertEquals(testTenantId, executedTenantId)
        verify { mockExecutor.execute(any()) }
    }

    @Test
    fun `execute should handle null tenant context gracefully`() {
        // Arrange
        every { bridge.getEffectiveTenantId() } returns null
        every { mockExecutor.execute(any()) } answers
            {
                val wrappedTask = firstArg<Runnable>()
                wrappedTask.run()
            }

        var executedTenantId: String? = "initial"
        val task = Runnable { executedTenantId = TenantContextHolder.getCurrentTenantId() }

        // Act
        executor.execute(task)

        // Assert
        assertNull(executedTenantId)
        verify { mockExecutor.execute(any()) }
    }

    @Test
    fun `submit should propagate tenant context to callable`() {
        // Arrange
        val testTenantId = "test-tenant"
        val defaultExecutor = SecurityAwareTenantExecutor(bridge)
        every { bridge.getEffectiveTenantId() } returns testTenantId

        var executedTenantId: String? = null
        val callable = {
            executedTenantId = TenantContextHolder.getCurrentTenantId()
            "result"
        }

        // Act
        val future = defaultExecutor.submit(callable)
        val result = future.get(1, TimeUnit.SECONDS)

        // Assert
        assertEquals("result", result)
        assertEquals(testTenantId, executedTenantId)
    }

    @Test
    fun `submit should throw exception when delegate is not ThreadPoolTaskExecutor`() {
        // Arrange
        every { bridge.getEffectiveTenantId() } returns "test-tenant"
        val callable = { "result" }

        // Act & Assert
        assertThrows<UnsupportedOperationException> { executor.submit(callable) }
    }

    @Test
    fun `execute should restore context after task completion`() {
        // Arrange
        val originalTenantId = "original-tenant"
        val asyncTenantId = "async-tenant"

        TenantContextHolder.setCurrentTenantId(originalTenantId)
        every { bridge.getEffectiveTenantId() } returns asyncTenantId

        val latch = CountDownLatch(1)
        val realExecutor =
            ThreadPoolTaskExecutor().apply {
                corePoolSize = 1
                initialize()
            }
        val realSecurityAwareExecutor = SecurityAwareTenantExecutor(bridge, realExecutor)

        var taskTenantId: String? = null
        val task =
            Runnable {
                taskTenantId = TenantContextHolder.getCurrentTenantId()
                latch.countDown()
            }

        // Act
        realSecurityAwareExecutor.execute(task)
        latch.await(1, TimeUnit.SECONDS)

        // Assert
        assertEquals(asyncTenantId, taskTenantId)
        assertEquals(originalTenantId, TenantContextHolder.getCurrentTenantId())

        // Cleanup
        realExecutor.shutdown()
    }

    @Test
    fun `wrapWithTenantContext should handle task exceptions gracefully`() {
        // Arrange
        val testTenantId = "test-tenant"
        every { bridge.getEffectiveTenantId() } returns testTenantId
        every { mockExecutor.execute(any()) } answers
            {
                val wrappedTask = firstArg<Runnable>()
                wrappedTask.run()
            }

        val task = Runnable { throw RuntimeException("Task failed") }

        // Act & Assert
        assertThrows<RuntimeException> { executor.execute(task) }
        verify { mockExecutor.execute(any()) }
    }

    @Test
    fun `default executor should be created with proper configuration`() {
        // Arrange & Act
        val defaultExecutor = SecurityAwareTenantExecutor(bridge)

        // Assert
        assertNotNull(defaultExecutor)
        // The default executor should work (difficult to test internal state)
    }

    @Test
    fun `execute should work with multiple concurrent tasks`() {
        // Arrange
        val tenant1 = "tenant-1"
        val tenant2 = "tenant-2"
        val realExecutor =
            ThreadPoolTaskExecutor().apply {
                corePoolSize = 2
                maxPoolSize = 2
                initialize()
            }
        val realSecurityAwareExecutor = SecurityAwareTenantExecutor(bridge, realExecutor)

        val latch = CountDownLatch(2)
        val results = mutableMapOf<String, String?>()

        // Act
        every { bridge.getEffectiveTenantId() } returns tenant1
        realSecurityAwareExecutor.execute {
            results["task1"] = TenantContextHolder.getCurrentTenantId()
            Thread.sleep(50) // Simulate some work
            latch.countDown()
        }

        every { bridge.getEffectiveTenantId() } returns tenant2
        realSecurityAwareExecutor.execute {
            results["task2"] = TenantContextHolder.getCurrentTenantId()
            Thread.sleep(50) // Simulate some work
            latch.countDown()
        }

        latch.await(2, TimeUnit.SECONDS)

        // Assert
        assertEquals(tenant1, results["task1"])
        assertEquals(tenant2, results["task2"])

        // Cleanup
        realExecutor.shutdown()
    }
}

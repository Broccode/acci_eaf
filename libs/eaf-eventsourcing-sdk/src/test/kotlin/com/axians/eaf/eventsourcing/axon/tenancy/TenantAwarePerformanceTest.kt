package com.axians.eaf.eventsourcing.axon.tenancy

import com.axians.eaf.core.tenancy.TenantContextHolder
import io.mockk.every
import io.mockk.mockk
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MetaData
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.system.measureNanoTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantAwarePerformanceTest {
    private lateinit var commandInterceptor: TenantAwareCommandInterceptor
    private lateinit var eventInterceptor: TenantAwareEventInterceptor
    private val performanceThresholdNanos = 1_000_000L // 1ms in nanoseconds

    @BeforeEach
    fun setUp() {
        commandInterceptor = TenantAwareCommandInterceptor()
        eventInterceptor = TenantAwareEventInterceptor()
        TenantContextHolder.clear()
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    @Test
    fun `command interceptor should add less than 1ms overhead`() {
        // Given
        val tenantId = "performance-tenant"
        val iterations = 1000
        val metaData = MetaData.with("tenant_id", tenantId)

        val unitOfWork = mockk<UnitOfWork<out CommandMessage<*>>>()
        val commandMessage = mockk<CommandMessage<*>>()
        val interceptorChain = mockk<InterceptorChain>()

        every { unitOfWork.getMessage() } returns commandMessage
        every { commandMessage.metaData } returns metaData
        every { interceptorChain.proceed() } returns "result"

        // Warmup
        repeat(100) { commandInterceptor.handle(unitOfWork, interceptorChain) }

        // When - Measure average performance over multiple iterations
        val totalTime =
            measureNanoTime {
                repeat(iterations) { commandInterceptor.handle(unitOfWork, interceptorChain) }
            }

        val averageTimePerOperation = totalTime / iterations

        // Then
        assertTrue(
            averageTimePerOperation < performanceThresholdNanos,
            "Command interceptor overhead: ${averageTimePerOperation / 1_000}μs exceeds 1ms threshold",
        )

        println(
            "Command interceptor average overhead: ${averageTimePerOperation / 1_000}μs per operation",
        )
    }

    @Test
    fun `event interceptor should add less than 1ms overhead`() {
        // Given
        val tenantId = "performance-tenant"
        val iterations = 1000
        val metaData = MetaData.with("tenant_id", tenantId)

        val unitOfWork = mockk<UnitOfWork<out EventMessage<*>>>()
        val eventMessage = mockk<EventMessage<*>>()
        val interceptorChain = mockk<InterceptorChain>()

        every { unitOfWork.getMessage() } returns eventMessage
        every { eventMessage.metaData } returns metaData
        every { interceptorChain.proceed() } returns "result"

        // Warmup
        repeat(100) { eventInterceptor.handle(unitOfWork, interceptorChain) }

        // When - Measure average performance over multiple iterations
        val totalTime =
            measureNanoTime {
                repeat(iterations) { eventInterceptor.handle(unitOfWork, interceptorChain) }
            }

        val averageTimePerOperation = totalTime / iterations

        // Then
        assertTrue(
            averageTimePerOperation < performanceThresholdNanos,
            "Event interceptor overhead: ${averageTimePerOperation / 1_000}μs exceeds 1ms threshold",
        )

        println(
            "Event interceptor average overhead: ${averageTimePerOperation / 1_000}μs per operation",
        )
    }

    @Test
    fun `metadata creation should be performant`() {
        // Given
        val commandMetadata = TenantAwareCommandMetadata()
        val iterations = 1000

        // Set up tenant context
        TenantContextHolder.executeInTenantContext("perf-tenant") {
            // Warmup
            repeat(100) { commandMetadata.createTenantAwareMetadata() }

            // When - Measure metadata creation performance
            val totalTime =
                measureNanoTime {
                    repeat(iterations) { commandMetadata.createTenantAwareMetadata() }
                }

            val averageTimePerOperation = totalTime / iterations

            // Then
            assertTrue(
                averageTimePerOperation < performanceThresholdNanos,
                "Metadata creation overhead: ${averageTimePerOperation / 1_000}μs exceeds 1ms threshold",
            )

            println(
                "Metadata creation average overhead: ${averageTimePerOperation / 1_000}μs per operation",
            )
        }
    }

    @Test
    fun `tenant context operations should be performant`() {
        // Given
        val workingMetadata = WorkingTenantMetadata()
        val iterations = 1000

        // Warmup
        repeat(100) {
            TenantContextHolder.executeInTenantContext("warmup-tenant") {
                workingMetadata.createTenantMetadata(null)
                workingMetadata.validateTenantContext("warmup")
            }
        }

        // When - Measure tenant context operations
        val totalTime =
            measureNanoTime {
                repeat(iterations) {
                    TenantContextHolder.executeInTenantContext("perf-tenant-$it") {
                        workingMetadata.createTenantMetadata(null)
                        workingMetadata.validateTenantContext("test-operation")
                    }
                }
            }

        val averageTimePerOperation = totalTime / iterations

        // Then
        assertTrue(
            averageTimePerOperation < performanceThresholdNanos,
            "Tenant context operations overhead: ${averageTimePerOperation / 1_000}μs exceeds 1ms threshold",
        )

        println(
            "Tenant context operations average overhead: ${averageTimePerOperation / 1_000}μs per operation",
        )
    }

    @Test
    fun `concurrent tenant processing should maintain performance`() {
        // Given
        val commandMetadata = TenantAwareCommandMetadata()
        val concurrentOperations = 100
        val iterations = 10

        // When - Measure concurrent performance
        val totalTime =
            measureNanoTime {
                repeat(iterations) {
                    val threads =
                        (1..concurrentOperations).map { tenantIndex ->
                            Thread {
                                TenantContextHolder.executeInTenantContext(
                                    "concurrent-tenant-$tenantIndex",
                                ) { commandMetadata.createTenantAwareMetadata() }
                            }
                        }

                    threads.forEach { it.start() }
                    threads.forEach { it.join() }
                }
            }

        val averageTimePerIteration = totalTime / iterations
        val averageTimePerOperation = averageTimePerIteration / concurrentOperations

        // Then
        assertTrue(
            averageTimePerOperation <
                performanceThresholdNanos *
                5, // Allow 5ms for concurrent overhead (more realistic for CI
            // environments)
            "Concurrent tenant processing overhead: ${averageTimePerOperation / 1_000}μs exceeds 5ms threshold",
        )

        println(
            "Concurrent tenant processing average overhead: ${averageTimePerOperation / 1_000}μs per operation",
        )
    }
}

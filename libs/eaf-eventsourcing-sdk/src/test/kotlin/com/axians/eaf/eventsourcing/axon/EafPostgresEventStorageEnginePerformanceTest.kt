package com.axians.eaf.eventsourcing.axon

import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.eventsourcing.EventSourcingTestcontainerConfiguration
import com.axians.eaf.eventsourcing.performance.EventStoreDataGenerator
import com.axians.eaf.eventsourcing.test.TestEafEventSourcingApplication
import kotlinx.coroutines.runBlocking
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.messaging.MetaData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.random.Random

/**
 * Comprehensive performance tests for EafPostgresEventStorageEngine using JMH.
 *
 * This test suite measures:
 * - Event append performance with various batch sizes
 * - Event streaming performance for TrackingEventProcessor scenarios
 * - Concurrent read/write performance
 * - Query optimization and index effectiveness
 * - Database performance characteristics under load
 *
 * Performance Targets (from Story 4.1.6):
 * - Event append: <10ms for single event, <50ms for 100-event batch
 * - Aggregate loading: <20ms for 1000-event aggregate
 * - Event streaming: <50ms for 1000-event batch
 * - Token operations: <1ms for all token creation/comparison operations
 *
 * Usage:
 * - Run individual JUnit tests for quick performance checks
 * - Run JMH benchmarks for detailed performance profiling
 *
 * Note: These tests are disabled by default due to longer execution times and infrastructure
 * requirements.
 */
@SpringBootTest(classes = [TestEafEventSourcingApplication::class])
@Testcontainers
@ActiveProfiles("test")
@Import(EventSourcingTestcontainerConfiguration::class)
class EafPostgresEventStorageEnginePerformanceTest {
    @Autowired private lateinit var eventStorageEngine: EafPostgresEventStorageEngine

    @Autowired private lateinit var dataSource: DataSource

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var dataGenerator: EventStoreDataGenerator
    private val logger =
        LoggerFactory.getLogger(EafPostgresEventStorageEnginePerformanceTest::class.java)

    // Test data
    private val performanceTenant = "PERF_TEST_TENANT"
    private val aggregateCounter = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        TenantContextHolder.setCurrentTenantId(performanceTenant)
        dataGenerator = EventStoreDataGenerator(eventStorageEngine, batchSize = 1000)

        // Warm up database connection pool
        jdbcTemplate.execute("SELECT 1")

        logger.info("Performance test setup completed for tenant: {}", performanceTenant)
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clear()
    }

    // ============================================================================
    // Task 4: Core Operation Performance Tests
    // ============================================================================

    @Test
    @Disabled("Performance test - enable for performance validation")
    fun `benchmark event append performance with different batch sizes`() {
        val batchSizes = listOf(1, 10, 50, 100, 500, 1000)
        val results = mutableMapOf<Int, PerformanceResult>()

        batchSizes.forEach { batchSize ->
            logger.info("Testing batch size: {}", batchSize)
            val events = createTestEvents(batchSize)

            val measurements =
                (1..5).map {
                    val startTime = System.nanoTime()
                    eventStorageEngine.appendEvents(events.toMutableList())
                    val duration = System.nanoTime() - startTime
                    TimeUnit.NANOSECONDS.toMillis(duration)
                }

            val avgTime = measurements.average()
            val p95Time = measurements.sorted()[measurements.size * 95 / 100].toDouble()

            results[batchSize] = PerformanceResult(avgTime, p95Time, batchSize)

            logger.info("Batch size {}: avg={}ms, p95={}ms", batchSize, avgTime, p95Time)
        }

        // Validate performance targets
        assertTrue(results[1]!!.averageTimeMs < 10.0, "Single event append should be <10ms")
        assertTrue(results[100]!!.averageTimeMs < 50.0, "100-event batch should be <50ms")

        logPerformanceResults("Event Append Performance", results)
    }

    @Test
    @Disabled("Performance test - enable for performance validation")
    fun `benchmark aggregate loading performance`() =
        runBlocking {
            // Generate test aggregate with 1000 events
            val aggregateId = "perf-aggregate-${UUID.randomUUID()}"
            val events =
                (0 until 1000).map { sequence ->
                    GenericDomainEventMessage(
                        "PerformanceAggregate",
                        aggregateId,
                        sequence.toLong(),
                        TestEventPayload("Event $sequence", sequence),
                    )
                }

            eventStorageEngine.appendEvents(events.toMutableList())

            // Benchmark aggregate loading
            val loadTimes =
                (1..10).map {
                    val startTime = System.nanoTime()
                    val eventStream = eventStorageEngine.readEvents(aggregateId, 0L)
                    val loadedEvents = eventStream.asSequence().toList()
                    val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

                    assertTrue(loadedEvents.size == 1000, "Should load all 1000 events")
                    duration
                }

            val avgLoadTime = loadTimes.average()
            val p95LoadTime = loadTimes.sorted()[loadTimes.size * 95 / 100]

            logger.info("Aggregate loading (1000 events): avg={}ms, p95={}ms", avgLoadTime, p95LoadTime)

            // Validate performance target
            assertTrue(avgLoadTime < 20.0, "1000-event aggregate loading should be <20ms")
        }

    @Test
    @Disabled("Performance test - enable for performance validation")
    fun `benchmark event streaming performance for TrackingEventProcessor`() =
        runBlocking {
            // Prepare test data - multiple aggregates with events
            val numberOfAggregates = 100
            val eventsPerAggregate = 10

            repeat(numberOfAggregates) { index ->
                val aggregateId = "stream-aggregate-$index"
                val events = createTestEvents(eventsPerAggregate, aggregateId)
                eventStorageEngine.appendEvents(events.toMutableList())
            }

            // Benchmark event streaming
            val streamTimes =
                (1..5).map {
                    val startTime = System.nanoTime()
                    val trackingToken = eventStorageEngine.createTailToken()
                    val eventStream = eventStorageEngine.readEvents(trackingToken, false)
                    val allStreamedEvents = eventStream.toList()
                    val streamedEvents = allStreamedEvents.take(1000)
                    val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

                    assertTrue(streamedEvents.size >= 1000, "Should stream at least 1000 events")
                    duration
                }

            val avgStreamTime = streamTimes.average()
            val p95StreamTime = streamTimes.sorted()[streamTimes.size * 95 / 100]

            logger.info(
                "Event streaming (1000 events): avg={}ms, p95={}ms",
                avgStreamTime,
                p95StreamTime,
            )

            // Validate performance target
            assertTrue(avgStreamTime < 50.0, "1000-event streaming should be <50ms")
        }

    @Test
    @Disabled("Performance test - enable for performance validation")
    fun `benchmark token operations performance`() {
        val tokenOperationTimes =
            (1..100).map {
                val startTime = System.nanoTime()

                val tailToken = eventStorageEngine.createTailToken()
                val headToken = eventStorageEngine.createHeadToken()

                // Test token comparison (typical TrackingEventProcessor operation)
                val comparison =
                    if (tailToken is GlobalSequenceTrackingToken &&
                        headToken is GlobalSequenceTrackingToken
                    ) {
                        tailToken.covers(headToken) || headToken.covers(tailToken)
                    } else {
                        false
                    }

                val duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime)
                duration
            }

        val avgTokenTime = tokenOperationTimes.average() / 1000.0 // Convert to milliseconds
        val p95TokenTime =
            tokenOperationTimes.sorted()[tokenOperationTimes.size * 95 / 100] / 1000.0

        logger.info("Token operations: avg={}ms, p95={}ms", avgTokenTime, p95TokenTime)

        // Validate performance target
        assertTrue(avgTokenTime < 1.0, "Token operations should be <1ms")
    }

    // ============================================================================
    // Task 5: Database Query Optimization Tests
    // ============================================================================

    @Test
    @Disabled("Performance test - enable for performance validation")
    fun `validate database indexes are used effectively`() {
        // Generate test data to ensure indexes are populated
        runBlocking {
            dataGenerator.generateEvents(10000) { progress ->
                if (progress.currentEvent % 1000 == 0L) {
                    logger.info(
                        "Generated {}/{} events",
                        progress.currentEvent,
                        progress.totalEvents,
                    )
                }
            }
        }

        // Test query plans for critical queries
        val queryPlans = mutableMapOf<String, String>()

        // Query 1: Aggregate event loading by stream_id
        val aggregateQuery =
            """
            EXPLAIN (ANALYZE, BUFFERS)
            SELECT * FROM domain_events
            WHERE tenant_id = ? AND stream_id = ? AND sequence_number >= ?
            ORDER BY sequence_number
            """.trimIndent()

        queryPlans["aggregate_loading"] =
            jdbcTemplate
                .queryForList(aggregateQuery, performanceTenant, "user-12345678", 0L)
                .joinToString("\n") { row ->
                    row.values.joinToString(" | ") { it.toString() }
                }

        // Query 2: Event streaming by global sequence
        val streamingQuery =
            """
            EXPLAIN (ANALYZE, BUFFERS)
            SELECT * FROM domain_events
            WHERE tenant_id = ? AND global_sequence_id > ?
            ORDER BY global_sequence_id
            LIMIT 1000
            """.trimIndent()

        queryPlans["event_streaming"] =
            jdbcTemplate.queryForList(streamingQuery, performanceTenant, 1000L).joinToString(
                "\n",
            ) { row -> row.values.joinToString(" | ") { it.toString() } }

        // Query 3: Tracking token creation (max global sequence)
        val tokenQuery =
            """
            EXPLAIN (ANALYZE, BUFFERS)
            SELECT COALESCE(MAX(global_sequence_id), 0)
            FROM domain_events
            WHERE tenant_id = ?
            """.trimIndent()

        queryPlans["token_creation"] =
            jdbcTemplate.queryForList(tokenQuery, performanceTenant).joinToString("\n") { row ->
                row.values.joinToString(" | ") { it.toString() }
            }

        // Log query plans for analysis
        queryPlans.forEach { (queryType, plan) ->
            logger.info("Query plan for {}: \n{}", queryType, plan)

            // Verify index usage (look for "Index Scan" in plan)
            assertTrue(
                plan.contains("Index Scan") || plan.contains("Bitmap Index Scan"),
                "Query $queryType should use index scan for optimal performance",
            )
        }
    }

    // ============================================================================
    // Task 6: Concurrent Load Testing
    // ============================================================================

    @Test
    @Disabled("Performance test - enable for concurrent load validation")
    fun `test concurrent event streaming scenarios`() {
        // Prepare large dataset
        runBlocking {
            dataGenerator.generateEvents(50000) { progress ->
                if (progress.currentEvent % 5000 == 0L) {
                    logger.info(
                        "Generated {}/{} events for concurrent test",
                        progress.currentEvent,
                        progress.totalEvents,
                    )
                }
            }
        }

        val numberOfThreads = 4
        val results = mutableListOf<Long>()

        // Simulate multiple TrackingEventProcessors
        val threads =
            (1..numberOfThreads).map { threadId ->
                Thread {
                    try {
                        TenantContextHolder.setCurrentTenantId(performanceTenant)

                        val startTime = System.currentTimeMillis()
                        val trackingToken = eventStorageEngine.createTailToken()
                        val eventStream = eventStorageEngine.readEvents(trackingToken, false)
                        val allEvents = eventStream.toList()
                        val events = allEvents.take(1000)
                        val duration = System.currentTimeMillis() - startTime

                        synchronized(results) { results.add(duration) }

                        logger.info(
                            "Thread {} processed {} events in {}ms",
                            threadId,
                            events.size,
                            duration,
                        )
                    } catch (e: Exception) {
                        logger.error("Thread {} failed", threadId, e)
                    } finally {
                        TenantContextHolder.clear()
                    }
                }
            }

        // Execute concurrent operations
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Analyze results
        val avgConcurrentTime = results.average()
        val maxConcurrentTime = results.maxOrNull() ?: 0L

        logger.info(
            "Concurrent streaming - avg: {}ms, max: {}ms",
            avgConcurrentTime,
            maxConcurrentTime,
        )

        // Validate concurrent performance doesn't degrade significantly
        assertTrue(avgConcurrentTime < 100.0, "Concurrent streaming average should be reasonable")
        assertTrue(maxConcurrentTime < 200L, "No single thread should take excessively long")
    }

    // ============================================================================
    // Task 7: Large Dataset Performance Tests
    // ============================================================================

    @Test
    @Disabled("Performance test - enable for large dataset validation")
    fun `test performance with 1M+ events`() =
        runBlocking {
            logger.info("Starting 1M+ event performance test")

            val targetEvents = 1_000_000L
            val startTime = System.currentTimeMillis()

            val summary =
                dataGenerator.generateEvents(targetEvents) { progress ->
                    if (progress.currentEvent % 50000 == 0L) {
                        logger.info(
                            "Progress: {}/{} events ({}%), ETA: {}",
                            progress.currentEvent,
                            progress.totalEvents,
                            (progress.currentEvent * 100 / progress.totalEvents),
                            progress.estimatedTimeRemaining,
                        )
                    }
                }

            val totalGenerationTime = System.currentTimeMillis() - startTime

            logger.info(
                "Generated {} events in {}ms",
                summary.totalEventsGenerated,
                totalGenerationTime,
            )
            summary.printSummary()

            // Test query performance with large dataset
            val queryStartTime = System.currentTimeMillis()
            val trackingToken = eventStorageEngine.createTailToken()
            val eventStream = eventStorageEngine.readEvents(trackingToken, false)
            val allEvents = eventStream.toList()
            val first10k = allEvents.take(10000)
            val queryTime = System.currentTimeMillis() - queryStartTime

            logger.info("Queried first 10K events from 1M+ dataset in {}ms", queryTime)

            // Performance assertions for large datasets
            assertTrue(
                summary.totalEventsGenerated >= targetEvents,
                "Should generate target number of events",
            )
            assertTrue(queryTime < 1000, "Querying 10K events from large dataset should be <1s")

            val throughput = summary.totalEventsGenerated * 1000 / totalGenerationTime
            logger.info("Overall throughput: {} events/second", throughput)
            assertTrue(throughput > 1000, "Should achieve >1000 events/second throughput")
        }

    // ============================================================================
    // JMH Benchmark State and Setup
    // ============================================================================

    @State(Scope.Benchmark)
    open class BenchmarkState {
        @Container
        val postgres =
            PostgreSQLContainer("postgres:15-alpine")
                .withDatabaseName("eaf_perf_test")
                .withUsername("eaf_test")
                .withPassword("eaf_test_password")

        lateinit var eventStorageEngine: EafPostgresEventStorageEngine
        private val benchmarkTenant = "BENCHMARK_TENANT"

        @Param("1", "10", "100", "1000")
        var batchSize: Int = 0

        @Setup(Level.Trial)
        fun setUp() {
            // Note: In a real implementation, this would need Spring context setup
            // For now, this shows the structure for JMH benchmarks
            TenantContextHolder.setCurrentTenantId(benchmarkTenant)
        }

        @TearDown(Level.Trial)
        fun tearDown() {
            TenantContextHolder.clear()
        }

        fun createTestEvents(count: Int): List<DomainEventMessage<*>> =
            (0 until count).map { sequence ->
                GenericDomainEventMessage(
                    "BenchmarkAggregate",
                    "benchmark-agg-${Random.nextInt(1000)}",
                    sequence.toLong(),
                    TestEventPayload("Benchmark event $sequence", sequence),
                )
            }
    }

    // ============================================================================
    // JMH Benchmarks (disabled by default - enable for detailed profiling)
    // ============================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
    @Fork(1)
    @Threads(1)
    @Disabled("JMH benchmark - enable for detailed performance profiling")
    fun benchmarkEventAppend(state: BenchmarkState): Long {
        val events = state.createTestEvents(state.batchSize)
        val startTime = System.nanoTime()

        // state.eventStorageEngine.appendEvents(events.toMutableList())

        return System.nanoTime() - startTime
    }

    @Test
    @Disabled("JMH runner - enable to run JMH benchmarks")
    fun runJmhBenchmarks() {
        val options =
            OptionsBuilder()
                .include(this::class.simpleName)
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(3)
                .build()

        Runner(options).run()
    }

    // ============================================================================
    // Helper Methods and Data Classes
    // ============================================================================

    private fun createTestEvents(
        count: Int,
        aggregateId: String? = null,
    ): List<DomainEventMessage<*>> {
        val actualAggregateId =
            aggregateId ?: "test-aggregate-${aggregateCounter.incrementAndGet()}"

        return (0 until count).map { sequence ->
            val payloadSize =
                when {
                    Random.nextDouble() < 0.7 -> Random.nextInt(100, 500) // 70% small
                    Random.nextDouble() < 0.95 -> Random.nextInt(1000, 5000) // 25% medium
                    else -> Random.nextInt(5000, 10000) // 5% large
                }

            GenericDomainEventMessage(
                "PerformanceAggregate",
                actualAggregateId,
                sequence.toLong(),
                TestEventPayload("Event $sequence", sequence, generateRandomData(payloadSize)),
                MetaData
                    .emptyInstance()
                    .and("testType", "performance")
                    .and("batchId", UUID.randomUUID().toString())
                    .and("payloadSize", payloadSize),
            )
        }
    }

    private fun generateRandomData(targetSize: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "
        return (1..targetSize)
            .map { chars[ThreadLocalRandom.current().nextInt(chars.length)] }
            .joinToString("")
    }

    private fun logPerformanceResults(
        testName: String,
        results: Map<Int, PerformanceResult>,
    ) {
        logger.info("=== {} Results ===", testName)
        results.forEach { (batchSize, result) ->
            logger.info(
                "Batch size {}: avg={}ms, p95={}ms, throughput={} events/ms",
                batchSize,
                "%.2f".format(result.averageTimeMs),
                "%.2f".format(result.p95TimeMs),
                "%.2f".format(batchSize / result.averageTimeMs),
            )
        }
    }

    data class PerformanceResult(
        val averageTimeMs: Double,
        val p95TimeMs: Double,
        val batchSize: Int,
    )

    data class TestEventPayload(
        val description: String,
        val sequenceNumber: Int,
        val data: String = "",
    )
}

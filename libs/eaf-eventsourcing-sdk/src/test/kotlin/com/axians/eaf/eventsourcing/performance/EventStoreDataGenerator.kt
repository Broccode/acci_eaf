@file:Suppress("ExplicitGarbageCollectionCall", "UnusedPrivateProperty")

package com.axians.eaf.eventsourcing.performance

import com.axians.eaf.core.tenancy.TenantContextHolder
import com.axians.eaf.eventsourcing.axon.EafPostgresEventStorageEngine
import kotlinx.coroutines.coroutineScope
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.messaging.MetaData
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.random.Random

/**
 * Utility for generating realistic event store test data for performance testing.
 *
 * Features:
 * - Multi-tenant event generation (10+ tenants)
 * - Realistic aggregate lifecycle patterns
 * - Varied payload sizes (100B - 10KB)
 * - Temporal event distribution
 * - Progress tracking and memory management
 * - Batch insertion for optimal performance
 */
class EventStoreDataGenerator(
    private val eventStorageEngine: EafPostgresEventStorageEngine,
    private val batchSize: Int = 10000,
) {
    private val logger = LoggerFactory.getLogger(EventStoreDataGenerator::class.java)
    private val eventCounter = AtomicLong(0)

    // Test tenant configurations
    private val tenants =
        listOf(
            "TENANT_LARGE_1",
            "TENANT_LARGE_2", // 40% of total events (80/20 rule)
            "TENANT_MEDIUM_1",
            "TENANT_MEDIUM_2",
            "TENANT_MEDIUM_3", // 35% of total events
            "TENANT_SMALL_1",
            "TENANT_SMALL_2",
            "TENANT_SMALL_3",
            "TENANT_SMALL_4",
            "TENANT_SMALL_5", // 25% of total events
        )

    // Aggregate type configurations
    private val aggregateTypes =
        mapOf(
            "User" to
                AggregateConfig(
                    eventTypes =
                        listOf(
                            "UserCreated",
                            "UserActivated",
                            "UserDeactivated",
                            "UserDeleted",
                        ),
                    avgEventsPerAggregate = 15,
                    payloadSizeRange = 200..800,
                ),
            "Order" to
                AggregateConfig(
                    eventTypes =
                        listOf(
                            "OrderCreated",
                            "OrderItemAdded",
                            "OrderShipped",
                            "OrderCompleted",
                            "OrderCancelled",
                        ),
                    avgEventsPerAggregate = 8,
                    payloadSizeRange = 500..2000,
                ),
            "Product" to
                AggregateConfig(
                    eventTypes =
                        listOf(
                            "ProductCreated",
                            "ProductUpdated",
                            "ProductPriceChanged",
                            "ProductDiscontinued",
                        ),
                    avgEventsPerAggregate = 25,
                    payloadSizeRange = 300..1500,
                ),
            "Invoice" to
                AggregateConfig(
                    eventTypes =
                        listOf(
                            "InvoiceCreated",
                            "InvoiceLineAdded",
                            "InvoiceSent",
                            "InvoicePaid",
                            "InvoiceCancelled",
                        ),
                    avgEventsPerAggregate = 6,
                    payloadSizeRange = 800..3000,
                ),
            "Ticket" to
                AggregateConfig(
                    eventTypes =
                        listOf(
                            "TicketCreated",
                            "TicketAssigned",
                            "TicketCommented",
                            "TicketResolved",
                            "TicketClosed",
                        ),
                    avgEventsPerAggregate = 12,
                    payloadSizeRange = 400..5000,
                ),
        )

    data class AggregateConfig(
        val eventTypes: List<String>,
        val avgEventsPerAggregate: Int,
        val payloadSizeRange: IntRange,
    )

    data class GenerationProgress(
        val totalEvents: Long,
        val currentEvent: Long,
        val currentTenant: String,
        val currentAggregate: String,
        val estimatedTimeRemaining: String,
    )

    /**
     * Generates the specified number of events across multiple tenants and aggregates.
     *
     * @param totalEvents Total number of events to generate
     * @param progressCallback Optional callback to track generation progress
     * @return Summary of generated data
     */
    suspend fun generateEvents(
        totalEvents: Long,
        progressCallback: ((GenerationProgress) -> Unit)? = null,
    ): GenerationSummary =
        coroutineScope {
            logger.info("Starting generation of {} events across {} tenants", totalEvents, tenants.size)
            val startTime = System.currentTimeMillis()

            val tenantEventCounts = distributeTenantEvents(totalEvents)
            val generatedData = mutableMapOf<String, MutableMap<String, MutableList<String>>>()

            for ((tenantIndex, tenant) in tenants.withIndex()) {
                val eventsForTenant = tenantEventCounts[tenant] ?: 0L
                if (eventsForTenant == 0L) continue

                logger.info("Generating {} events for tenant: {}", eventsForTenant, tenant)
                TenantContextHolder.setCurrentTenantId(tenant)

                val tenantAggregates =
                    generateTenantEvents(tenant, eventsForTenant) { progress ->
                        progressCallback?.invoke(progress)
                    }

                generatedData[tenant] = tenantAggregates

                val progressPercent = ((tenantIndex + 1) * 100) / tenants.size
                logger.info(
                    "Completed tenant {} ({}/{}): {}%",
                    tenant,
                    tenantIndex + 1,
                    tenants.size,
                    progressPercent,
                )
            }

            val totalTime = System.currentTimeMillis() - startTime
            logger.info("Event generation completed in {}ms", totalTime)

            GenerationSummary(
                totalEventsGenerated = eventCounter.get(),
                totalAggregates =
                    generatedData.values.sumOf {
                        it.values.sumOf { aggregates -> aggregates.size }
                    },
                tenantBreakdown =
                    generatedData.mapValues { (_, aggregateMap) ->
                        aggregateMap.mapValues { (_, aggregateList) -> aggregateList.size }
                    },
                generationTimeMs = totalTime,
            )
        }

    /** Distributes total events across tenants following 80/20 rule. */
    private fun distributeTenantEvents(totalEvents: Long): Map<String, Long> {
        val largeTenantEvents = (totalEvents * 0.4).toLong() / 2 // 20% each for 2 large tenants
        val mediumTenantEvents =
            (totalEvents * 0.35).toLong() / 3 // ~11.7% each for 3 medium tenants
        val smallTenantEvents = (totalEvents * 0.25).toLong() / 5 // 5% each for 5 small tenants

        return mapOf(
            "TENANT_LARGE_1" to largeTenantEvents,
            "TENANT_LARGE_2" to largeTenantEvents,
            "TENANT_MEDIUM_1" to mediumTenantEvents,
            "TENANT_MEDIUM_2" to mediumTenantEvents,
            "TENANT_MEDIUM_3" to mediumTenantEvents,
            "TENANT_SMALL_1" to smallTenantEvents,
            "TENANT_SMALL_2" to smallTenantEvents,
            "TENANT_SMALL_3" to smallTenantEvents,
            "TENANT_SMALL_4" to smallTenantEvents,
            "TENANT_SMALL_5" to smallTenantEvents,
        )
    }

    /** Generates events for a specific tenant with realistic aggregate patterns. */
    private suspend fun generateTenantEvents(
        tenant: String,
        eventCount: Long,
        progressCallback: ((GenerationProgress) -> Unit)? = null,
    ): MutableMap<String, MutableList<String>> {
        val aggregateMap = mutableMapOf<String, MutableList<String>>()
        val eventBatch = mutableListOf<DomainEventMessage<*>>()
        var eventsGenerated = 0L
        val batchStartTime = System.currentTimeMillis()

        while (eventsGenerated < eventCount) {
            // Select aggregate type based on realistic distribution
            val aggregateType = selectAggregateType()
            val config = aggregateTypes[aggregateType]!!

            // Create aggregate lifecycle
            val aggregateId = generateAggregateId(aggregateType)
            val aggregateEvents = generateAggregateLifecycle(aggregateType, aggregateId, config)

            eventBatch.addAll(aggregateEvents)
            eventsGenerated += aggregateEvents.size

            // Track aggregates
            aggregateMap.computeIfAbsent(aggregateType) { mutableListOf() }.add(aggregateId)

            // Batch insert when threshold reached
            if (eventBatch.size >= batchSize || eventsGenerated >= eventCount) {
                val eventsToInsert =
                    if (eventsGenerated > eventCount) {
                        eventBatch.take(
                            (eventCount - (eventsGenerated - eventBatch.size)).toInt(),
                        )
                    } else {
                        eventBatch
                    }

                eventStorageEngine.appendEvents(eventsToInsert.toMutableList())
                eventCounter.addAndGet(eventsToInsert.size.toLong())

                // Report progress
                progressCallback?.invoke(
                    GenerationProgress(
                        totalEvents = eventCount,
                        currentEvent = eventsGenerated,
                        currentTenant = tenant,
                        currentAggregate = aggregateId,
                        estimatedTimeRemaining =
                            estimateTimeRemaining(
                                eventsGenerated,
                                eventCount,
                                batchStartTime,
                            ),
                    ),
                )

                eventBatch.clear()

                // Memory management
                if (eventsGenerated % (batchSize * 10) == 0L) {
                    System.gc()
                }
            }
        }

        return aggregateMap
    }

    /** Generates a realistic lifecycle of events for an aggregate. */
    private fun generateAggregateLifecycle(
        aggregateType: String,
        aggregateId: String,
        config: AggregateConfig,
    ): List<DomainEventMessage<*>> {
        val events = mutableListOf<DomainEventMessage<*>>()
        val eventCount = generateEventCount(config.avgEventsPerAggregate)
        val baseTime = generateRealisticTimestamp()

        for (sequenceNumber in 0 until eventCount) {
            val eventType = selectEventType(config.eventTypes, sequenceNumber, eventCount)
            val payload = generateEventPayload(eventType, config.payloadSizeRange)
            val timestamp =
                baseTime.plus(
                    (sequenceNumber * Random.nextLong(1, 3600)).toLong(),
                    ChronoUnit.SECONDS,
                )

            val metadata =
                MetaData
                    .emptyInstance()
                    .and("eventType", eventType)
                    .and("aggregateType", aggregateType)
                    .and("generatedAt", Instant.now().toString())
                    .and("sequenceInLifecycle", sequenceNumber)
                    .and("totalInLifecycle", eventCount)
                    .and("payloadSize", payload.toString().length)

            val event =
                GenericDomainEventMessage(
                    aggregateType,
                    aggregateId,
                    sequenceNumber.toLong(),
                    payload,
                    metadata,
                )

            events.add(event)
        }

        return events
    }

    /** Selects appropriate event type based on lifecycle position. */
    private fun selectEventType(
        eventTypes: List<String>,
        sequenceNumber: Int,
        totalEvents: Int,
    ): String =
        when {
            sequenceNumber == 0 -> eventTypes.first() // Always start with "Created" event
            sequenceNumber == totalEvents - 1 &&
                eventTypes.any {
                    it.contains("Closed") ||
                        it.contains("Completed") ||
                        it.contains("Deleted")
                } -> {
                eventTypes.lastOrNull {
                    it.contains("Closed") || it.contains("Completed") || it.contains("Deleted")
                }
                    ?: eventTypes.random()
            }
            else -> eventTypes.drop(1).random() // Random from non-creation events
        }

    /** Generates realistic event payload with specified size characteristics. */
    private fun generateEventPayload(
        eventType: String,
        sizeRange: IntRange,
    ): EventPayload {
        val targetSize = Random.nextInt(sizeRange.first, sizeRange.last)

        return when {
            targetSize <= 500 -> SmallEventPayload(eventType, generateRandomData(targetSize / 4))
            targetSize <= 2000 ->
                MediumEventPayload(
                    eventType,
                    generateRandomData(targetSize / 3),
                    generateRandomData(targetSize / 6),
                )
            else ->
                LargeEventPayload(
                    eventType,
                    generateRandomData(targetSize / 4),
                    generateRandomData(targetSize / 4),
                    generateRandomData(targetSize / 4),
                    generateComplexData(targetSize / 8),
                )
        }
    }

    /** Generates realistic timestamp within the last 6 months. */
    private fun generateRealisticTimestamp(): Instant {
        val now = Instant.now()
        val sixMonthsAgo = now.minus(180, ChronoUnit.DAYS)
        val randomDays = ThreadLocalRandom.current().nextLong(0, 180)
        val randomSeconds = ThreadLocalRandom.current().nextLong(0, 86400) // Random time within day

        return sixMonthsAgo
            .plus(randomDays, ChronoUnit.DAYS)
            .plus(randomSeconds, ChronoUnit.SECONDS)
    }

    private fun selectAggregateType(): String = aggregateTypes.keys.random()

    private fun generateAggregateId(aggregateType: String): String =
        "${aggregateType.lowercase()}-${UUID.randomUUID().toString().take(8)}"

    private fun generateEventCount(average: Int): Int {
        // Use normal distribution around average with some variance
        val variance = max(1, average / 4)
        return max(1, Random.nextInt(average - variance, average + variance + 1))
    }

    private fun generateRandomData(targetLength: Int): String {
        val words =
            listOf(
                "performance",
                "testing",
                "event",
                "sourcing",
                "aggregate",
                "domain",
                "business",
                "process",
                "workflow",
                "integration",
                "microservice",
                "transaction",
                "consistency",
                "resilience",
                "scalability",
                "monitoring",
                "observability",
                "tracing",
                "correlation",
                "tenant",
            )

        val result = StringBuilder()
        while (result.length < targetLength) {
            if (result.isNotEmpty()) result.append(" ")
            result.append(words.random())
        }

        return result.toString().take(targetLength)
    }

    private fun generateComplexData(approximateSize: Int): Map<String, Any> =
        mapOf(
            "id" to UUID.randomUUID().toString(),
            "timestamp" to Instant.now().toString(),
            "data" to generateRandomData(approximateSize / 2),
            "metadata" to
                mapOf(
                    "version" to Random.nextInt(1, 10),
                    "source" to "performance-test",
                    "tags" to listOf("test", "performance", "generated"),
                ),
        )

    private fun estimateTimeRemaining(
        current: Long,
        total: Long,
        startTime: Long,
    ): String {
        if (current == 0L) return "calculating..."

        val elapsed = System.currentTimeMillis() - startTime
        val rate = current.toDouble() / elapsed
        val remaining = ((total - current) / rate).toLong()

        return when {
            remaining < 60000 -> "${remaining / 1000}s"
            remaining < 3600000 -> "${remaining / 60000}m ${(remaining % 60000) / 1000}s"
            else -> "${remaining / 3600000}h ${(remaining % 3600000) / 60000}m"
        }
    }

    // Event payload classes for different size categories
    sealed class EventPayload

    data class SmallEventPayload(
        val type: String,
        val description: String,
    ) : EventPayload()

    data class MediumEventPayload(
        val type: String,
        val description: String,
        val details: String,
    ) : EventPayload()

    data class LargeEventPayload(
        val type: String,
        val description: String,
        val details: String,
        val additionalInfo: String,
        val complexData: Map<String, Any>,
    ) : EventPayload()

    data class GenerationSummary(
        val totalEventsGenerated: Long,
        val totalAggregates: Int,
        val tenantBreakdown: Map<String, Map<String, Int>>,
        val generationTimeMs: Long,
    ) {
        fun printSummary() {
            println("=== Event Generation Summary ===")
            println("Total Events Generated: $totalEventsGenerated")
            println("Total Aggregates: $totalAggregates")
            println("Generation Time: ${generationTimeMs}ms")
            println("Rate: ${totalEventsGenerated * 1000 / generationTimeMs} events/second")
            println("\nTenant Breakdown:")
            tenantBreakdown.forEach { (tenant, aggregates) ->
                val totalEvents = aggregates.values.sum()
                println("  $tenant: $totalEvents events across ${aggregates.size} aggregate types")
            }
        }
    }
}

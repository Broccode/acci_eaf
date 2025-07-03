package com.axians.eaf.controlplane.integration.security

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test utility for capturing and verifying events in integration tests.
 *
 * This component provides:
 * - Thread-safe event capture for concurrent testing
 * - Timeout and retry mechanisms for async event verification
 * - Event metadata extraction and validation
 * - Event filtering and search capabilities
 */
@Component
class TestEventCapture {
    private val capturedEvents = ConcurrentLinkedQueue<CapturedEvent>()
    private val eventCounter = AtomicInteger(0)

    // For async verification
    private var expectedEventCount: Int = 0
    private var eventLatch: CountDownLatch? = null

    /** Data class representing a captured event with its metadata. */
    data class CapturedEvent(
        val event: Any,
        val metadata: Map<String, Any>,
        val subject: String,
        val tenantId: String,
        val captureTimestamp: Instant = Instant.now(),
        val eventId: String =
            java.util.UUID
                .randomUUID()
                .toString(),
    )

    /**
     * Captures an event with its metadata. This method is thread-safe and can be called from event
     * handlers.
     */
    fun captureEvent(
        event: Any,
        metadata: Map<String, Any>,
        subject: String,
        tenantId: String,
    ) {
        val capturedEvent =
            CapturedEvent(
                event = event,
                metadata = metadata,
                subject = subject,
                tenantId = tenantId,
            )

        capturedEvents.add(capturedEvent)
        eventCounter.incrementAndGet()

        // Signal waiting tests
        eventLatch?.countDown()
    }

    /**
     * Sets up async verification for a specific number of events. Tests can use this with
     * Awaitility for timeout handling.
     */
    fun setExpectedEvents(
        count: Int,
        latch: CountDownLatch,
    ) {
        expectedEventCount = count
        eventLatch = latch
    }

    /**
     * Returns all captured events. This is thread-safe and returns a snapshot of current events.
     */
    fun getCapturedEvents(): List<CapturedEvent> = capturedEvents.toList()

    /** Returns captured events filtered by tenant ID. */
    fun getEventsByTenant(tenantId: String): List<CapturedEvent> = capturedEvents.filter { it.tenantId == tenantId }

    /** Returns captured events filtered by event type. */
    fun getEventsByType(eventClass: Class<*>): List<CapturedEvent> =
        capturedEvents.filter { eventClass.isInstance(it.event) }

    /** Returns captured events filtered by subject. */
    fun getEventsBySubject(subject: String): List<CapturedEvent> = capturedEvents.filter { it.subject == subject }

    /** Searches for events with specific metadata values. */
    fun getEventsWithMetadata(
        key: String,
        value: Any,
    ): List<CapturedEvent> = capturedEvents.filter { it.metadata[key] == value }

    /** Returns the count of captured events. */
    fun getEventCount(): Int = eventCounter.get()

    /**
     * Waits for the expected number of events with a timeout. Returns true if all expected events
     * were captured within the timeout.
     */
    fun waitForEvents(timeoutSeconds: Long = 10): Boolean = eventLatch?.await(timeoutSeconds, TimeUnit.SECONDS) ?: false

    /** Resets the capture state for a new test. Should be called in @BeforeEach methods. */
    fun reset() {
        capturedEvents.clear()
        eventCounter.set(0)
        expectedEventCount = 0
        eventLatch = null
    }

    /**
     * Verifies that all captured events have the required security metadata. Throws assertion
     * errors if any events are missing required fields.
     */
    fun verifySecurityMetadata(requiredFields: Set<String> = DEFAULT_SECURITY_FIELDS) {
        val eventsWithMissingMetadata =
            capturedEvents.filter { event ->
                requiredFields.any { field -> !event.metadata.containsKey(field) }
            }

        if (eventsWithMissingMetadata.isNotEmpty()) {
            val missingFieldsReport =
                eventsWithMissingMetadata.joinToString("\n") { event ->
                    val missingFields =
                        requiredFields.filter { !event.metadata.containsKey(it) }
                    "Event ${event.eventId} (${event.event::class.simpleName}) missing: $missingFields"
                }
            throw AssertionError("Events missing required security metadata:\n$missingFieldsReport")
        }
    }

    /** Verifies tenant isolation by ensuring no events contain data from other tenants. */
    fun verifyTenantIsolation(allowedTenantId: String) {
        val violatingEvents =
            capturedEvents.filter { event ->
                val eventTenantId = event.metadata["tenant_id"] as? String
                eventTenantId != null &&
                    eventTenantId != allowedTenantId &&
                    eventTenantId != "SYSTEM"
            }

        if (violatingEvents.isNotEmpty()) {
            val violationReport =
                violatingEvents.joinToString("\n") { event ->
                    "Event ${event.eventId} has tenant_id=" +
                        "'${event.metadata["tenant_id"] ?: "null"}' but expected '$allowedTenantId'"
                }
            throw AssertionError("Tenant isolation violated:\n$violationReport")
        }
    }

    /** Returns detailed statistics about captured events for debugging. */
    fun getEventStatistics(): EventStatistics {
        val events = getCapturedEvents()

        return EventStatistics(
            totalEvents = events.size,
            eventsByType =
                events.groupBy { it.event::class.simpleName ?: "Unknown" }.mapValues {
                    it.value.size
                },
            eventsByTenant = events.groupBy { it.tenantId }.mapValues { it.value.size },
            eventsBySubject = events.groupBy { it.subject }.mapValues { it.value.size },
            metadataCompleteness = calculateMetadataCompleteness(events),
        )
    }

    private fun calculateMetadataCompleteness(events: List<CapturedEvent>): Map<String, Int> =
        DEFAULT_SECURITY_FIELDS.associateWith { field ->
            events.count { it.metadata.containsKey(field) }
        }

    data class EventStatistics(
        val totalEvents: Int,
        val eventsByType: Map<String, Int>,
        val eventsByTenant: Map<String, Int>,
        val eventsBySubject: Map<String, Int>,
        val metadataCompleteness: Map<String, Int>,
    )

    companion object {
        val DEFAULT_SECURITY_FIELDS =
            setOf("tenant_id", "user_id", "correlation_id", "request_timestamp")
    }
}

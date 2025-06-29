package com.axians.eaf.eventsourcing.adapter

import com.axians.eaf.eventsourcing.exception.OptimisticLockingFailureException
import com.axians.eaf.eventsourcing.model.AggregateSnapshot
import com.axians.eaf.eventsourcing.model.PersistedEvent
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * JDBC-based implementation of EventStoreRepository using PostgreSQL.
 *
 * This implementation provides atomic event append operations with optimistic concurrency control,
 * event stream retrieval, and snapshot management with strict tenant isolation.
 */
@Repository
class JdbcEventStoreRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : EventStoreRepository {
    companion object {
        // Event queries
        private const val INSERT_EVENT_SQL =
            """
            INSERT INTO domain_events (
                event_id, stream_id, aggregate_id, aggregate_type, expected_version,
                sequence_number, tenant_id, event_type, payload, metadata, timestamp_utc
            ) VALUES (
                :eventId, :streamId, :aggregateId, :aggregateType, :expectedVersion,
                :sequenceNumber, :tenantId, :eventType, :payload::jsonb, :metadata::jsonb, :timestampUtc
            )
        """

        private const val SELECT_EVENTS_SQL =
            """
            SELECT global_sequence_id, event_id, stream_id, aggregate_id, aggregate_type,
                   expected_version, sequence_number, tenant_id, event_type, payload, metadata, timestamp_utc
            FROM domain_events
            WHERE tenant_id = :tenantId AND aggregate_id = :aggregateId
            AND sequence_number >= :fromSequenceNumber
            ORDER BY sequence_number ASC
        """

        private const val SELECT_EVENTS_RANGE_SQL =
            """
            SELECT global_sequence_id, event_id, stream_id, aggregate_id, aggregate_type,
                   expected_version, sequence_number, tenant_id, event_type, payload, metadata, timestamp_utc
            FROM domain_events
            WHERE tenant_id = :tenantId AND aggregate_id = :aggregateId
            AND sequence_number >= :fromSequenceNumber
            AND sequence_number <= :toSequenceNumber
            ORDER BY sequence_number ASC
        """

        private const val SELECT_CURRENT_VERSION_SQL =
            """
            SELECT MAX(sequence_number)
            FROM domain_events
            WHERE tenant_id = :tenantId AND aggregate_id = :aggregateId
        """

        // Global streaming queries for TrackingEventProcessor
        private const val SELECT_EVENTS_FROM_GLOBAL_SEQUENCE_SQL =
            """
            SELECT global_sequence_id, event_id, stream_id, aggregate_id, aggregate_type,
                   expected_version, sequence_number, tenant_id, event_type, payload, metadata, timestamp_utc
            FROM domain_events
            WHERE tenant_id = :tenantId AND global_sequence_id > :fromGlobalSequence
            ORDER BY global_sequence_id ASC
            LIMIT :batchSize
        """

        private const val SELECT_MAX_GLOBAL_SEQUENCE_SQL =
            """
            SELECT COALESCE(MAX(global_sequence_id), 0)
            FROM domain_events
            WHERE tenant_id = :tenantId
        """

        // Snapshot queries
        private const val INSERT_SNAPSHOT_SQL =
            """
            INSERT INTO aggregate_snapshots (
                aggregate_id, tenant_id, aggregate_type, last_sequence_number,
                snapshot_payload_jsonb, version, timestamp_utc
            ) VALUES (
                :aggregateId, :tenantId, :aggregateType, :lastSequenceNumber,
                :snapshotPayloadJsonb::jsonb, :version, :timestampUtc
            )
            ON CONFLICT (tenant_id, aggregate_id)
            DO UPDATE SET
                last_sequence_number = EXCLUDED.last_sequence_number,
                snapshot_payload_jsonb = EXCLUDED.snapshot_payload_jsonb,
                version = EXCLUDED.version,
                timestamp_utc = EXCLUDED.timestamp_utc
        """

        private const val SELECT_SNAPSHOT_SQL =
            """
            SELECT id, aggregate_id, tenant_id, aggregate_type, last_sequence_number,
                   snapshot_payload_jsonb, version, timestamp_utc
            FROM aggregate_snapshots
            WHERE tenant_id = :tenantId AND aggregate_id = :aggregateId
        """
    }

    @Transactional
    override suspend fun appendEvents(
        events: List<PersistedEvent>,
        tenantId: String,
        aggregateId: String,
        expectedVersion: Long?,
    ): Unit =
        withContext(Dispatchers.IO) {
            require(events.isNotEmpty()) { "Events list cannot be empty" }

            // Validate that all events belong to the same aggregate and tenant
            events.forEach { event ->
                require(event.aggregateId == aggregateId) {
                    "All events must belong to the same aggregate. Expected: $aggregateId, found: ${event.aggregateId}"
                }
                require(event.tenantId == tenantId) {
                    "All events must belong to the same tenant. Expected: $tenantId, found: ${event.tenantId}"
                }
            }

            // Check optimistic concurrency
            val currentVersion = getCurrentVersionInternal(tenantId, aggregateId)
            if (expectedVersion != currentVersion) {
                throw OptimisticLockingFailureException(
                    tenantId = tenantId,
                    aggregateId = aggregateId,
                    expectedVersion = expectedVersion,
                    actualVersion = currentVersion,
                )
            }

            // Insert events atomically
            try {
                events.forEach { event ->
                    val parameters =
                        MapSqlParameterSource().apply {
                            addValue("eventId", event.eventId)
                            addValue("streamId", event.streamId)
                            addValue("aggregateId", event.aggregateId)
                            addValue("aggregateType", event.aggregateType)
                            addValue("expectedVersion", event.expectedVersion)
                            addValue("sequenceNumber", event.sequenceNumber)
                            addValue("tenantId", event.tenantId)
                            addValue("eventType", event.eventType)
                            addValue("payload", event.payload)
                            addValue("metadata", event.metadata)
                            addValue("timestampUtc", Timestamp.from(event.timestampUtc))
                        }

                    jdbcTemplate.update(INSERT_EVENT_SQL, parameters)
                }
            } catch (e: DuplicateKeyException) {
                // This can happen if there's a concurrent modification
                val newCurrentVersion = getCurrentVersionInternal(tenantId, aggregateId)
                throw OptimisticLockingFailureException(
                    tenantId = tenantId,
                    aggregateId = aggregateId,
                    expectedVersion = expectedVersion,
                    actualVersion = newCurrentVersion,
                    cause = e,
                )
            }
        }

    override suspend fun getEvents(
        tenantId: String,
        aggregateId: String,
        fromSequenceNumber: Long,
    ): List<PersistedEvent> =
        withContext(Dispatchers.IO) {
            val parameters =
                MapSqlParameterSource().apply {
                    addValue("tenantId", tenantId)
                    addValue("aggregateId", aggregateId)
                    addValue("fromSequenceNumber", fromSequenceNumber)
                }

            jdbcTemplate.query(SELECT_EVENTS_SQL, parameters, persistedEventRowMapper)
        }

    override suspend fun getEventsInRange(
        tenantId: String,
        aggregateId: String,
        fromSequenceNumber: Long,
        toSequenceNumber: Long?,
    ): List<PersistedEvent> =
        withContext(Dispatchers.IO) {
            if (toSequenceNumber == null) {
                return@withContext getEvents(tenantId, aggregateId, fromSequenceNumber)
            }

            val parameters =
                MapSqlParameterSource().apply {
                    addValue("tenantId", tenantId)
                    addValue("aggregateId", aggregateId)
                    addValue("fromSequenceNumber", fromSequenceNumber)
                    addValue("toSequenceNumber", toSequenceNumber)
                }

            jdbcTemplate.query(SELECT_EVENTS_RANGE_SQL, parameters, persistedEventRowMapper)
        }

    override suspend fun getCurrentVersion(
        tenantId: String,
        aggregateId: String,
    ): Long? = withContext(Dispatchers.IO) { getCurrentVersionInternal(tenantId, aggregateId) }

    override suspend fun readEventsFrom(
        tenantId: String,
        fromGlobalSequence: Long,
        batchSize: Int,
    ): List<PersistedEvent> =
        withContext(Dispatchers.IO) {
            val parameters =
                MapSqlParameterSource().apply {
                    addValue("tenantId", tenantId)
                    addValue("fromGlobalSequence", fromGlobalSequence)
                    addValue("batchSize", batchSize)
                }

            jdbcTemplate.query(
                SELECT_EVENTS_FROM_GLOBAL_SEQUENCE_SQL,
                parameters,
                persistedEventRowMapper,
            )
        }

    override suspend fun getMaxGlobalSequence(tenantId: String): Long =
        withContext(Dispatchers.IO) {
            val parameters = MapSqlParameterSource().apply { addValue("tenantId", tenantId) }

            jdbcTemplate.queryForObject(
                SELECT_MAX_GLOBAL_SEQUENCE_SQL,
                parameters,
                Long::class.java,
            )
                ?: 0L
        }

    private fun getCurrentVersionInternal(
        tenantId: String,
        aggregateId: String,
    ): Long? {
        val parameters =
            MapSqlParameterSource().apply {
                addValue("tenantId", tenantId)
                addValue("aggregateId", aggregateId)
            }

        return jdbcTemplate.queryForObject(SELECT_CURRENT_VERSION_SQL, parameters, Long::class.java)
    }

    @Transactional
    override suspend fun saveSnapshot(
        snapshot: AggregateSnapshot,
        tenantId: String,
        aggregateId: String,
    ): Unit =
        withContext(Dispatchers.IO) {
            require(snapshot.tenantId == tenantId) {
                "Snapshot tenant ID must match the provided tenant ID. Expected: $tenantId, found: ${snapshot.tenantId}"
            }
            require(snapshot.aggregateId == aggregateId) {
                "Snapshot aggregate ID must match the provided aggregate ID. Expected: $aggregateId, found: ${snapshot.aggregateId}"
            }

            val parameters =
                MapSqlParameterSource().apply {
                    addValue("aggregateId", snapshot.aggregateId)
                    addValue("tenantId", snapshot.tenantId)
                    addValue("aggregateType", snapshot.aggregateType)
                    addValue("lastSequenceNumber", snapshot.lastSequenceNumber)
                    addValue("snapshotPayloadJsonb", snapshot.snapshotPayloadJsonb)
                    addValue("version", snapshot.version)
                    addValue("timestampUtc", Timestamp.from(snapshot.timestampUtc))
                }

            jdbcTemplate.update(INSERT_SNAPSHOT_SQL, parameters)
        }

    override suspend fun getSnapshot(
        tenantId: String,
        aggregateId: String,
    ): AggregateSnapshot? =
        withContext(Dispatchers.IO) {
            val parameters =
                MapSqlParameterSource().apply {
                    addValue("tenantId", tenantId)
                    addValue("aggregateId", aggregateId)
                }

            val results = jdbcTemplate.query(SELECT_SNAPSHOT_SQL, parameters, snapshotRowMapper)
            results.firstOrNull()
        }

    private val persistedEventRowMapper =
        RowMapper<PersistedEvent> { rs: ResultSet, _ ->
            PersistedEvent(
                globalSequenceId = rs.getLong("global_sequence_id"),
                eventId = UUID.fromString(rs.getString("event_id")),
                streamId = rs.getString("stream_id"),
                aggregateId = rs.getString("aggregate_id"),
                aggregateType = rs.getString("aggregate_type"),
                expectedVersion = rs.getObject("expected_version") as Long?,
                sequenceNumber = rs.getLong("sequence_number"),
                tenantId = rs.getString("tenant_id"),
                eventType = rs.getString("event_type"),
                payload = rs.getString("payload"),
                metadata = rs.getString("metadata"),
                timestampUtc = rs.getTimestamp("timestamp_utc").toInstant(),
            )
        }

    private val snapshotRowMapper =
        RowMapper<AggregateSnapshot> { rs: ResultSet, _ ->
            AggregateSnapshot(
                id = rs.getLong("id"),
                aggregateId = rs.getString("aggregate_id"),
                tenantId = rs.getString("tenant_id"),
                aggregateType = rs.getString("aggregate_type"),
                lastSequenceNumber = rs.getLong("last_sequence_number"),
                snapshotPayloadJsonb = rs.getString("snapshot_payload_jsonb"),
                version = rs.getInt("version"),
                timestampUtc = rs.getTimestamp("timestamp_utc").toInstant(),
            )
        }
}

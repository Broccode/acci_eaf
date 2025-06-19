package com.axians.eaf.eventing.consumer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Repository interface for managing processed events for projector idempotency.
 */
interface ProcessedEventRepository {
    /**
     * Checks if an event has already been processed by a specific projector.
     *
     * @param projectorName the name of the projector
     * @param eventId the ID of the event
     * @param tenantId the tenant ID
     * @return true if the event has been processed, false otherwise
     */
    suspend fun isEventProcessed(
        projectorName: String,
        eventId: UUID,
        tenantId: String,
    ): Boolean

    /**
     * Marks an event as processed by a specific projector.
     *
     * @param projectorName the name of the projector
     * @param eventId the ID of the event
     * @param tenantId the tenant ID
     */
    suspend fun markEventAsProcessed(
        projectorName: String,
        eventId: UUID,
        tenantId: String,
    )
}

/**
 * JDBC-based implementation of ProcessedEventRepository using PostgreSQL.
 *
 * This implementation provides atomic operations for tracking processed events
 * with strict tenant isolation.
 */
@Repository
open class JdbcProcessedEventRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ProcessedEventRepository {
    companion object {
        private const val CHECK_PROCESSED_SQL = """
            SELECT COUNT(1)
            FROM processed_events
            WHERE projector_name = :projectorName
            AND event_id = :eventId
            AND tenant_id = :tenantId
        """

        private const val INSERT_PROCESSED_SQL = """
            INSERT INTO processed_events (projector_name, event_id, tenant_id, processed_at)
            VALUES (:projectorName, :eventId, :tenantId, :processedAt)
        """
    }

    override suspend fun isEventProcessed(
        projectorName: String,
        eventId: UUID,
        tenantId: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val parameters =
                MapSqlParameterSource().apply {
                    addValue("projectorName", projectorName)
                    addValue("eventId", eventId)
                    addValue("tenantId", tenantId)
                }

            val count = jdbcTemplate.queryForObject(CHECK_PROCESSED_SQL, parameters, Int::class.java) ?: 0
            count > 0
        }

    override suspend fun markEventAsProcessed(
        projectorName: String,
        eventId: UUID,
        tenantId: String,
    ): Unit =
        withContext(Dispatchers.IO) {
            val parameters =
                MapSqlParameterSource().apply {
                    addValue("projectorName", projectorName)
                    addValue("eventId", eventId)
                    addValue("tenantId", tenantId)
                    addValue("processedAt", Timestamp.from(Instant.now()))
                }

            jdbcTemplate.update(INSERT_PROCESSED_SQL, parameters)
        }
}

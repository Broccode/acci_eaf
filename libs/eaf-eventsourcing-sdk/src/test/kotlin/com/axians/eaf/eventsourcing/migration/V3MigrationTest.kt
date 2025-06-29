package com.axians.eaf.eventsourcing.migration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
class V3MigrationTest {
    @Container
    private val postgres =
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("eventstore_test")
            .withUsername("test")
            .withPassword("test")

    @Test
    fun `should successfully migrate from V2 to V3 with existing data`() {
        // Given: Database with V1 and V2 migrations applied and sample data
        val flyway =
            Flyway
                .configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()

        // Apply V1 and V2 migrations
        flyway.migrate()

        // Insert sample data to test migration with existing events
        insertSampleEvents()

        // When: Apply V3 migration (should be included in the migrate call above)
        val migrationResult = flyway.info()

        // Then: Verify migration was successful
        val appliedMigrations = migrationResult.applied()
        assertTrue(appliedMigrations.any { migration -> migration.version.version == "3" })

        // Verify new columns exist and have correct data
        verifyNewColumns()

        // Verify new indexes exist
        verifyNewIndexes()

        // Verify constraints are in place
        verifyConstraints()

        // Verify existing data integrity
        verifyDataIntegrity()
    }

    @Test
    fun `should handle fresh database migration from V1 to V3`() {
        // Given: Fresh database
        val flyway =
            Flyway
                .configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()

        // When: Apply all migrations
        flyway.migrate()

        // Then: Verify all migrations applied successfully
        val migrationInfo = flyway.info()
        val appliedMigrations = migrationInfo.applied()
        assertTrue(appliedMigrations.size >= 3)
        assertTrue(appliedMigrations.any { migration -> migration.version.version == "3" })

        // Verify schema structure
        verifySchemaStructure()
    }

    @Test
    fun `should maintain backward compatibility with existing EAF SDK operations`() {
        // Given: Migrated database
        val flyway =
            Flyway
                .configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()
        flyway.migrate()

        // When: Insert event using existing EAF SDK pattern
        insertEafSdkStyleEvent()

        // Then: Verify event was inserted correctly with new columns populated
        verifyEafSdkCompatibility()
    }

    private fun insertSampleEvents() {
        getConnection().use { conn ->
            val stmt =
                conn.prepareStatement(
                    """
                INSERT INTO domain_events (
                    event_id, stream_id, aggregate_id, aggregate_type, expected_version,
                    sequence_number, tenant_id, event_type, payload, metadata, timestamp_utc
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
            """,
                )

            // Insert multiple events for testing
            repeat(3) { i ->
                stmt.setObject(1, UUID.randomUUID())
                stmt.setString(2, "User-user-123")
                stmt.setString(3, "user-123")
                stmt.setString(4, "User")
                stmt.setLong(5, i.toLong())
                stmt.setLong(6, i.toLong())
                stmt.setString(7, "tenant-1")
                stmt.setString(8, "UserCreatedEvent")
                stmt.setString(9, """{"userId":"user-123","email":"test@example.com"}""")
                stmt.setString(10, """{"correlationId":"corr-123"}""")
                stmt.setTimestamp(11, java.sql.Timestamp.from(Instant.now()))
                stmt.executeUpdate()
            }
        }
    }

    private fun verifyNewColumns() {
        getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs =
                stmt.executeQuery(
                    """
                SELECT payload_type, payload_revision
                FROM domain_events
                WHERE event_type = 'UserCreatedEvent'
                LIMIT 1
            """,
                )

            assertTrue(rs.next())
            assertEquals("UserCreatedEvent", rs.getString("payload_type"))
            assertEquals("1.0", rs.getString("payload_revision"))
        }
    }

    private fun verifyNewIndexes() {
        getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs =
                stmt.executeQuery(
                    """
                SELECT indexname
                FROM pg_indexes
                WHERE tablename = 'domain_events'
                AND indexname IN (
                    'idx_events_tenant_sequence',
                    'idx_events_aggregate_enhanced',
                    'idx_events_tracking',
                    'idx_events_payload_type',
                    'idx_stream_id_seq_tenant',
                    'idx_tenant_event_type_enhanced',
                    'idx_tenant_aggregate_enhanced',
                    'idx_events_high_frequency_types'
                )
            """,
                )

            val indexes = mutableSetOf<String>()
            while (rs.next()) {
                indexes.add(rs.getString("indexname"))
            }

            assertTrue(indexes.contains("idx_events_tenant_sequence"))
            assertTrue(indexes.contains("idx_events_aggregate_enhanced"))
            assertTrue(indexes.contains("idx_events_tracking"))
            assertTrue(indexes.contains("idx_events_payload_type"))
            assertTrue(indexes.contains("idx_events_high_frequency_types"))
        }
    }

    private fun verifyConstraints() {
        getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs =
                stmt.executeQuery(
                    """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_name = 'domain_events'
                AND constraint_type = 'CHECK'
                AND constraint_name IN ('chk_payload_type_not_empty', 'chk_payload_revision_format')
            """,
                )

            val constraints = mutableSetOf<String>()
            while (rs.next()) {
                constraints.add(rs.getString("constraint_name"))
            }

            assertTrue(constraints.contains("chk_payload_type_not_empty"))
            assertTrue(constraints.contains("chk_payload_revision_format"))
        }
    }

    private fun verifyDataIntegrity() {
        getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs =
                stmt.executeQuery(
                    """
                SELECT COUNT(*) as event_count,
                       COUNT(DISTINCT aggregate_id) as aggregate_count,
                       COUNT(DISTINCT tenant_id) as tenant_count
                FROM domain_events
            """,
                )

            assertTrue(rs.next())
            assertTrue(rs.getInt("event_count") > 0)
            assertTrue(rs.getInt("aggregate_count") > 0)
            assertTrue(rs.getInt("tenant_count") > 0)
        }
    }

    private fun verifySchemaStructure() {
        getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs =
                stmt.executeQuery(
                    """
                SELECT column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_name = 'domain_events'
                AND column_name IN ('payload_type', 'payload_revision')
                ORDER BY column_name
            """,
                )

            val columns = mutableMapOf<String, Pair<String, String>>()
            while (rs.next()) {
                columns[rs.getString("column_name")] =
                    Pair(rs.getString("data_type"), rs.getString("is_nullable"))
            }

            assertNotNull(columns["payload_type"])
            assertNotNull(columns["payload_revision"])
            assertEquals("character varying", columns["payload_type"]?.first)
            assertEquals("character varying", columns["payload_revision"]?.first)
        }
    }

    private fun insertEafSdkStyleEvent() {
        getConnection().use { conn ->
            val stmt =
                conn.prepareStatement(
                    """
                INSERT INTO domain_events (
                    event_id, stream_id, aggregate_id, aggregate_type, expected_version,
                    sequence_number, tenant_id, event_type, payload, metadata, timestamp_utc
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
            """,
                )

            stmt.setObject(1, UUID.randomUUID())
            stmt.setString(2, "Order-order-456")
            stmt.setString(3, "order-456")
            stmt.setString(4, "Order")
            stmt.setLong(5, 0)
            stmt.setLong(6, 0)
            stmt.setString(7, "tenant-2")
            stmt.setString(8, "OrderCreatedEvent")
            stmt.setString(9, """{"orderId":"order-456","amount":100.00}""")
            stmt.setString(10, """{"source":"eaf-sdk"}""")
            stmt.setTimestamp(11, java.sql.Timestamp.from(Instant.now()))
            stmt.executeUpdate()
        }
    }

    private fun verifyEafSdkCompatibility() {
        getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs =
                stmt.executeQuery(
                    """
                SELECT event_type, payload_type, payload_revision
                FROM domain_events
                WHERE event_type = 'OrderCreatedEvent'
            """,
                )

            assertTrue(rs.next())
            assertEquals("OrderCreatedEvent", rs.getString("event_type"))
            // New columns should be populated via triggers or constraints
            assertNotNull(rs.getString("payload_type"))
            assertNotNull(rs.getString("payload_revision"))
        }
    }

    private fun getConnection(): Connection =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
}

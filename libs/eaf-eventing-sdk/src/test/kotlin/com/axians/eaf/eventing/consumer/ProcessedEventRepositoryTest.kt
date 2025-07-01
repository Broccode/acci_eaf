@file:Suppress("TooGenericExceptionThrown")

package com.axians.eaf.eventing.consumer

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID
import kotlin.test.assertEquals

/** Unit tests for the ProcessedEventRepository implementations. */
class ProcessedEventRepositoryTest {
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    private lateinit var repository: JdbcProcessedEventRepository

    @BeforeEach
    fun setUp() {
        jdbcTemplate = mockk()
        repository = JdbcProcessedEventRepository(jdbcTemplate)
    }

    @Test
    fun `should return true when event is already processed`() =
        runTest {
            // Given
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"

            every {
                jdbcTemplate.queryForObject(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                    Int::class.java,
                )
            } returns 1

            // When
            val result = repository.isEventProcessed(projectorName, eventId, tenantId)

            // Then
            assertEquals(true, result)
            verify {
                jdbcTemplate.queryForObject(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                    Int::class.java,
                )
            }
        }

    @Test
    fun `should return false when event is not processed`() =
        runTest {
            // Given
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"

            every {
                jdbcTemplate.queryForObject(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                    Int::class.java,
                )
            } returns 0

            // When
            val result = repository.isEventProcessed(projectorName, eventId, tenantId)

            // Then
            assertEquals(false, result)
        }

    @Test
    fun `should mark event as processed`() =
        runTest {
            // Given
            val projectorName = "test-projector"
            val eventId = UUID.randomUUID()
            val tenantId = "tenant-123"

            every {
                jdbcTemplate.update(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                )
            } returns 1

            // When
            repository.markEventAsProcessed(projectorName, eventId, tenantId)

            // Then
            verify {
                jdbcTemplate.update(
                    any<String>(),
                    any<MapSqlParameterSource>(),
                )
            }
        }
}

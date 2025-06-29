package com.axians.eaf.eventsourcing.axon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Unit tests for GlobalSequenceTrackingToken enhanced features from story 4.1.2.
 *
 * Tests cover:
 * - Factory methods (initial(), of(), head())
 * - Utility methods (advance(), advanceTo())
 * - Core TrackingToken contract methods (covers(), upperBound(), lowerBound())
 * - Serialization compatibility with Axon 4.11.2 TokenStore
 * - Edge cases and boundary conditions
 */
class GlobalSequenceTrackingTokenTest {
    // ============================================================================
    // Factory Methods Testing
    // ============================================================================

    @Test
    fun `initial factory method should create token with sequence 0`() {
        // When
        val token = GlobalSequenceTrackingToken.initial()

        // Then
        assertEquals(0L, token.globalSequence)
    }

    @Test
    fun `of factory method should create token with specified sequence`() {
        // Given
        val sequence = 42L

        // When
        val token = GlobalSequenceTrackingToken.of(sequence)

        // Then
        assertEquals(sequence, token.globalSequence)
    }

    @Test
    fun `head factory method should create token with max sequence`() {
        // Given
        val maxSequence = 999L

        // When
        val token = GlobalSequenceTrackingToken.head(maxSequence)

        // Then
        assertEquals(maxSequence, token.globalSequence)
    }

    @Test
    fun `factory methods should create equal tokens for same sequence`() {
        // Given
        val sequence = 123L

        // When
        val token1 = GlobalSequenceTrackingToken.of(sequence)
        val token2 = GlobalSequenceTrackingToken(sequence)

        // Then
        assertEquals(token1, token2)
        assertEquals(token1.hashCode(), token2.hashCode())
    }

    // ============================================================================
    // Utility Methods Testing
    // ============================================================================

    @Test
    fun `advance should increment sequence by 1`() {
        // Given
        val originalToken = GlobalSequenceTrackingToken.of(10L)

        // When
        val advancedToken = originalToken.advance()

        // Then
        assertEquals(11L, advancedToken.globalSequence)
        assertEquals(10L, originalToken.globalSequence) // Original unchanged
    }

    @Test
    fun `advanceTo should set sequence to higher value`() {
        // Given
        val originalToken = GlobalSequenceTrackingToken.of(10L)

        // When
        val advancedToken = originalToken.advanceTo(20L)

        // Then
        assertEquals(20L, advancedToken.globalSequence)
        assertEquals(10L, originalToken.globalSequence) // Original unchanged
    }

    @Test
    fun `advanceTo should keep current sequence if target is lower`() {
        // Given
        val originalToken = GlobalSequenceTrackingToken.of(20L)

        // When
        val advancedToken = originalToken.advanceTo(10L)

        // Then
        assertEquals(20L, advancedToken.globalSequence) // max(20, 10) = 20
    }

    @Test
    fun `advanceTo with same sequence should return equal token`() {
        // Given
        val originalToken = GlobalSequenceTrackingToken.of(15L)

        // When
        val advancedToken = originalToken.advanceTo(15L)

        // Then
        assertEquals(originalToken.globalSequence, advancedToken.globalSequence)
        assertEquals(originalToken, advancedToken)
    }

    // ============================================================================
    // TrackingToken Contract Methods Testing
    // ============================================================================

    @Test
    fun `covers should return true for null token`() {
        // Given
        val token = GlobalSequenceTrackingToken.of(10L)

        // When/Then
        assertTrue(token.covers(null))
    }

    @Test
    fun `covers should return true when sequence is greater than or equal`() {
        // Given
        val token1 = GlobalSequenceTrackingToken.of(10L)
        val token2 = GlobalSequenceTrackingToken.of(5L)
        val token3 = GlobalSequenceTrackingToken.of(10L)

        // When/Then
        assertTrue(token1.covers(token2)) // 10 >= 5
        assertTrue(token1.covers(token3)) // 10 >= 10
    }

    @Test
    fun `covers should return false when sequence is lower`() {
        // Given
        val token1 = GlobalSequenceTrackingToken.of(5L)
        val token2 = GlobalSequenceTrackingToken.of(10L)

        // When/Then
        assertFalse(token1.covers(token2)) // 5 < 10
    }

    @Test
    fun `covers should return true for non-GlobalSequenceTrackingToken`() {
        // Given
        val token = GlobalSequenceTrackingToken.of(10L)
        val otherToken =
            object : org.axonframework.eventhandling.TrackingToken {
                override fun covers(other: org.axonframework.eventhandling.TrackingToken?): Boolean = false

                override fun upperBound(
                    other: org.axonframework.eventhandling.TrackingToken,
                ): org.axonframework.eventhandling.TrackingToken = this

                override fun lowerBound(
                    other: org.axonframework.eventhandling.TrackingToken,
                ): org.axonframework.eventhandling.TrackingToken = this
            }

        // When/Then
        assertTrue(token.covers(otherToken))
    }

    @Test
    fun `upperBound should return token with higher sequence`() {
        // Given
        val token1 = GlobalSequenceTrackingToken.of(10L)
        val token2 = GlobalSequenceTrackingToken.of(15L)

        // When
        val result = token1.upperBound(token2)

        // Then
        assertTrue(result is GlobalSequenceTrackingToken)
        assertEquals(15L, (result as GlobalSequenceTrackingToken).globalSequence)
    }

    @Test
    fun `upperBound should return current token when other is not GlobalSequenceTrackingToken`() {
        // Given
        val token = GlobalSequenceTrackingToken.of(10L)
        val otherToken =
            object : org.axonframework.eventhandling.TrackingToken {
                override fun covers(other: org.axonframework.eventhandling.TrackingToken?): Boolean = false

                override fun upperBound(
                    other: org.axonframework.eventhandling.TrackingToken,
                ): org.axonframework.eventhandling.TrackingToken = this

                override fun lowerBound(
                    other: org.axonframework.eventhandling.TrackingToken,
                ): org.axonframework.eventhandling.TrackingToken = this
            }

        // When
        val result = token.upperBound(otherToken)

        // Then
        assertEquals(token, result)
    }

    @Test
    fun `lowerBound should return token with lower sequence`() {
        // Given
        val token1 = GlobalSequenceTrackingToken.of(15L)
        val token2 = GlobalSequenceTrackingToken.of(10L)

        // When
        val result = token1.lowerBound(token2)

        // Then
        assertTrue(result is GlobalSequenceTrackingToken)
        assertEquals(10L, (result as GlobalSequenceTrackingToken).globalSequence)
    }

    @Test
    fun `lowerBound should return current token when other is not GlobalSequenceTrackingToken`() {
        // Given
        val token = GlobalSequenceTrackingToken.of(10L)
        val otherToken =
            object : org.axonframework.eventhandling.TrackingToken {
                override fun covers(other: org.axonframework.eventhandling.TrackingToken?): Boolean = false

                override fun upperBound(
                    other: org.axonframework.eventhandling.TrackingToken,
                ): org.axonframework.eventhandling.TrackingToken = this

                override fun lowerBound(
                    other: org.axonframework.eventhandling.TrackingToken,
                ): org.axonframework.eventhandling.TrackingToken = this
            }

        // When
        val result = token.lowerBound(otherToken)

        // Then
        assertEquals(token, result)
    }

    // ============================================================================
    // Serialization Testing (Axon 4.11.2 TokenStore Compatibility)
    // ============================================================================

    @Test
    fun `token should be serializable and deserializable`() {
        // Given
        val originalToken = GlobalSequenceTrackingToken.of(12345L)

        // When - serialize
        val serializedBytes =
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos -> oos.writeObject(originalToken) }
                baos.toByteArray()
            }

        // Then - deserialize
        val deserializedToken =
            ByteArrayInputStream(serializedBytes).use { bais ->
                ObjectInputStream(bais).use { ois ->
                    ois.readObject() as GlobalSequenceTrackingToken
                }
            }

        assertEquals(originalToken, deserializedToken)
        assertEquals(originalToken.globalSequence, deserializedToken.globalSequence)
    }

    @Test
    fun `serialization should preserve all token properties`() {
        // Given
        val tokens =
            listOf(
                GlobalSequenceTrackingToken.initial(),
                GlobalSequenceTrackingToken.of(1L),
                GlobalSequenceTrackingToken.of(Long.MAX_VALUE),
                GlobalSequenceTrackingToken.head(999999L),
            )

        tokens.forEach { originalToken ->
            // When - serialize and deserialize
            val serializedBytes =
                ByteArrayOutputStream().use { baos ->
                    ObjectOutputStream(baos).use { oos -> oos.writeObject(originalToken) }
                    baos.toByteArray()
                }

            val deserializedToken =
                ByteArrayInputStream(serializedBytes).use { bais ->
                    ObjectInputStream(bais).use { ois ->
                        ois.readObject() as GlobalSequenceTrackingToken
                    }
                }

            // Then
            assertEquals(originalToken, deserializedToken)
            assertEquals(originalToken.globalSequence, deserializedToken.globalSequence)
            assertEquals(originalToken.toString(), deserializedToken.toString())
        }
    }

    // ============================================================================
    // Edge Cases and Boundary Conditions
    // ============================================================================

    @Test
    fun `should handle edge case values`() {
        // Test boundary values
        val tokens =
            listOf(
                GlobalSequenceTrackingToken.of(0L),
                GlobalSequenceTrackingToken.of(1L),
                GlobalSequenceTrackingToken.of(Long.MAX_VALUE),
            )

        tokens.forEach { token ->
            // Should not throw
            assertNotEquals(null, token)
            assertTrue(token.globalSequence >= 0L)
        }
    }

    @Test
    fun `advance should handle Long MAX_VALUE gracefully`() {
        // Given
        val token = GlobalSequenceTrackingToken.of(Long.MAX_VALUE)

        // When - should not throw ArithmeticException
        val advanced = token.advance()

        // Then - wraps around or handles overflow gracefully
        assertNotEquals(null, advanced)
    }

    @Test
    fun `toString should provide readable representation`() {
        // Given
        val token = GlobalSequenceTrackingToken.of(42L)

        // When
        val stringRepresentation = token.toString()

        // Then
        assertTrue(stringRepresentation.contains("GlobalSequenceTrackingToken"))
        assertTrue(stringRepresentation.contains("42"))
    }

    @Test
    fun `data class equality should work correctly`() {
        // Given
        val token1 = GlobalSequenceTrackingToken.of(100L)
        val token2 = GlobalSequenceTrackingToken.of(100L)
        val token3 = GlobalSequenceTrackingToken.of(200L)

        // When/Then
        assertEquals(token1, token2)
        assertEquals(token1.hashCode(), token2.hashCode())
        assertNotEquals(token1, token3)
        assertNotEquals(token1.hashCode(), token3.hashCode())
    }
}

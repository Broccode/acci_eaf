package com.axians.eaf.eventsourcing.axon

import org.axonframework.eventhandling.TrackingToken
import java.io.Serializable

/**
 * A tracking token that uses the global sequence ID from the PostgreSQL event store.
 *
 * This is simpler than GapAwareTrackingToken as we rely on the database sequence to guarantee
 * ordering without gaps. The global_sequence_id is a BIGSERIAL column that provides a monotonically
 * increasing sequence across all events.
 */
data class GlobalSequenceTrackingToken(
    val globalSequence: Long,
) : TrackingToken,
    Serializable {
    override fun covers(other: TrackingToken?): Boolean =
        other == null ||
            (other is GlobalSequenceTrackingToken && globalSequence >= other.globalSequence)

    override fun upperBound(other: TrackingToken): TrackingToken =
        if (other is GlobalSequenceTrackingToken) {
            GlobalSequenceTrackingToken(maxOf(globalSequence, other.globalSequence))
        } else {
            this
        }

    override fun lowerBound(other: TrackingToken): TrackingToken =
        if (other is GlobalSequenceTrackingToken) {
            GlobalSequenceTrackingToken(minOf(globalSequence, other.globalSequence))
        } else {
            this
        }

    /** Advances this token to the next position. */
    fun advance(): GlobalSequenceTrackingToken = GlobalSequenceTrackingToken(globalSequence + 1)

    /** Advances this token to a specific sequence position. */
    fun advanceTo(sequence: Long): GlobalSequenceTrackingToken =
        GlobalSequenceTrackingToken(maxOf(globalSequence, sequence))

    companion object {
        /** Creates a token for the specified global sequence. */
        fun of(globalSequence: Long): GlobalSequenceTrackingToken = GlobalSequenceTrackingToken(globalSequence)

        /** Creates an initial token for starting from the beginning. */
        fun initial(): GlobalSequenceTrackingToken = GlobalSequenceTrackingToken(0)

        /** Creates a token representing the head position. */
        fun head(maxSequence: Long): GlobalSequenceTrackingToken = GlobalSequenceTrackingToken(maxSequence)
    }

    override fun toString(): String = "GlobalSequenceTrackingToken(globalSequence=$globalSequence)"
}

package com.axians.eaf.eventsourcing.axon.exception

/** Exception thrown when event serialization/deserialization fails. */
class EventSerializationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

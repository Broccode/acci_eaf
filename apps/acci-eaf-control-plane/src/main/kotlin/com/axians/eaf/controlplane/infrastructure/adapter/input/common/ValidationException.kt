package com.axians.eaf.controlplane.infrastructure.adapter.input.common

/** Exception thrown when input validation fails in endpoint operations. */
class ValidationException(
    message: String,
) : IllegalArgumentException(message)

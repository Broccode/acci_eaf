package com.axians.eaf.testservice.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Sample domain model for TestService service.
 * Replace this with your actual domain model.
 */
data class SampleTestService(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
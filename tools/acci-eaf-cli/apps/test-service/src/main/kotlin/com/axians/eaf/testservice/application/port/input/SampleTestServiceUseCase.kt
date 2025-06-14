package com.axians.eaf.testservice.application.port.input

import com.axians.eaf.testservice.domain.model.SampleTestService

/**
 * Use case interface for SampleTestService operations.
 * This represents the input port in hexagonal architecture.
 */
interface SampleTestServiceUseCase {

    /**
     * Find a sample by its ID.
     * @param id The ID to search for
     * @return The sample if found
     * @throws IllegalArgumentException if not found
     */
    fun findById(id: String): SampleTestService

    /**
     * Create a new sample.
     * @param name The name of the sample
     * @param description Optional description
     * @return The created sample
     */
    fun create(name: String, description: String? = null): SampleTestService
}
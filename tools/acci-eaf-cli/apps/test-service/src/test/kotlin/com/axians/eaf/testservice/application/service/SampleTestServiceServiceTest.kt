package com.axians.eaf.testservice.application.service

import com.axians.eaf.testservice.domain.model.SampleTestService
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test class for SampleTestServiceService.
 * This is an example of TDD - start with failing tests and implement the functionality.
 */
class SampleTestServiceServiceTest {

    private val service = SampleTestServiceService()

    @Test
    fun `should find sample by id`() {
        // Given
        val id = "test-id"

        // When
        val result = service.findById(id)

        // Then
        assertThat(result.id).isEqualTo(id)
        assertThat(result.name).isEqualTo("Sample TestService")
        assertThat(result.description).contains(id)
    }

    @Test
    fun `should create new sample`() {
        // Given
        val name = "Test Sample"
        val description = "Test Description"

        // When
        val result = service.create(name, description)

        // Then
        assertThat(result.name).isEqualTo(name)
        assertThat(result.description).isEqualTo(description)
        assertThat(result.id).isNotBlank()
    }

    // TODO: Add more tests as you implement the service
    // Examples:
    // - Test error cases (invalid input, not found, etc.)
    // - Test with mocked dependencies
    // - Test business logic validation
}
package com.axians.eaf.testservice.application.service

import com.axians.eaf.testservice.application.port.input.SampleTestServiceUseCase
import com.axians.eaf.testservice.domain.model.SampleTestService
import org.springframework.stereotype.Service

/**
 * Implementation of SampleTestServiceUseCase.
 * This is the application service that orchestrates domain logic.
 */
@Service
class SampleTestServiceService : SampleTestServiceUseCase {

    // TODO: Inject repository port (output adapter) when implementing persistence

    override fun findById(id: String): SampleTestService {
        // TODO: Replace with actual repository call
        return SampleTestService(
            id = id,
            name = "Sample TestService",
            description = "This is a sample TestService with ID: $id",
        )
    }

    override fun create(name: String, description: String?): SampleTestService {
        // TODO: Replace with actual repository call
        return SampleTestService(
            name = name,
            description = description,
        )
    }
}
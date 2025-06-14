package com.axians.eaf.testservice.infrastructure.adapter.input.web

import com.axians.eaf.testservice.application.port.input.SampleTestServiceUseCase
import com.axians.eaf.testservice.domain.model.SampleTestService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sample-testservice")
class SampleTestServiceController(
    private val sampleUseCase: SampleTestServiceUseCase,
) {

    @GetMapping("/{id}")
    fun getSample(@PathVariable id: String): ResponseEntity<SampleTestService> {
        val sample = sampleUseCase.findById(id)
        return ResponseEntity.ok(sample)
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "OK", "service" to "TestServiceService"))
    }
}
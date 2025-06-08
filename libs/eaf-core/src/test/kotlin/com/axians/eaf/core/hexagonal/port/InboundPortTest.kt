package com.axians.eaf.core.hexagonal.port

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InboundPortTest {
    @Test
    fun `should define InboundPort interface with generic types`() {
        // Given/When
        val portClass = InboundPort::class.java

        // Then
        assertNotNull(portClass)
        assertTrue(portClass.isInterface, "InboundPort should be an interface")

        // Verify generic type parameters
        val typeParameters = portClass.typeParameters
        assertTrue(typeParameters.size == 2, "InboundPort should have 2 generic type parameters")
        assertTrue(typeParameters[0].name == "C", "First type parameter should be 'C' (Command)")
        assertTrue(typeParameters[1].name == "R", "Second type parameter should be 'R' (Result)")
    }

    @Test
    fun `should allow concrete implementation of InboundPort`() {
        // Given
        data class TestCommand(
            val value: String,
        )

        data class TestResult(
            val processedValue: String,
        )

        class TestInboundPort : InboundPort<TestCommand, TestResult> {
            override fun handle(command: TestCommand): TestResult = TestResult("processed: ${command.value}")
        }

        // When
        val port = TestInboundPort()
        val result = port.handle(TestCommand("test"))

        // Then
        assertNotNull(result)
        assertTrue(result.processedValue == "processed: test")
    }
}

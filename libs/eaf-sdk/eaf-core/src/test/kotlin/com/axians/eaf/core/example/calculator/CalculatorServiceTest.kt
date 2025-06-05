package com.axians.eaf.core.example.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * TDD Example: Test-Driven Development demonstration for CalculatorService.
 * This test is written BEFORE the implementation to follow the red-green-refactor cycle.
 *
 * This serves as a reference implementation for EAF developers practicing TDD.
 */
class CalculatorServiceTest {
    private lateinit var calculatorService: CalculatorService

    @BeforeEach
    fun setUp() {
        calculatorService = CalculatorService()
    }

    @Test
    fun `should return sum of two positive numbers`() {
        // Given
        val a = 5
        val b = 3

        // When
        val result = calculatorService.add(a, b)

        // Then
        assertEquals(8, result)
    }

    @Test
    fun `should return sum of positive and negative numbers`() {
        // Given
        val a = 10
        val b = -3

        // When
        val result = calculatorService.add(a, b)

        // Then
        assertEquals(7, result)
    }

    @Test
    fun `should return zero when adding zero to any number`() {
        // Given
        val a = 42
        val b = 0

        // When
        val result = calculatorService.add(a, b)

        // Then
        assertEquals(42, result)
    }

    @Test
    fun `should handle large numbers correctly`() {
        // Given
        val a = Int.MAX_VALUE - 1
        val b = 1

        // When
        val result = calculatorService.add(a, b)

        // Then
        assertEquals(Int.MAX_VALUE, result)
    }

    @Test
    fun `should throw exception when addition would cause overflow`() {
        // Given
        val a = Int.MAX_VALUE
        val b = 1

        // When & Then
        assertThrows<ArithmeticException> {
            calculatorService.add(a, b)
        }
    }
}

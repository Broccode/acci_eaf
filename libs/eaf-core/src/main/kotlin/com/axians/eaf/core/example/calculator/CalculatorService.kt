package com.axians.eaf.core.example.calculator

/**
 * TDD Example: Simple Calculator Service demonstrating Test-Driven Development.
 * This implementation is created AFTER the test to follow the red-green-refactor cycle.
 *
 * This serves as a reference implementation for EAF developers practicing TDD.
 */
class CalculatorService {
    /**
     * Adds two integers with overflow protection.
     *
     * @param a First integer
     * @param b Second integer
     * @return Sum of a and b
     * @throws ArithmeticException if the addition would cause overflow
     */
    fun add(
        a: Int,
        b: Int,
    ): Int {
        // Check for overflow before performing addition
        if (a > 0 && b > Int.MAX_VALUE - a) {
            throw ArithmeticException("Integer overflow: $a + $b would exceed Int.MAX_VALUE")
        }
        if (a < 0 && b < Int.MIN_VALUE - a) {
            throw ArithmeticException("Integer underflow: $a + $b would be less than Int.MIN_VALUE")
        }

        return a + b
    }
}

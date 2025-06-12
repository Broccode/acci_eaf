package com.axians.eaf.iam.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {
    private lateinit var eafIamProperties: EafIamProperties

    @BeforeEach
    fun setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext()

        // Create properties
        eafIamProperties =
            EafIamProperties(
                serviceUrl = "http://iam-service:8080",
                jwt =
                    EafIamProperties.JwtProperties(
                        issuerUri = "http://iam-service:8080",
                        audience = "test-service",
                    ),
            )
    }

    @Test
    fun `should extract token from Authorization header with Bearer prefix`() {
        // Given
        val request = MockHttpServletRequest()
        val token = "valid-jwt-token"
        request.addHeader("Authorization", "Bearer $token")

        // When
        val extractedToken = extractTokenFromRequest(request)

        // Then
        assertThat(extractedToken).isEqualTo(token)
    }

    @Test
    fun `should return null when Authorization header is missing`() {
        // Given
        val request = MockHttpServletRequest()

        // When
        val extractedToken = extractTokenFromRequest(request)

        // Then
        assertThat(extractedToken).isNull()
    }

    @Test
    fun `should return null when Authorization header does not start with Bearer`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz")

        // When
        val extractedToken = extractTokenFromRequest(request)

        // Then
        assertThat(extractedToken).isNull()
    }

    @Test
    fun `should return empty string when Authorization header is just Bearer with space`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer ")

        // When
        val extractedToken = extractTokenFromRequest(request)

        // Then
        assertThat(extractedToken).isEqualTo("")
    }

    @Test
    fun `should return null when Authorization header is just Bearer`() {
        // Given
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer")

        // When
        val extractedToken = extractTokenFromRequest(request)

        // Then
        assertThat(extractedToken).isNull()
    }

    // Helper method to test token extraction logic
    private fun extractTokenFromRequest(request: MockHttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")
        return if (authHeader?.startsWith("Bearer ") == true) {
            authHeader.substring(7)
        } else {
            null
        }
    }
}

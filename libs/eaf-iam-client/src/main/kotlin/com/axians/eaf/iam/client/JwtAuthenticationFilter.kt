package com.axians.eaf.iam.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * JWT Authentication Filter for EAF services.
 * Validates JWT tokens by calling the IAM service introspection endpoint.
 */
class JwtAuthenticationFilter(
    private val iamServiceUrl: String,
    private val webClient: WebClient = WebClient.builder().build(),
    private val objectMapper: ObjectMapper =
        ObjectMapper().apply {
            findAndRegisterModules()
        },
) : OncePerRequestFilter() {
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val INTROSPECT_ENDPOINT = "/api/v1/auth/introspect"
        private val REQUEST_TIMEOUT = Duration.ofSeconds(5)
    }

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = extractToken(request)

        if (token != null) {
            try {
                val principal = validateTokenSync(token)
                if (principal != null) {
                    val authentication = EafAuthentication(principal, token)
                    SecurityContextHolder.getContext().authentication = authentication
                    logger.debug("Successfully authenticated user: ${principal.name} for tenant: ${principal.tenantId}")
                } else {
                    logger.warn("Token validation failed - token is invalid or expired")
                    handleUnauthorized(response)
                    return
                }
            } catch (e: Exception) {
                logger.error("Error during token validation", e)
                handleUnauthorized(response)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader(AUTHORIZATION_HEADER)
        return if (authHeader?.startsWith(BEARER_PREFIX) == true) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    private fun validateTokenSync(token: String): EafPrincipal? =
        try {
            webClient
                .post()
                .uri("$iamServiceUrl$INTROSPECT_ENDPOINT")
                .header(HttpHeaders.AUTHORIZATION, "$BEARER_PREFIX$token")
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(REQUEST_TIMEOUT)
                .map { responseBody ->
                    parseIntrospectionResponse(responseBody)
                }.onErrorResume { e ->
                    when (e) {
                        is WebClientResponseException -> {
                            if (e.statusCode == HttpStatus.UNAUTHORIZED || e.statusCode == HttpStatus.FORBIDDEN) {
                                logger.debug("Token validation failed: ${e.statusCode}")
                            } else {
                                logger.error("IAM service error during token validation: ${e.statusCode}", e)
                            }
                        }
                        else -> logger.error("Network error during token validation", e)
                    }
                    Mono.empty()
                }.block()
        } catch (e: Exception) {
            logger.error("Token validation failed", e)
            null
        }

    private fun parseIntrospectionResponse(responseBody: String): EafPrincipal? {
        return try {
            val response: Map<String, Any> = objectMapper.readValue(responseBody)

            // Check if token is active
            val active = response["active"] as? Boolean ?: false
            if (!active) {
                return null
            }

            val tenantId =
                response["tenant_id"] as? String
                    ?: throw IllegalArgumentException("Missing tenant_id in introspection response")

            val userId = response["user_id"] as? String
            val username = response["username"] as? String
            val email = response["email"] as? String

            @Suppress("UNCHECKED_CAST")
            val roles = (response["roles"] as? List<String>)?.toSet() ?: emptySet()

            @Suppress("UNCHECKED_CAST")
            val permissions = (response["permissions"] as? List<String>)?.toSet() ?: emptySet()

            EafPrincipal(
                tenantId = tenantId,
                userId = userId,
                username = username,
                email = email,
                roles = roles,
                permissions = permissions,
            )
        } catch (e: Exception) {
            logger.error("Failed to parse introspection response", e)
            null
        }
    }

    private fun handleUnauthorized(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write("""{"error": "Unauthorized", "message": "Invalid or missing JWT token"}""")
    }
}

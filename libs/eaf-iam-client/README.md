# EAF IAM Client SDK

The EAF IAM Client SDK provides seamless integration with the ACCI EAF Identity and Access Management (IAM) service for Spring Boot applications. It enables easy authentication and authorization using JWT tokens with tenant-scoped access control.

## Features

- **Auto-Configuration**: Automatically configures Spring Security when included as a dependency
- **JWT Token Validation**: Validates JWT tokens issued by the EAF IAM service via token introspection
- **Tenant Context**: Provides easy access to tenant ID and user context throughout the application
- **Role-Based Access Control (RBAC)**: Supports Spring Security's `@PreAuthorize` annotations with roles and permissions
- **Configurable Security**: Flexible security settings with customizable permit-all paths

## Quick Start

### 1. Add Dependency

Add the EAF IAM Client SDK to your Spring Boot application:

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":libs:eaf-iam-client"))
}
```

### 2. Configure Properties

Add the following configuration to your `application.yml`:

```yaml
eaf:
  iam:
    service-url: http://iam-service:8080  # Required: URL of the IAM service
    jwt:
      issuer-uri: http://iam-service:8080  # Optional: JWT issuer URI
      audience: my-eaf-service             # Optional: Expected audience claim
    security:
      enabled: true                        # Optional: Enable/disable security (default: true)
      permit-all-paths:                    # Optional: Paths that don't require authentication
        - /actuator/health
        - /actuator/info
        - /public/**
```

### 3. Secure Your Endpoints

Use Spring Security annotations to protect your endpoints:

```kotlin
@RestController
@RequestMapping("/api/v1")
class MyController {

    @GetMapping("/public")
    fun publicEndpoint(): String = "This is public"

    @GetMapping("/protected")
    @PreAuthorize("hasRole('PILOT_APP_USER')")
    fun protectedEndpoint(): String = "This requires authentication"

    @GetMapping("/admin")
    @PreAuthorize("hasRole('PILOT_APP_ADMIN')")
    fun adminEndpoint(): String = "This requires admin role"

    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): Map<String, Any?> {
        val eafAuth = authentication as EafAuthentication
        return mapOf(
            "tenantId" to eafAuth.getTenantId(),
            "userId" to eafAuth.getUserId(),
            "name" to eafAuth.name,
            "roles" to eafAuth.principal.roles,
            "permissions" to eafAuth.principal.permissions
        )
    }
}
```

### 4. Access Tenant Context

The SDK provides easy access to the current tenant and user context:

```kotlin
@Service
class MyService {

    fun doSomethingTenantSpecific(): String {
        val authentication = SecurityContextHolder.getContext().authentication as EafAuthentication
        val tenantId = authentication.getTenantId()
        val userId = authentication.getUserId()
        
        // Use tenant and user context for business logic
        return "Processing for tenant: $tenantId, user: $userId"
    }
}
```

## Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `eaf.iam.service-url` | String | Required | Base URL of the EAF IAM service |
| `eaf.iam.jwt.issuer-uri` | String | `http://localhost:8080` | Expected issuer URI in JWT tokens |
| `eaf.iam.jwt.audience` | String | `eaf-service` | Expected audience claim in JWT tokens |
| `eaf.iam.security.enabled` | Boolean | `true` | Enable/disable security features |
| `eaf.iam.security.permit-all-paths` | List<String> | `["/actuator/health", "/actuator/info"]` | Paths that don't require authentication |

## Authentication Flow

1. **Token Extraction**: The SDK extracts JWT tokens from the `Authorization: Bearer <token>` header
2. **Token Validation**: Validates the token by calling the IAM service's introspection endpoint (`/api/v1/auth/introspect`)
3. **Principal Creation**: Creates an `EafAuthentication` object containing user details, tenant ID, roles, and permissions
4. **Security Context**: Populates Spring Security's `SecurityContextHolder` for use throughout the request

## Role and Permission Mapping

The SDK automatically maps roles and permissions from JWT tokens to Spring Security authorities:

- **Roles**: Prefixed with `ROLE_` (e.g., `PILOT_APP_ADMIN` becomes `ROLE_PILOT_APP_ADMIN`)
- **Permissions**: Used as-is (e.g., `user:read`, `admin:delete`)

This allows you to use standard Spring Security expressions:

```kotlin
@PreAuthorize("hasRole('PILOT_APP_ADMIN')")           // Check for role
@PreAuthorize("hasAuthority('user:read')")           // Check for permission
@PreAuthorize("hasAnyRole('PILOT_APP_ADMIN', 'PILOT_APP_USER')") // Check for any role
```

## Error Handling

The SDK handles various error scenarios:

- **Missing Token**: Requests without `Authorization` header are allowed to proceed (may be rejected by endpoint security)
- **Invalid Token**: Returns HTTP 401 Unauthorized with JSON error response
- **IAM Service Unavailable**: Returns HTTP 401 Unauthorized (fails securely)
- **Malformed Response**: Returns HTTP 401 Unauthorized

## Testing

For testing, you can disable security or mock the authentication:

### Disable Security for Tests

```yaml
# application-test.yml
eaf:
  iam:
    security:
      enabled: false
```

### Mock Authentication in Tests

```kotlin
@Test
fun testWithMockAuth() {
    val principal = EafPrincipal(
        tenantId = "test-tenant",
        userId = "test-user",
        username = "testuser",
        email = "test@example.com",
        roles = setOf("PILOT_APP_USER"),
        permissions = setOf("user:read")
    )
    val authentication = EafAuthentication(principal)
    
    SecurityContextHolder.getContext().authentication = authentication
    
    // Your test code here
}
```

## Advanced Usage

### Custom Security Configuration

If you need to customize the security configuration beyond the provided properties, you can create your own `SecurityFilterChain` bean:

```kotlin
@Configuration
class CustomSecurityConfig {

    @Bean
    @Primary
    fun customSecurityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/custom/public/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
```

### Accessing Principal Details

The `EafPrincipal` class provides convenient methods for checking roles and permissions:

```kotlin
val principal = (authentication as EafAuthentication).principal

// Check individual roles/permissions
if (principal.hasRole("PILOT_APP_ADMIN")) {
    // Admin-specific logic
}

if (principal.hasPermission("user:write")) {
    // Write permission logic
}

// Check multiple roles/permissions
if (principal.hasAnyRole("ADMIN", "MODERATOR")) {
    // Admin or moderator logic
}

if (principal.hasAllRoles("USER", "VERIFIED")) {
    // Must have both roles
}
```

## Troubleshooting

### Common Issues

1. **Auto-configuration not working**: Ensure `eaf.iam.service-url` is configured
2. **401 Unauthorized**: Check that the IAM service is accessible and tokens are valid
3. **Role checks failing**: Verify that roles in JWT tokens match the expected format
4. **Tenant context missing**: Ensure the JWT token contains a valid `tenant_id` claim

### Debug Logging

Enable debug logging to troubleshoot issues:

```yaml
logging:
  level:
    com.axians.eaf.iam.client: DEBUG
```

## Dependencies

The SDK requires the following dependencies (automatically included):

- Spring Boot 3.x
- Spring Security 6.x
- Spring WebFlux (for WebClient)
- Jackson (for JSON processing)
- Nimbus JOSE JWT (for JWT processing)

## License

This library is part of the ACCI EAF project and follows the project's licensing terms.

# Spring Boot Integration Testing Setup

This guide provides patterns and templates for setting up robust integration tests in EAF Spring Boot services, based on lessons learned from the IAM service implementation.

## Overview

Integration tests in EAF services use:

- **Spring Boot Test** framework for application context loading
- **Testcontainers** for database isolation with PostgreSQL
- **JPA repositories** for persistence layer testing
- **Hexagonal architecture** patterns with proper port/adapter separation

## Quick Start Template

### 1. File Structure

For any EAF service, create this test structure:

```
src/test/kotlin/com/axians/eaf/{service}/
├── test/
│   └── Test{Service}Application.kt          # Dedicated test boot class
├── {Service}TestcontainerConfiguration.kt   # Testcontainer setup  
└── infrastructure/adapter/outbound/persistence/
    └── *PersistenceAdapterTest.kt           # Integration tests
```

### 2. Test Application Class

Create a dedicated Spring Boot application for tests in the `test` package:

```kotlin
package com.axians.eaf.{service}.test

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Dedicated Spring Boot application for integration tests.
 * 
 * Key differences from main application:
 * - Does NOT exclude DataSourceAutoConfiguration (allows @ServiceConnection)
 * - Refined component scanning to avoid conflicts with main app
 * - Located in separate package to prevent duplicate @SpringBootConfiguration detection
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.axians.eaf.{service}.application",
        "com.axians.eaf.{service}.domain", 
        "com.axians.eaf.{service}.infrastructure",
        "com.axians.eaf.{service}.web"
    ]
)
class Test{Service}Application
```

### 3. Testcontainer Configuration

```kotlin
package com.axians.eaf.{service}

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration
class {Service}TestcontainerConfiguration {
    
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test") 
            .withPassword("test")
    }
}
```

### 4. Integration Test Annotations

Use this standard annotation pattern for all integration tests:

```kotlin
@SpringBootTest(classes = [Test{Service}Application::class])
@Testcontainers
@ActiveProfiles("test")
@Transactional
@Import({Service}TestcontainerConfiguration::class, JpaConfig::class)
class SomePersistenceAdapterTest {
    // Test implementation
}
```

### 5. Test Properties Configuration

Update `src/test/resources/application-test.properties`:

```properties
# Test Configuration
debug=true

# Disable Docker Compose integration for tests (Testcontainers handles PostgreSQL)
spring.docker.compose.enabled=false

# JPA/Hibernate configuration for tests  
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Disable Flyway for tests (DDL auto-creation is used instead)
spring.flyway.enabled=false

# Disable security for simple tests
security.basic.enabled=false

# Test logging
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web=WARN
logging.level.com.axians.eaf=INFO
logging.level.org.springframework=DEBUG

# Testcontainers will override database configuration
```

## Architecture Principles

### Test Context Isolation

Integration tests should:

- ✅ Use dedicated test application class to avoid configuration conflicts
- ✅ Import only necessary configurations (`JpaConfig`, testcontainer config)
- ✅ Use `@Transactional` for automatic rollback between tests
- ✅ Clean up data in `@AfterEach` methods if needed

### Testcontainer Lifecycle

- Use `@ServiceConnection` for automatic connection management
- Container starts once per test class and reuses for multiple tests
- PostgreSQL schema is created/dropped automatically via `ddl-auto=create-drop`

### Repository Testing Patterns

```kotlin
@Test
fun `should save and retrieve entity correctly`() {
    // Given
    val entity = SomeEntity.create("test-data")
    
    // When
    val saved = repository.save(entity)
    
    // Then
    assertNotNull(saved.id)
    assertEquals("test-data", saved.someProperty)
    
    // Verify persistence
    val retrieved = repository.findById(saved.id!!)
    assertTrue(retrieved.isPresent)
    assertEquals(saved, retrieved.get())
}
```

## Common Issues & Solutions

### Issue: `NoSuchBeanDefinitionException: entityManagerFactory`

**Cause:** Test application inherits DataSource exclusions from main application

**Solution:**

- Use dedicated test application that allows `DataSourceAutoConfiguration`
- Add `@Import(JpaConfig::class)` to tests
- Ensure correct database dialect in test properties

### Issue: `Found multiple @SpringBootConfiguration`

**Cause:** Test and main application classes in same package

**Solution:**

- Move test application to separate `test` package
- Use refined component scanning to exclude main application

### Issue: `relation "table_name" does not exist`

**Cause:** Database dialect mismatch or DDL auto-creation disabled

**Solution:**

- Use `PostgreSQLDialect` in test properties (not H2Dialect)
- Ensure `hibernate.ddl-auto=create-drop`
- Disable Flyway in tests: `spring.flyway.enabled=false`

## Best Practices

### Test Data Management

```kotlin
class SomePersistenceAdapterTest {
    
    @Autowired
    private lateinit var repository: SomeJpaRepository
    
    @AfterEach
    fun cleanup() {
        repository.deleteAll()
        // Clean up related tables in correct order
    }
}
```

### Testing Business Logic

Focus integration tests on:

- ✅ **Persistence behavior** (save, find, delete operations)
- ✅ **Transaction boundaries** (rollback scenarios)
- ✅ **Database constraints** (unique constraints, foreign keys)
- ✅ **Query correctness** (custom repository methods)

Avoid testing:

- ❌ **Business logic** (belongs in unit tests)
- ❌ **Validation logic** (belongs in unit tests)
- ❌ **External API calls** (use mocks instead)

### Performance Considerations

- Keep integration tests focused and fast
- Use `@Transactional` for automatic cleanup
- Consider `@Sql` annotations for complex test data setup
- Run integration tests in separate Gradle task if needed

## Troubleshooting Checklist

When integration tests fail:

1. **Check application context loading**
   - Verify test application class is in separate package
   - Ensure required configurations are imported
   - Check for conflicting auto-configurations

2. **Verify database setup**
   - Confirm PostgreSQL dialect in test properties
   - Check DDL auto-creation is enabled
   - Verify Testcontainer is starting successfully

3. **Examine dependency injection**
   - Ensure JPA repositories are available as beans
   - Check entity scanning configuration
   - Verify transaction manager setup

4. **Debug configuration**
   - Enable `debug=true` in test properties
   - Review auto-configuration report
   - Check Spring Boot startup logs

## Related Documentation

- [NATS Integration Testing](./nats-integration-testing.md) - For event-driven testing patterns
- [Security Context Access](./security-context-access.md) - For authentication in tests
- [Architecture Decision Records](../adr/) - For architectural context

## References

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers Documentation](https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/)

:::info Troubleshooting Reference
This guide is based on real troubleshooting experience from the IAM service implementation. The original detailed troubleshooting log is available in `docs/troubleshooting/iam-integration-test-solution-attempts.md` for historical reference.
:::

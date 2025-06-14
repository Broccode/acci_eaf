package com.axians.eaf.cli.template

class TemplateEngine {
    fun generateBuildGradle(serviceName: String): String =
        """
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // EAF Core dependencies
    implementation(project(":libs:eaf-core"))
    implementation(project(":libs:eaf-eventing-sdk"))
    implementation(project(":libs:eaf-eventsourcing-sdk"))

    runtimeOnly("org.postgresql:postgresql")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:${'$'}{rootProject.extra["junitVersion"]}")
    testImplementation("io.mockk:mockk:${'$'}{rootProject.extra["mockkVersion"]}")
    testImplementation("org.assertj:assertj-core:${'$'}{rootProject.extra["assertjVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.testcontainers:junit-jupiter:${'$'}{rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${'$'}{rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("com.ninja-squad:springmockk:${'$'}{rootProject.extra["springMockkVersion"]}")
    testImplementation("com.h2database:h2")

    // ArchUnit for architectural testing
    testImplementation("com.tngtech.archunit:archunit-junit5:${'$'}{rootProject.extra["archunitVersion"]}")
}
        """.trimIndent()

    fun generateProjectJson(
        serviceName: String,
        targetPath: String,
    ): String =
        """
{
  "name": "$serviceName",
  "projectType": "application",
  "sourceRoot": "$targetPath/$serviceName/src",
  "targets": {
    "build": {
      "executor": "@nx/gradle:gradle",
      "options": {
        "taskName": "build"
      }
    },
    "test": {
      "executor": "@nx/gradle:gradle",
      "options": {
        "taskName": "test"
      }
    },
    "run": {
      "executor": "@nx/gradle:gradle",
      "options": {
        "taskName": "bootRun"
      }
    },
    "clean": {
      "executor": "@nx/gradle:gradle",
      "options": {
        "taskName": "clean"
      }
    }
  },
  "tags": ["type:app", "scope:${serviceName.replace("-", "")}", "platform:backend"]
}
        """.trimIndent()

    fun generateApplication(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(basePackages = ["$packageName"])
class ${className}ServiceApplication

fun main(args: Array<String>) {
    runApplication<${className}ServiceApplication>(*args)
}
        """.trimIndent()

    fun generateSampleController(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName.infrastructure.adapter.input.web

import $packageName.application.port.input.Sample${className}UseCase
import $packageName.domain.model.Sample$className
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sample-${className.lowercase()}")
class Sample${className}Controller(
    private val sampleUseCase: Sample${className}UseCase,
) {

    @GetMapping("/{id}")
    fun getSample(@PathVariable id: String): ResponseEntity<Sample$className> {
        val sample = sampleUseCase.findById(id)
        return ResponseEntity.ok(sample)
    }

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "OK", "service" to "${className}Service"))
    }
}
        """.trimIndent()

    fun generateSampleDomainModel(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Sample domain model for $className service.
 * Replace this with your actual domain model.
 */
data class Sample$className(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
        """.trimIndent()

    fun generateSampleServiceInterface(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName.application.port.input

import $packageName.domain.model.Sample$className

/**
 * Use case interface for Sample$className operations.
 * This represents the input port in hexagonal architecture.
 */
interface Sample${className}UseCase {

    /**
     * Find a sample by its ID.
     * @param id The ID to search for
     * @return The sample if found
     * @throws IllegalArgumentException if not found
     */
    fun findById(id: String): Sample$className

    /**
     * Create a new sample.
     * @param name The name of the sample
     * @param description Optional description
     * @return The created sample
     */
    fun create(name: String, description: String? = null): Sample$className
}
        """.trimIndent()

    fun generateSampleServiceImpl(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName.application.service

import $packageName.application.port.input.Sample${className}UseCase
import $packageName.domain.model.Sample$className
import org.springframework.stereotype.Service

/**
 * Implementation of Sample${className}UseCase.
 * This is the application service that orchestrates domain logic.
 */
@Service
class Sample${className}Service : Sample${className}UseCase {

    // TODO: Inject repository port (output adapter) when implementing persistence

    override fun findById(id: String): Sample$className {
        // TODO: Replace with actual repository call
        return Sample$className(
            id = id,
            name = "Sample $className",
            description = "This is a sample $className with ID: ${'$'}id",
        )
    }

    override fun create(name: String, description: String?): Sample$className {
        // TODO: Replace with actual repository call
        return Sample$className(
            name = name,
            description = description,
        )
    }
}
        """.trimIndent()

    fun generateArchitectureTest(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests to enforce hexagonal architecture rules.
 * Uncomment and customize the rules as needed.
 */
class ArchitectureTest {

    private val importedClasses = ClassFileImporter().importPackages("$packageName")

    @Test
    fun `domain layer should not depend on infrastructure layer`() {
        classes()
            .that().resideInAPackage("$packageName.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "$packageName.domain..",
                "java..",
                "kotlin..",
                "org.jetbrains.annotations.."
            )
            .check(importedClasses)
    }

    @Test
    fun `application layer should not depend on infrastructure layer`() {
        classes()
            .that().resideInAPackage("$packageName.application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "$packageName.domain..",
                "$packageName.application..",
                "java..",
                "kotlin..",
                "org.springframework.stereotype..",
                "org.jetbrains.annotations.."
            )
            .check(importedClasses)
    }

    // TODO: Add more architectural rules as needed
    // Examples:
    // - Controllers should only be in web package
    // - Repositories should only be in persistence package
    // - Services should be annotated with @Service
    // - Domain objects should not have Spring annotations
}
        """.trimIndent()

    fun generateSampleServiceTest(
        packageName: String,
        className: String,
    ): String =
        """
package $packageName.application.service

import $packageName.domain.model.Sample$className
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test class for Sample${className}Service.
 * This is an example of TDD - start with failing tests and implement the functionality.
 */
class Sample${className}ServiceTest {

    private val service = Sample${className}Service()

    @Test
    fun `should find sample by id`() {
        // Given
        val id = "test-id"

        // When
        val result = service.findById(id)

        // Then
        assertThat(result.id).isEqualTo(id)
        assertThat(result.name).isEqualTo("Sample $className")
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
        """.trimIndent()

    fun generateApplicationYml(serviceName: String): String =
        """
# Application configuration for $serviceName service
spring:
  application:
    name: $serviceName

# --- EAF Local Development Configuration ---
# NATS Connection (values from EAF standard docker-compose)
eaf:
  eventing:
    nats:
      url: "nats://localhost:4222"
      # tenant-credentials-path: ... (configure as needed)

# PostgreSQL Connection (values from EAF standard docker-compose)
# Uncomment and configure when implementing persistence
# spring:
#   datasource:
#     url: jdbc:postgresql://localhost:5432/eaf_db
#     username: postgres
#     password: password
#   jpa:
#     hibernate:
#       ddl-auto: validate
#     show-sql: false
#     properties:
#       hibernate:
#         dialect: org.hibernate.dialect.PostgreSQLDialect
#         format_sql: true

# Server configuration
server:
  port: 8080

# Logging configuration
logging:
  level:
    com.axians.eaf: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
        """.trimIndent()
}

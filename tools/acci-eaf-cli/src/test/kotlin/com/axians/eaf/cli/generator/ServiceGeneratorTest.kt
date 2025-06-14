package com.axians.eaf.cli.generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ServiceGeneratorTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var generator: ServiceGenerator

    @BeforeEach
    fun setUp() {
        generator = ServiceGenerator(tempDir)

        // Create a basic settings.gradle.kts for testing
        File(tempDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"

            // Include backend applications
            include(":apps:existing-service")

            // Include tools
            // include(":tools:acci-eaf-cli")
            """.trimIndent(),
        )
    }

    @Test
    fun `should generate service with correct directory structure`() {
        // When
        generator.generateService("test-service", "apps")

        // Then
        val serviceDir = File(tempDir, "apps/test-service")
        assertThat(serviceDir).exists()

        // Check main directory structure
        assertThat(File(serviceDir, "src/main/kotlin/com/axians/eaf/testservice")).exists()
        assertThat(File(serviceDir, "src/main/kotlin/com/axians/eaf/testservice/application")).exists()
        assertThat(File(serviceDir, "src/main/kotlin/com/axians/eaf/testservice/domain")).exists()
        assertThat(File(serviceDir, "src/main/kotlin/com/axians/eaf/testservice/infrastructure")).exists()

        // Check test directory structure
        assertThat(File(serviceDir, "src/test/kotlin/com/axians/eaf/testservice")).exists()
    }

    @Test
    fun `should generate build gradle with correct content`() {
        // When
        generator.generateService("user-service", "apps")

        // Then
        val buildGradle = File(tempDir, "apps/user-service/build.gradle.kts")
        assertThat(buildGradle).exists()

        val content = buildGradle.readText()
        assertThat(content).contains("kotlin(\"jvm\")")
        assertThat(content).contains("org.springframework.boot")
        assertThat(content).contains("implementation(project(\":libs:eaf-core\"))")
        assertThat(content).contains("testImplementation(\"io.mockk:mockk")
        assertThat(content).contains("testImplementation(\"com.tngtech.archunit:archunit-junit5")
    }

    @Test
    fun `should generate application class with correct package`() {
        // When
        generator.generateService("notification-service", "apps")

        // Then
        val appClass =
            File(
                tempDir,
                "apps/notification-service/src/main/kotlin/com/axians/eaf/notificationservice/NotificationServiceApplication.kt",
            )
        assertThat(appClass).exists()

        val content = appClass.readText()
        assertThat(content).contains("package com.axians.eaf.notificationservice")
        assertThat(content).contains("class NotificationServiceApplication")
        assertThat(content).contains("@SpringBootApplication")
    }
}

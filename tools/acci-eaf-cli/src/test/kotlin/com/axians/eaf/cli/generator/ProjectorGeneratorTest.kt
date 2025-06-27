package com.axians.eaf.cli.generator

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectorGeneratorTest {
    @TempDir lateinit var tempDir: File

    private lateinit var generator: ProjectorGenerator

    @BeforeEach
    fun setUp() {
        generator = ProjectorGenerator(tempDir)

        // Create a basic settings.gradle.kts for testing
        File(tempDir, "settings.gradle.kts")
            .writeText(
                """
                rootProject.name = "test-project"
                include(":apps:test-service")
                """.trimIndent(),
            )

        // Create service directories for all tests
        createServiceDirectory("test-service")
        createServiceDirectory("user-management-service")
        createServiceDirectory("user-service")
        createServiceDirectory("notification-service")
        createServiceDirectory("payment-service")
        createServiceDirectory("analytics-service")
    }

    private fun createServiceDirectory(serviceName: String) {
        val serviceDir = File(tempDir, "apps/$serviceName")
        serviceDir.mkdirs()
        File(serviceDir, "build.gradle.kts").writeText("// test build file")

        // Create source directory structure
        val srcDir = File(serviceDir, "src/main/kotlin/com/axians/eaf/${serviceName.replace("-", "")}")
        srcDir.mkdirs()
    }

    @Test
    fun `should generate projector with correct directory structure and file`() {
        // When
        generator.generateProjector("UserReadModelProjector", "UserCreated", "test-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/infrastructure/adapter/input/messaging/UserReadModelProjector.kt",
            )
        assertThat(projectorFile).exists()
    }

    @Test
    fun `should generate projector with correct content and annotations`() {
        // When
        generator.generateProjector("OrderReportProjector", "OrderCompleted", "user-management-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/user-management-service/src/main/kotlin/com/axians/eaf/usermanagementservice/infrastructure/adapter/input/messaging/OrderReportProjector.kt",
            )
        val content = projectorFile.readText()

        assertThat(
            content,
        ).contains(
            "package com.axians.eaf.usermanagementservice.infrastructure.adapter.input.messaging",
        )
        assertThat(content).contains("@Component")
        assertThat(content).contains("class OrderReportProjector")
        assertThat(content)
            .contains("@NatsJetStreamListener(\"events.user.management.service.order_completed\")")
        assertThat(content).contains("fun handle(event: OrderCompleted)")
        assertThat(content)
            .contains("logger.info(\"Processing OrderCompleted for aggregate: {}\", event.aggregateId)")
    }

    @Test
    fun `should handle service names with hyphens correctly in NATS subject`() {
        // When
        generator.generateProjector("NotificationProjector", "EmailSent", "notification-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/notification-service/src/main/kotlin/com/axians/eaf/notificationservice/infrastructure/adapter/input/messaging/NotificationProjector.kt",
            )
        val content = projectorFile.readText()

        // Check that service name is converted from "notification-service" to "notification.service" in
        // NATS subject
        assertThat(content)
            .contains("@NatsJetStreamListener(\"events.notification.service.email_sent\")")
        assertThat(content).contains("fun handle(event: EmailSent)")
    }

    @Test
    fun `should convert PascalCase event names to snake_case in NATS subject`() {
        // When
        generator.generateProjector("UserActivityProjector", "UserProfileUpdated", "user-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/user-service/src/main/kotlin/com/axians/eaf/userservice/infrastructure/adapter/input/messaging/UserActivityProjector.kt",
            )
        val content = projectorFile.readText()

        // Check that event name is converted from "UserProfileUpdated" to "user_profile_updated"
        assertThat(content)
            .contains("@NatsJetStreamListener(\"events.user.service.user_profile_updated\")")
        assertThat(content).contains("fun handle(event: UserProfileUpdated)")
    }

    @Test
    fun `should include proper imports and logger setup`() {
        // When
        generator.generateProjector("PaymentProjector", "PaymentProcessed", "payment-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/payment-service/src/main/kotlin/com/axians/eaf/paymentservice/infrastructure/adapter/input/messaging/PaymentProjector.kt",
            )
        val content = projectorFile.readText()

        assertThat(content).contains("import com.axians.eaf.eventing.NatsJetStreamListener")
        assertThat(content).contains("import org.slf4j.LoggerFactory")
        assertThat(content).contains("import org.springframework.stereotype.Component")
        assertThat(content)
            .contains("private val logger = LoggerFactory.getLogger(PaymentProjector::class.java)")
    }

    @Test
    fun `should include TODO comments and example logic`() {
        // When
        generator.generateProjector("ReportProjector", "DataSynced", "analytics-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/analytics-service/src/main/kotlin/com/axians/eaf/analyticsservice/infrastructure/adapter/input/messaging/ReportProjector.kt",
            )
        val content = projectorFile.readText()

        assertThat(content).contains("// TODO: Implement projection logic here")
        assertThat(content).contains("// Examples:")
        assertThat(content).contains("// - Update read model in database")
        assertThat(content).contains("// - Send notification")
        assertThat(content).contains("// - Update search index")
        assertThat(content).contains("// - Generate reports")
        assertThat(content).contains("// TODO: Add more event handlers as needed")
    }

    @Test
    fun `should include snake_case conversion extension function`() {
        // When
        generator.generateProjector("TestProjector", "TestEvent", "test-service")

        // Then
        val projectorFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/infrastructure/adapter/input/messaging/TestProjector.kt",
            )
        val content = projectorFile.readText()

        assertThat(content).contains("private fun String.toSnakeCase(): String")
        assertThat(content).contains("this.replace(Regex(\"([a-z])([A-Z])\"), \"$1_$2\").lowercase()")
    }

    @Test
    fun `should throw exception when service directory does not exist`() {
        // When & Then
        assertThatThrownBy {
            generator.generateProjector("TestProjector", "TestEvent", "non-existent-service")
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Service directory does not exist")
    }
}

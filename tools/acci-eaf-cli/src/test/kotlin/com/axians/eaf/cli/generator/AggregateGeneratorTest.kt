package com.axians.eaf.cli.generator

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AggregateGeneratorTest {
    @TempDir lateinit var tempDir: File

    private lateinit var generator: AggregateGenerator

    @BeforeEach
    fun setUp() {
        generator = AggregateGenerator(tempDir)

        // Create a basic settings.gradle.kts for testing
        File(tempDir, "settings.gradle.kts")
            .writeText(
                """
                rootProject.name = "test-project"
                include(":apps:test-service")
                """.trimIndent(),
            )

        // Create a basic service structure
        val serviceDir = File(tempDir, "apps/test-service")
        serviceDir.mkdirs()
        File(serviceDir, "build.gradle.kts").writeText("// test build file")
    }

    @Test
    fun `should generate aggregate with correct directory structure and files`() {
        // When
        generator.generateAggregate("User", "test-service")

        // Then
        val serviceDir = File(tempDir, "apps/test-service")

        // Check that command file is created
        val commandFile =
            File(
                serviceDir,
                "src/main/kotlin/com/axians/eaf/testservice/domain/command/CreateUserCommand.kt",
            )
        assertThat(commandFile).exists()

        // Check that event file is created
        val eventFile =
            File(
                serviceDir,
                "src/main/kotlin/com/axians/eaf/testservice/domain/event/UserCreatedEvent.kt",
            )
        assertThat(eventFile).exists()

        // Check that aggregate file is created
        val aggregateFile =
            File(serviceDir, "src/main/kotlin/com/axians/eaf/testservice/domain/model/User.kt")
        assertThat(aggregateFile).exists()

        // Check that test file is created
        val testFile =
            File(serviceDir, "src/test/kotlin/com/axians/eaf/testservice/domain/model/UserTest.kt")
        assertThat(testFile).exists()
    }

    @Test
    fun `should generate command with correct content`() {
        // When
        generator.generateAggregate("Product", "test-service")

        // Then
        val commandFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/command/CreateProductCommand.kt",
            )
        val content = commandFile.readText()

        assertThat(content).contains("package com.axians.eaf.testservice.domain.command")
        assertThat(content).contains("data class CreateProductCommand")
        assertThat(content).contains("val id: String = UUID.randomUUID().toString()")
        assertThat(content).contains("val name: String")
        assertThat(content).contains("val description: String? = null")
    }

    @Test
    fun `should generate event with correct content`() {
        // When
        generator.generateAggregate("Order", "test-service")

        // Then
        val eventFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/event/OrderCreatedEvent.kt",
            )
        val content = eventFile.readText()

        assertThat(content).contains("package com.axians.eaf.testservice.domain.event")
        assertThat(content).contains("data class OrderCreatedEvent")
        assertThat(content).contains("val id: String")
        assertThat(content).contains("val name: String")
        assertThat(content).contains("val createdAt: Instant = Instant.now()")
    }

    @Test
    fun `should generate aggregate with correct content and annotations`() {
        // When
        generator.generateAggregate("Customer", "test-service")

        // Then
        val aggregateFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/model/Customer.kt",
            )
        val content = aggregateFile.readText()

        assertThat(content).contains("package com.axians.eaf.testservice.domain.model")
        assertThat(content).contains("@EafAggregate")
        assertThat(content).contains("class Customer : AbstractAggregateRoot<Customer>()")
        assertThat(content).contains("@AggregateIdentifier")
        assertThat(content).contains("@EafCommandHandler")
        assertThat(content).contains("constructor(command: CreateCustomerCommand)")
        assertThat(content).contains("@EafEventSourcingHandler")
        assertThat(content).contains("fun on(event: CustomerCreatedEvent)")
    }

    @Test
    fun `should generate test with correct content`() {
        // When
        generator.generateAggregate("Account", "test-service")

        // Then
        val testFile =
            File(
                tempDir,
                "apps/test-service/src/test/kotlin/com/axians/eaf/testservice/domain/model/AccountTest.kt",
            )
        val content = testFile.readText()

        assertThat(content).contains("package com.axians.eaf.testservice.domain.model")
        assertThat(content).contains("class AccountTest")
        assertThat(content).contains("@Test")
        assertThat(content).contains("should create Account when valid command is given")
        assertThat(content).contains("should apply AccountCreatedEvent correctly")
        assertThat(content).contains("CreateAccountCommand")
        assertThat(content).contains("AccountCreatedEvent")
    }

    @Test
    fun `should throw exception when service directory does not exist`() {
        // When & Then
        assertThatThrownBy { generator.generateAggregate("User", "non-existent-service") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Service directory does not exist")
    }
}

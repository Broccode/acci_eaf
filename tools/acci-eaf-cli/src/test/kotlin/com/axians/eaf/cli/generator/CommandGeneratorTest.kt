package com.axians.eaf.cli.generator

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CommandGeneratorTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var generator: CommandGenerator

    @BeforeEach
    fun setUp() {
        generator = CommandGenerator(tempDir)

        // Create a basic settings.gradle.kts for testing
        File(tempDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"
            include(":apps:test-service")
            """.trimIndent(),
        )

        // Create a basic service structure
        val serviceDir = File(tempDir, "apps/test-service")
        serviceDir.mkdirs()
        File(serviceDir, "build.gradle.kts").writeText("// test build file")

        // Create a basic aggregate file for testing
        val aggregateDir = File(serviceDir, "src/main/kotlin/com/axians/eaf/testservice/domain/model")
        aggregateDir.mkdirs()
        val aggregateFile = File(aggregateDir, "User.kt")
        aggregateFile.writeText(
            """
            package com.axians.eaf.testservice.domain.model

            import com.axians.eaf.eventsourcing.AbstractAggregateRoot
            import com.axians.eaf.eventsourcing.EafAggregate
            import com.axians.eaf.eventsourcing.EafCommandHandler
            import com.axians.eaf.eventsourcing.EafEventSourcingHandler
            import org.axonframework.modelling.command.AggregateIdentifier

            @EafAggregate
            class User : AbstractAggregateRoot<User>() {

                @AggregateIdentifier
                lateinit var id: String
                    private set

                lateinit var name: String
                    private set

                // Required by framework
                constructor()

                @EafCommandHandler
                constructor(command: CreateUserCommand) {
                    applyEvent(UserCreatedEvent(id = command.id, name = command.name))
                }

                @EafEventSourcingHandler
                fun on(event: UserCreatedEvent) {
                    this.id = event.id
                    this.name = event.name
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should generate command file with correct content`() {
        // When
        generator.generateCommand("UpdateUser", "User", "test-service")

        // Then
        val commandFile =
            File(tempDir, "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/command/UpdateUser.kt")
        assertThat(commandFile).exists()

        val content = commandFile.readText()
        assertThat(content).contains("package com.axians.eaf.testservice.domain.command")
        assertThat(content).contains("data class UpdateUser")
        assertThat(content).contains("val aggregateId: String")
        assertThat(content).contains("// TODO: Add command-specific fields here")
    }

    @Test
    fun `should add command handler to existing aggregate`() {
        // When
        generator.generateCommand("DeactivateUser", "User", "test-service")

        // Then
        val aggregateFile =
            File(tempDir, "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/model/User.kt")
        val content = aggregateFile.readText()

        // Check that import was added
        assertThat(content).contains("import com.axians.eaf.testservice.domain.command.DeactivateUser")

        // Check that command handler method was added
        assertThat(content).contains("@EafCommandHandler")
        assertThat(content).contains("fun handle(command: DeactivateUser)")
        assertThat(content).contains("// TODO: Validate business rules")
        assertThat(content).contains("// TODO: Apply appropriate event(s)")
    }

    @Test
    fun `should not duplicate import if already exists`() {
        // Given - generate first command
        generator.generateCommand("UpdateUser", "User", "test-service")

        // When - generate second command with same import pattern
        generator.generateCommand("ActivateUser", "User", "test-service")

        // Then
        val aggregateFile =
            File(tempDir, "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/model/User.kt")
        val content = aggregateFile.readText()

        // Should have both command handlers
        assertThat(content).contains("fun handle(command: UpdateUser)")
        assertThat(content).contains("fun handle(command: ActivateUser)")

        // Should have both imports
        assertThat(content).contains("import com.axians.eaf.testservice.domain.command.UpdateUser")
        assertThat(content).contains("import com.axians.eaf.testservice.domain.command.ActivateUser")
    }

    @Test
    fun `should throw exception when service directory does not exist`() {
        // When & Then
        assertThatThrownBy {
            generator.generateCommand("UpdateUser", "User", "non-existent-service")
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Service directory does not exist")
    }

    @Test
    fun `should throw exception when aggregate file does not exist`() {
        // When & Then
        assertThatThrownBy {
            generator.generateCommand("UpdateNonExistent", "NonExistentAggregate", "test-service")
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Aggregate file does not exist")
    }
}

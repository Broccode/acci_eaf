package com.axians.eaf.cli.generator

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EventGeneratorTest {
    @TempDir lateinit var tempDir: File

    private lateinit var generator: EventGenerator

    @BeforeEach
    fun setUp() {
        generator = EventGenerator(tempDir)

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
    fun `should generate event file with correct content`() {
        // When
        generator.generateEvent("UserUpdated", "User", "test-service")

        // Then
        val eventFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/event/UserUpdated.kt",
            )
        assertThat(eventFile).exists()

        val content = eventFile.readText()
        assertThat(content).contains("package com.axians.eaf.testservice.domain.event")
        assertThat(content).contains("data class UserUpdated")
        assertThat(content).contains("val aggregateId: String")
        assertThat(content).contains("val occurredAt: Instant = Instant.now()")
        assertThat(content).contains("// TODO: Add event-specific fields here")
    }

    @Test
    fun `should add event sourcing handler to existing aggregate`() {
        // When
        generator.generateEvent("UserDeactivated", "User", "test-service")

        // Then
        val aggregateFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/model/User.kt",
            )
        val content = aggregateFile.readText()

        // Check that import was added
        assertThat(content).contains("import com.axians.eaf.testservice.domain.event.UserDeactivated")

        // Check that event sourcing handler method was added
        assertThat(content).contains("@EafEventSourcingHandler")
        assertThat(content).contains("fun on(event: UserDeactivated)")
        assertThat(content).contains("// TODO: Apply state changes based on the event")
        assertThat(content).contains("this.updatedAt = event.occurredAt")
    }

    @Test
    fun `should not duplicate import if already exists`() {
        // Given - generate first event
        generator.generateEvent("UserUpdated", "User", "test-service")

        // When - generate second event with same import pattern
        generator.generateEvent("UserActivated", "User", "test-service")

        // Then
        val aggregateFile =
            File(
                tempDir,
                "apps/test-service/src/main/kotlin/com/axians/eaf/testservice/domain/model/User.kt",
            )
        val content = aggregateFile.readText()

        // Should have both event sourcing handlers
        assertThat(content).contains("fun on(event: UserUpdated)")
        assertThat(content).contains("fun on(event: UserActivated)")

        // Should have both imports
        assertThat(content).contains("import com.axians.eaf.testservice.domain.event.UserUpdated")
        assertThat(content).contains("import com.axians.eaf.testservice.domain.event.UserActivated")
    }

    @Test
    fun `should throw exception when service directory does not exist`() {
        // When & Then
        assertThatThrownBy { generator.generateEvent("UserUpdated", "User", "non-existent-service") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Service directory does not exist")
    }

    @Test
    fun `should throw exception when aggregate file does not exist`() {
        // When & Then
        assertThatThrownBy {
            generator.generateEvent("NonExistentUpdated", "NonExistentAggregate", "test-service")
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Aggregate file does not exist")
    }
}

package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.ProjectorGenerator
import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "projector",
    description = ["Generate a new Projector with NATS event handler for the specified event"],
    mixinStandardHelpOptions = true,
)
class GenerateProjectorCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = [
            "The name of the projector to generate (e.g., 'UserReadModelProjector', 'OrderReportProjector')",
        ],
    )
    lateinit var projectorName: String

    @CommandLine.Option(
        names = ["--service"],
        description = ["The target service name (e.g., 'user-management-service')"],
        required = true,
    )
    lateinit var serviceName: String

    @CommandLine.Option(
        names = ["--event"],
        description = ["The event name to handle (e.g., 'UserCreated', 'OrderCompleted')"],
        required = true,
    )
    lateinit var eventName: String

    override fun run() {
        try {
            validateInputs()

            val currentDir = File(System.getProperty("user.dir"))
            val generator = ProjectorGenerator(currentDir)

            println("Generating projector '$projectorName' for event '$eventName' in service '$serviceName'...")
            generator.generateProjector(projectorName, eventName, serviceName)
            println("✅ Projector '$projectorName' generated successfully!")
        } catch (e: Exception) {
            println("❌ Error generating projector: ${e.message}")
            exitProcess(1)
        }
    }

    private fun validateInputs() {
        if (projectorName.isBlank()) {
            throw IllegalArgumentException("Projector name cannot be blank")
        }

        if (!projectorName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw IllegalArgumentException(
                "Projector name must be PascalCase and start with a capital letter (e.g., 'UserReadModelProjector', 'OrderReportProjector')",
            )
        }

        if (eventName.isBlank()) {
            throw IllegalArgumentException("Event name cannot be blank")
        }

        if (!eventName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw IllegalArgumentException(
                "Event name must be PascalCase and start with a capital letter (e.g., 'UserCreated', 'OrderCompleted')",
            )
        }

        if (serviceName.isBlank()) {
            throw IllegalArgumentException("Service name cannot be blank")
        }

        if (!serviceName.matches(Regex("^[a-z][a-z0-9-]*[a-z0-9]$"))) {
            throw IllegalArgumentException(
                "Service name must be lowercase, start with a letter, and contain only letters, numbers, and hyphens",
            )
        }
    }
}

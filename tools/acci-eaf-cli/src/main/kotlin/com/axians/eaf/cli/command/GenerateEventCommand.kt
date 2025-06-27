package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.EventGenerator
import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "event",
    description = ["Generate a new Event and add handler method to an existing Aggregate"],
    mixinStandardHelpOptions = true,
)
class GenerateEventCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = ["The name of the event to generate (e.g., 'UserUpdated', 'OrderDeactivated')"],
    )
    lateinit var eventName: String

    @CommandLine.Option(
        names = ["--aggregate"],
        description = ["The target aggregate name (e.g., 'User', 'Order')"],
        required = true,
    )
    lateinit var aggregateName: String

    @CommandLine.Option(
        names = ["--service"],
        description = ["The target service name (e.g., 'user-management-service')"],
        required = true,
    )
    lateinit var serviceName: String

    override fun run() {
        try {
            validateInputs()

            val currentDir = File(System.getProperty("user.dir"))
            val generator = EventGenerator(currentDir)

            println(
                "Generating event '$eventName' for aggregate '$aggregateName' in service '$serviceName'...",
            )
            generator.generateEvent(eventName, aggregateName, serviceName)
            println("✅ Event '$eventName' generated successfully!")
        } catch (e: Exception) {
            println("❌ Error generating event: ${e.message}")
            exitProcess(1)
        }
    }

    private fun validateInputs() {
        if (eventName.isBlank()) {
            throw IllegalArgumentException("Event name cannot be blank")
        }

        if (!eventName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw IllegalArgumentException(
                "Event name must be PascalCase and start with a capital letter (e.g., 'UserUpdated', 'OrderDeactivated')",
            )
        }

        if (aggregateName.isBlank()) {
            throw IllegalArgumentException("Aggregate name cannot be blank")
        }

        if (!aggregateName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw IllegalArgumentException(
                "Aggregate name must be PascalCase and start with a capital letter (e.g., 'User', 'Order')",
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

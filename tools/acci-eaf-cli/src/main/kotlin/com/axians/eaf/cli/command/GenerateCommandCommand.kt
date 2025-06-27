package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.CommandGenerator
import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "command",
    description = ["Generate a new Command and add handler method to an existing Aggregate"],
    mixinStandardHelpOptions = true,
)
class GenerateCommandCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = ["The name of the command to generate (e.g., 'UpdateUser', 'DeactivateOrder')"],
    )
    lateinit var commandName: String

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
            val generator = CommandGenerator(currentDir)

            println(
                "Generating command '$commandName' for aggregate '$aggregateName' in service '$serviceName'...",
            )
            generator.generateCommand(commandName, aggregateName, serviceName)
            println("✅ Command '$commandName' generated successfully!")
        } catch (e: Exception) {
            println("❌ Error generating command: ${e.message}")
            exitProcess(1)
        }
    }

    private fun validateInputs() {
        if (commandName.isBlank()) {
            throw IllegalArgumentException("Command name cannot be blank")
        }

        if (!commandName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw IllegalArgumentException(
                "Command name must be PascalCase and start with a capital letter (e.g., 'UpdateUser', 'DeactivateOrder')",
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

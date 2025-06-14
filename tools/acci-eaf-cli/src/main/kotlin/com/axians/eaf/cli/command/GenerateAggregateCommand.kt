package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.AggregateGenerator
import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "aggregate",
    description = ["Generate a new Aggregate with creation Command, Event, and test files"],
    mixinStandardHelpOptions = true,
)
class GenerateAggregateCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = ["The name of the aggregate to generate (e.g., 'User', 'Order')"],
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
            val generator = AggregateGenerator(currentDir)

            println("Generating aggregate '$aggregateName' in service '$serviceName'...")
            generator.generateAggregate(aggregateName, serviceName)
            println("✅ Aggregate '$aggregateName' generated successfully!")
        } catch (e: Exception) {
            println("❌ Error generating aggregate: ${e.message}")
            exitProcess(1)
        }
    }

    private fun validateInputs() {
        if (aggregateName.isBlank()) {
            throw IllegalArgumentException("Aggregate name cannot be blank")
        }

        if (!aggregateName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
            throw IllegalArgumentException(
                "Aggregate name must be PascalCase and start with a capital letter (e.g., 'User', 'OrderItem')",
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

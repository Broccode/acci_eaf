package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.ServiceGenerator
import picocli.CommandLine
import java.io.File
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "service",
    description = ["Generate a new Kotlin/Spring backend service with hexagonal architecture"],
    mixinStandardHelpOptions = true,
)
class GenerateServiceCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = ["The name of the service to generate (e.g., 'user-management')"],
    )
    lateinit var serviceName: String

    @CommandLine.Option(
        names = ["--path"],
        description = ["Target directory: 'apps' or 'libs' (default: 'apps')"],
        defaultValue = "apps",
    )
    lateinit var targetPath: String

    override fun run() {
        try {
            validateInputs()

            val currentDir = File(System.getProperty("user.dir"))
            val generator = ServiceGenerator(currentDir)

            println("Generating service '$serviceName' in '$targetPath'...")
            generator.generateService(serviceName, targetPath)
            println("✅ Service '$serviceName' generated successfully!")
        } catch (e: Exception) {
            println("❌ Error generating service: ${e.message}")
            exitProcess(1)
        }
    }

    private fun validateInputs() {
        if (serviceName.isBlank()) {
            throw IllegalArgumentException("Service name cannot be blank")
        }

        if (!serviceName.matches(Regex("^[a-z][a-z0-9-]*[a-z0-9]$"))) {
            throw IllegalArgumentException(
                "Service name must be lowercase, start with a letter, and contain only letters, numbers, and hyphens",
            )
        }

        if (targetPath !in listOf("apps", "libs")) {
            throw IllegalArgumentException("Target path must be either 'apps' or 'libs'")
        }
    }
}

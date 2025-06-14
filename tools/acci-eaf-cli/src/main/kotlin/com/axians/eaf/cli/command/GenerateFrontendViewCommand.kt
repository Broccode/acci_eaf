package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.FrontendViewGenerator
import picocli.CommandLine
import java.io.File

@CommandLine.Command(
    name = "frontend-view",
    description = ["Generate a new frontend view for a Hilla application"],
    mixinStandardHelpOptions = true,
)
class GenerateFrontendViewCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = ["The name of the view to generate (e.g., 'Dashboard', 'UserProfile')"],
    )
    private lateinit var viewName: String

    @CommandLine.Option(
        names = ["--app"],
        description = ["The name of the Hilla application (e.g., 'acci-eaf-control-plane')"],
        required = true,
    )
    private lateinit var appName: String

    override fun run() {
        try {
            val currentDir = File(System.getProperty("user.dir"))
            val generator = FrontendViewGenerator(currentDir)

            generator.generateView(viewName, appName)

            println("✅ Successfully generated frontend view: ${viewName}View")
            println("📁 Location: apps/$appName/src/main/frontend/views/${viewName}View.tsx")
        } catch (e: Exception) {
            System.err.println("❌ Error generating frontend view: ${e.message}")
            System.exit(1)
        }
    }
}

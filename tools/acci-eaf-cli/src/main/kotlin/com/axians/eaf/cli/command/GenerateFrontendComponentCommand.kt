package com.axians.eaf.cli.command

import com.axians.eaf.cli.generator.FrontendComponentGenerator
import picocli.CommandLine
import java.io.File

@CommandLine.Command(
    name = "frontend-component",
    description = ["Generate a new frontend component with Storybook story"],
    mixinStandardHelpOptions = true,
)
class GenerateFrontendComponentCommand : Runnable {
    @CommandLine.Parameters(
        index = "0",
        description = ["The name of the component to generate (e.g., 'UserCard', 'DataTable')"],
    )
    private lateinit var componentName: String

    @CommandLine.Option(
        names = ["--lib"],
        description = ["Generate in ui-foundation-kit library"],
    )
    private var lib: String? = null

    @CommandLine.Option(
        names = ["--app"],
        description = ["Generate in specific Hilla application (e.g., 'acci-eaf-control-plane')"],
    )
    private var appName: String? = null

    override fun run() {
        try {
            val currentDir = File(System.getProperty("user.dir"))
            val generator = FrontendComponentGenerator(currentDir)

            when {
                lib == "ui-foundation-kit" -> {
                    generator.generateComponentInLibrary(componentName)
                    println("✅ Successfully generated component in UI Foundation Kit: $componentName")
                    println("📁 Location: libs/ui-foundation-kit/src/components/$componentName/")
                }
                appName != null -> {
                    generator.generateComponentInApp(componentName, appName!!)
                    println("✅ Successfully generated component in app: $componentName")
                    println("📁 Location: apps/$appName/src/main/frontend/components/$componentName/")
                }
                else -> {
                    System.err.println("❌ Error: Must specify either --lib=ui-foundation-kit or --app=<app-name>")
                    System.exit(1)
                }
            }
        } catch (e: Exception) {
            System.err.println("❌ Error generating frontend component: ${e.message}")
            System.exit(1)
        }
    }
}

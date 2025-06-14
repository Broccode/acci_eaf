package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class FrontendComponentGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateComponentInLibrary(componentName: String) {
        // Find the actual project root by looking for settings.gradle.kts
        var projectRoot = rootDir
        while (!File(projectRoot, "settings.gradle.kts").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }

        val libDir = File(projectRoot, "libs/ui-foundation-kit")
        if (!libDir.exists()) {
            throw IllegalStateException("UI Foundation Kit library does not exist: ${libDir.path}")
        }

        val componentsDir = File(libDir, "src/components")
        val componentDir = File(componentsDir, componentName)

        // Ensure component directory doesn't already exist
        if (componentDir.exists()) {
            throw IllegalStateException("Component directory already exists: ${componentDir.path}")
        }

        // Create component directory
        componentDir.mkdirs()

        // Generate component file
        val componentFile = File(componentDir, "$componentName.tsx")
        val componentContent = templateEngine.generateFrontendComponent(componentName)
        componentFile.writeText(componentContent)

        // Generate placeholder Storybook story file
        val storyFile = File(componentDir, "$componentName.stories.tsx")
        storyFile.writeText("// TODO: Generate Storybook story for $componentName")

        println("TODO: Complete Storybook configuration")

        println("Generated component files:")
        println("  - ${componentFile.path}")
        println("  - ${storyFile.path}")
    }

    fun generateComponentInApp(
        componentName: String,
        appName: String,
    ) {
        // Find the actual project root by looking for settings.gradle.kts
        var projectRoot = rootDir
        while (!File(projectRoot, "settings.gradle.kts").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }

        val appDir = File(projectRoot, "apps/$appName")
        if (!appDir.exists()) {
            throw IllegalStateException("Application directory does not exist: ${appDir.path}")
        }

        val frontendDir = File(appDir, "src/main/frontend")
        val componentsDir = File(frontendDir, "components")
        val componentDir = File(componentsDir, componentName)

        // Ensure frontend structure exists
        if (!frontendDir.exists()) {
            throw IllegalStateException(
                "Frontend directory does not exist. Generate a view first to initialize the frontend structure.",
            )
        }

        // Ensure component directory doesn't already exist
        if (componentDir.exists()) {
            throw IllegalStateException("Component directory already exists: ${componentDir.path}")
        }

        // Create component directory
        componentDir.mkdirs()

        // Generate component file
        val componentFile = File(componentDir, "$componentName.tsx")
        val componentContent = templateEngine.generateFrontendComponent(componentName)
        componentFile.writeText(componentContent)

        // Generate placeholder Storybook story file
        val storyFile = File(componentDir, "$componentName.stories.tsx")
        storyFile.writeText("// TODO: Generate Storybook story for $componentName")

        println("TODO: Complete Storybook configuration for app")

        println("Generated component files:")
        println("  - ${componentFile.path}")
        println("  - ${storyFile.path}")
    }
}

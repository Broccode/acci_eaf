package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class FrontendViewGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateView(
        viewName: String,
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
        val viewsDir = File(frontendDir, "views")

        // Ensure frontend structure exists, create if needed
        if (!frontendDir.exists()) {
            initializeFrontendStructure(frontendDir, appName)
        }

        // Ensure views directory exists
        viewsDir.mkdirs()

        // Generate the view file
        val viewFileName = "${viewName}View.tsx"
        val viewFile = File(viewsDir, viewFileName)

        if (viewFile.exists()) {
            throw IllegalStateException("View file already exists: ${viewFile.path}")
        }

        val viewContent = templateEngine.generateFrontendView(viewName)
        viewFile.writeText(viewContent)

        println("Generated view file: ${viewFile.path}")
    }

    private fun initializeFrontendStructure(
        frontendDir: File,
        appName: String,
    ) {
        // Create basic frontend directory structure
        File(frontendDir, "views").mkdirs()
        File(frontendDir, "components").mkdirs()
        File(frontendDir, "themes").mkdirs()
        File(frontendDir, "config").mkdirs()
        File(frontendDir, "locales/en").mkdirs()
        File(frontendDir, "locales/de").mkdirs()
        File(frontendDir, "public").mkdirs()

        // Create placeholder files
        File(frontendDir, "package.json").writeText("// TODO: Generate package.json for $appName")
        File(frontendDir, "tsconfig.json").writeText("// TODO: Generate tsconfig.json")
        File(frontendDir, "vite.config.ts").writeText("// TODO: Generate vite.config.ts")
        File(frontendDir, ".eslintrc.json").writeText("// TODO: Generate .eslintrc.json")
        File(frontendDir, ".prettierrc").writeText("// TODO: Generate .prettierrc")
        File(frontendDir, "config/i18n.ts").writeText("// TODO: Generate i18n config")
        File(frontendDir, "locales/en/translation.json").writeText("{}")
        File(frontendDir, "locales/de/translation.json").writeText("{}")

        println("Initialized frontend structure for app: $appName")
    }
}

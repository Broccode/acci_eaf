package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class ServiceGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateService(
        serviceName: String,
        targetPath: String,
    ) {
        // Find the actual project root by looking for settings.gradle.kts
        var projectRoot = rootDir
        while (!File(projectRoot, "settings.gradle.kts").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }

        val serviceDir = File(projectRoot, "$targetPath/$serviceName")

        // Ensure the service directory doesn't already exist
        if (serviceDir.exists()) {
            throw IllegalStateException("Service directory already exists: ${serviceDir.path}")
        }

        // Create directory structure
        createDirectoryStructure(serviceDir, serviceName)

        // Generate build.gradle.kts
        generateBuildGradle(serviceDir, serviceName)

        // Generate project.json
        generateProjectJson(serviceDir, serviceName, targetPath)

        // Generate source files
        generateSourceFiles(serviceDir, serviceName)

        // Generate test files
        generateTestFiles(serviceDir, serviceName)

        // Generate application.yml
        generateApplicationYml(serviceDir, serviceName)

        // Update settings.gradle.kts
        updateSettingsGradle(serviceName, targetPath)

        println("Generated service structure in: ${serviceDir.path}")
    }

    private fun createDirectoryStructure(
        serviceDir: File,
        serviceName: String,
    ) {
        val packagePath = "com/axians/eaf/${serviceName.replace("-", "")}"

        // Main source directories
        File(serviceDir, "src/main/kotlin/$packagePath/application/port/input").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/application/port/output").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/application/service").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/domain/model").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/domain/port").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/infrastructure/adapter/input/web").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/infrastructure/adapter/output/persistence").mkdirs()
        File(serviceDir, "src/main/kotlin/$packagePath/infrastructure/config").mkdirs()
        File(serviceDir, "src/main/resources").mkdirs()

        // Test directories
        File(serviceDir, "src/test/kotlin/$packagePath/application/service").mkdirs()
        File(serviceDir, "src/test/kotlin/$packagePath/architecture").mkdirs()
        File(serviceDir, "src/test/resources").mkdirs()
    }

    private fun generateBuildGradle(
        serviceDir: File,
        serviceName: String,
    ) {
        val buildGradleContent = templateEngine.generateBuildGradle(serviceName)
        File(serviceDir, "build.gradle.kts").writeText(buildGradleContent)
    }

    private fun generateProjectJson(
        serviceDir: File,
        serviceName: String,
        targetPath: String,
    ) {
        val projectJsonContent = templateEngine.generateProjectJson(serviceName, targetPath)
        File(serviceDir, "project.json").writeText(projectJsonContent)
    }

    private fun generateSourceFiles(
        serviceDir: File,
        serviceName: String,
    ) {
        val packagePath = "com/axians/eaf/${serviceName.replace("-", "")}"
        val packageName = "com.axians.eaf.${serviceName.replace("-", "")}"
        val className = serviceName.toCamelCase()

        // Generate main application class
        val applicationContent = templateEngine.generateApplication(packageName, className)
        val applicationClassName =
            if (className.endsWith("Service")) {
                "${className}Application"
            } else {
                "${className}ServiceApplication"
            }
        File(serviceDir, "src/main/kotlin/$packagePath/$applicationClassName.kt").writeText(applicationContent)

        // Generate sample controller
        val controllerContent = templateEngine.generateSampleController(packageName, className)
        File(
            serviceDir,
            "src/main/kotlin/$packagePath/infrastructure/adapter/input/web/Sample${className}Controller.kt",
        ).writeText(controllerContent)

        // Generate sample domain model
        val domainModelContent = templateEngine.generateSampleDomainModel(packageName, className)
        File(serviceDir, "src/main/kotlin/$packagePath/domain/model/Sample$className.kt").writeText(domainModelContent)

        // Generate sample application service interface
        val serviceInterfaceContent = templateEngine.generateSampleServiceInterface(packageName, className)
        File(
            serviceDir,
            "src/main/kotlin/$packagePath/application/port/input/Sample${className}UseCase.kt",
        ).writeText(serviceInterfaceContent)

        // Generate sample application service implementation
        val serviceImplContent = templateEngine.generateSampleServiceImpl(packageName, className)
        File(
            serviceDir,
            "src/main/kotlin/$packagePath/application/service/Sample${className}Service.kt",
        ).writeText(serviceImplContent)
    }

    private fun generateTestFiles(
        serviceDir: File,
        serviceName: String,
    ) {
        val packagePath = "com/axians/eaf/${serviceName.replace("-", "")}"
        val packageName = "com.axians.eaf.${serviceName.replace("-", "")}"
        val className = serviceName.toCamelCase()

        // Generate ArchUnit test
        val archTestContent = templateEngine.generateArchitectureTest(packageName, className)
        File(serviceDir, "src/test/kotlin/$packagePath/architecture/ArchitectureTest.kt").writeText(archTestContent)

        // Generate sample service test
        val serviceTestContent = templateEngine.generateSampleServiceTest(packageName, className)
        File(
            serviceDir,
            "src/test/kotlin/$packagePath/application/service/Sample${className}ServiceTest.kt",
        ).writeText(serviceTestContent)
    }

    private fun generateApplicationYml(
        serviceDir: File,
        serviceName: String,
    ) {
        val applicationYmlContent = templateEngine.generateApplicationYml(serviceName)
        File(serviceDir, "src/main/resources/application.yml").writeText(applicationYmlContent)
    }

    private fun updateSettingsGradle(
        serviceName: String,
        targetPath: String,
    ) {
        // Find the actual project root by looking for settings.gradle.kts
        var searchDir = rootDir
        while (!File(searchDir, "settings.gradle.kts").exists() && searchDir.parentFile != null) {
            searchDir = searchDir.parentFile
        }

        val settingsFile = File(searchDir, "settings.gradle.kts")
        if (!settingsFile.exists()) {
            throw IllegalStateException("Could not find settings.gradle.kts in project hierarchy")
        }

        val content = settingsFile.readText()
        val newInclude = "include(\":$targetPath:$serviceName\")"

        // Add the new include statement before the tools section
        val toolsIndex = content.indexOf("// Include tools")
        if (toolsIndex != -1) {
            val beforeTools = content.substring(0, toolsIndex)
            val afterTools = content.substring(toolsIndex)
            val updatedContent = "$beforeTools$newInclude\n\n$afterTools"
            settingsFile.writeText(updatedContent)
        } else {
            // Fallback: append at the end
            settingsFile.writeText("$content\n$newInclude\n")
        }
    }

    private fun String.toCamelCase(): String =
        this
            .split("-")
            .joinToString("") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
}

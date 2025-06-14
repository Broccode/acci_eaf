package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class CommandGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateCommand(
        commandName: String,
        aggregateName: String,
        serviceName: String,
    ) {
        // Find the actual project root by looking for settings.gradle.kts
        var projectRoot = rootDir
        while (!File(projectRoot, "settings.gradle.kts").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }

        val serviceDir = File(projectRoot, "apps/$serviceName")

        // Ensure the service directory exists
        if (!serviceDir.exists()) {
            throw IllegalStateException(
                "Service directory does not exist: ${serviceDir.path}. Generate the service first using 'eaf generate service $serviceName'",
            )
        }

        val packagePath = "com/axians/eaf/${serviceName.replace("-", "")}"
        val packageName = "com.axians.eaf.${serviceName.replace("-", "")}"

        // Generate Command data class
        generateCommandFile(serviceDir, packagePath, packageName, commandName, aggregateName)

        // Add command handler to existing Aggregate
        addCommandHandlerToAggregate(serviceDir, packagePath, packageName, commandName, aggregateName, serviceName)

        println("Generated command '$commandName' and added handler to aggregate '$aggregateName'")
    }

    private fun generateCommandFile(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        commandName: String,
        aggregateName: String,
    ) {
        val commandContent = templateEngine.generateCommand(packageName, commandName, aggregateName)
        val commandDir = File(serviceDir, "src/main/kotlin/$packagePath/domain/command")
        commandDir.mkdirs()

        File(commandDir, "$commandName.kt").writeText(commandContent)
    }

    private fun addCommandHandlerToAggregate(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        commandName: String,
        aggregateName: String,
        serviceName: String,
    ) {
        val aggregateFile = File(serviceDir, "src/main/kotlin/$packagePath/domain/model/$aggregateName.kt")

        if (!aggregateFile.exists()) {
            throw IllegalStateException(
                "Aggregate file does not exist: ${aggregateFile.path}. Generate the aggregate first using 'eaf generate aggregate $aggregateName --service=$serviceName'",
            )
        }

        val currentContent = aggregateFile.readText()

        // Add import for the new command
        val importToAdd = "import $packageName.domain.command.$commandName"
        val updatedContentWithImport =
            if (!currentContent.contains(importToAdd)) {
                // Find the last import line and add the new import after it
                val importLines = currentContent.lines()
                val lastImportIndex = importLines.indexOfLast { it.trim().startsWith("import ") }
                if (lastImportIndex != -1) {
                    val beforeImports = importLines.take(lastImportIndex + 1)
                    val afterImports = importLines.drop(lastImportIndex + 1)
                    (beforeImports + importToAdd + afterImports).joinToString("\n")
                } else {
                    // No imports found, add after package declaration
                    val packageLine = importLines.indexOfFirst { it.trim().startsWith("package ") }
                    if (packageLine != -1) {
                        val beforePackage = importLines.take(packageLine + 1)
                        val afterPackage = importLines.drop(packageLine + 1)
                        (beforePackage + "" + importToAdd + afterPackage).joinToString("\n")
                    } else {
                        "$importToAdd\n$currentContent"
                    }
                }
            } else {
                currentContent
            }

        // Add the command handler method
        val commandHandlerMethod = generateCommandHandlerMethod(commandName, aggregateName)

        // Find the end of the class (before the last closing brace) and add the method
        val lines = updatedContentWithImport.lines().toMutableList()
        val lastClosingBraceIndex = lines.indexOfLast { it.trim() == "}" }

        if (lastClosingBraceIndex != -1) {
            // Add the command handler method before the last closing brace
            lines.add(lastClosingBraceIndex, "")
            lines.add(lastClosingBraceIndex + 1, commandHandlerMethod)
        } else {
            throw IllegalStateException("Could not find the end of the $aggregateName class to add the command handler")
        }

        aggregateFile.writeText(lines.joinToString("\n"))
    }

    private fun generateCommandHandlerMethod(
        commandName: String,
        aggregateName: String,
    ): String =
        """    /**
     * Command handler for $commandName.
     * TODO: Implement business logic and apply appropriate event(s)
     */
    @EafCommandHandler
    fun handle(command: $commandName) {
        // TODO: Validate business rules
        // TODO: Apply appropriate event(s)
        // Example:
        // applyEvent(${aggregateName}UpdatedEvent(...))
    }"""
}

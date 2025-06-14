package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class EventGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateEvent(
        eventName: String,
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

        // Generate Event data class
        generateEventFile(serviceDir, packagePath, packageName, eventName, aggregateName)

        // Add event sourcing handler to existing Aggregate
        addEventSourcingHandlerToAggregate(serviceDir, packagePath, packageName, eventName, aggregateName, serviceName)

        println("Generated event '$eventName' and added handler to aggregate '$aggregateName'")
    }

    private fun generateEventFile(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        eventName: String,
        aggregateName: String,
    ) {
        val eventContent = templateEngine.generateEvent(packageName, eventName, aggregateName)
        val eventDir = File(serviceDir, "src/main/kotlin/$packagePath/domain/event")
        eventDir.mkdirs()

        File(eventDir, "$eventName.kt").writeText(eventContent)
    }

    private fun addEventSourcingHandlerToAggregate(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        eventName: String,
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

        // Add import for the new event
        val importToAdd = "import $packageName.domain.event.$eventName"
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

        // Add the event sourcing handler method
        val eventSourcingHandlerMethod = generateEventSourcingHandlerMethod(eventName, aggregateName)

        // Find the end of the class (before the last closing brace) and add the method
        val lines = updatedContentWithImport.lines().toMutableList()
        val lastClosingBraceIndex = lines.indexOfLast { it.trim() == "}" }

        if (lastClosingBraceIndex != -1) {
            // Add the event sourcing handler method before the last closing brace
            lines.add(lastClosingBraceIndex, "")
            lines.add(lastClosingBraceIndex + 1, eventSourcingHandlerMethod)
        } else {
            throw IllegalStateException(
                "Could not find the end of the $aggregateName class to add the event sourcing handler",
            )
        }

        aggregateFile.writeText(lines.joinToString("\n"))
    }

    private fun generateEventSourcingHandlerMethod(
        eventName: String,
        aggregateName: String,
    ): String =
        """    /**
     * Event sourcing handler for $eventName.
     * TODO: Apply state changes from this event
     */
    @EafEventSourcingHandler
    fun on(event: $eventName) {
        // TODO: Apply state changes based on the event
        // Update aggregate state fields
        this.updatedAt = event.occurredAt

        // Example:
        // this.name = event.newName
        // this.description = event.updatedDescription
    }"""
}

package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class AggregateGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateAggregate(
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
        generateCommand(serviceDir, packagePath, packageName, aggregateName)

        // Generate Event data class
        generateEvent(serviceDir, packagePath, packageName, aggregateName)

        // Generate Aggregate root class
        generateAggregateRoot(serviceDir, packagePath, packageName, aggregateName)

        // Generate test file
        generateAggregateTest(serviceDir, packagePath, packageName, aggregateName)

        println("Generated aggregate structure for '$aggregateName' in service '$serviceName'")
    }

    private fun generateCommand(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        aggregateName: String,
    ) {
        val commandContent = templateEngine.generateCreateCommand(packageName, aggregateName)
        val commandDir = File(serviceDir, "src/main/kotlin/$packagePath/domain/command")
        commandDir.mkdirs()

        File(commandDir, "Create${aggregateName}Command.kt").writeText(commandContent)
    }

    private fun generateEvent(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        aggregateName: String,
    ) {
        val eventContent = templateEngine.generateCreatedEvent(packageName, aggregateName)
        val eventDir = File(serviceDir, "src/main/kotlin/$packagePath/domain/event")
        eventDir.mkdirs()

        File(eventDir, "${aggregateName}CreatedEvent.kt").writeText(eventContent)
    }

    private fun generateAggregateRoot(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        aggregateName: String,
    ) {
        val aggregateContent = templateEngine.generateAggregateRoot(packageName, aggregateName)
        val modelDir = File(serviceDir, "src/main/kotlin/$packagePath/domain/model")
        modelDir.mkdirs()

        File(modelDir, "$aggregateName.kt").writeText(aggregateContent)
    }

    private fun generateAggregateTest(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        aggregateName: String,
    ) {
        val testContent = templateEngine.generateAggregateTest(packageName, aggregateName)
        val testDir = File(serviceDir, "src/test/kotlin/$packagePath/domain/model")
        testDir.mkdirs()

        File(testDir, "${aggregateName}Test.kt").writeText(testContent)
    }
}

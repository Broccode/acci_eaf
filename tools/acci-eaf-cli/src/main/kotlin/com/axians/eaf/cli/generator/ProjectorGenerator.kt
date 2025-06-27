package com.axians.eaf.cli.generator

import com.axians.eaf.cli.template.TemplateEngine
import java.io.File

class ProjectorGenerator(
    private val rootDir: File,
) {
    private val templateEngine = TemplateEngine()

    fun generateProjector(
        projectorName: String,
        eventName: String,
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

        // Generate Projector class
        generateProjectorFile(
            serviceDir,
            packagePath,
            packageName,
            projectorName,
            eventName,
            serviceName,
        )

        println("Generated projector '$projectorName' for event '$eventName' in service '$serviceName'")
    }

    private fun generateProjectorFile(
        serviceDir: File,
        packagePath: String,
        packageName: String,
        projectorName: String,
        eventName: String,
        serviceName: String,
    ) {
        val projectorContent =
            templateEngine.generateProjector(packageName, projectorName, eventName, serviceName)

        // Create projector in the infrastructure/adapter/input/messaging package
        val projectorDir =
            File(serviceDir, "src/main/kotlin/$packagePath/infrastructure/adapter/input/messaging")
        projectorDir.mkdirs()

        File(projectorDir, "$projectorName.kt").writeText(projectorContent)
    }
}

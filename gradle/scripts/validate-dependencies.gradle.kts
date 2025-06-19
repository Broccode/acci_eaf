/**
 * Dependency Version Validation Script
 *
 * This script validates that all modules use the Gradle Version Catalog (`libs.versions.toml`).
 * It checks for legacy dependency declarations and ensures compliance with modern standards.
 */

tasks.register("validateDependencies") {
    group = "verification"
    description = "Validates that all modules use the Gradle Version Catalog"

    doLast {
        val violations = mutableListOf<String>()
        val rootDir = project.rootDir

        // Find all module build.gradle.kts files
        val buildFiles = fileTree(rootDir) {
            include("**/build.gradle.kts")
            exclude("build.gradle.kts") // Exclude root build file
            exclude("gradle/**") // Exclude gradle scripts
            exclude(".nx/**") // Exclude Nx cache
            exclude("**/build/**") // Exclude build directories
            exclude("**/node_modules/**") // Exclude node_modules
        }

        buildFiles.forEach { buildFile ->
            val relativePath = rootDir.toPath().relativize(buildFile.toPath()).toString()
            val content = buildFile.readText()

            // Check for hardcoded versions in dependencies (e.g., "group:artifact:1.0.0")
            val hardcodedVersionPattern = Regex(
                """(implementation|testImplementation|api|compileOnly|runtimeOnly)\s*\(\s*["'][^"']+:[^"']+:[\d.]+[^"']*["']\s*\)"""
            )
            hardcodedVersionPattern.findAll(content).forEach { match ->
                val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
                violations.add("$relativePath:$lineNumber - Found hardcoded dependency version. Please use the version catalog (libs.versions.toml). Offending line: ${match.value}")
            }

            // Check for legacy rootProject.extra properties
            val extraPropertyPattern = Regex("""rootProject\.extra\["([^"]+)"]""")
            extraPropertyPattern.findAll(content).forEach { match ->
                val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
                violations.add("$relativePath:$lineNumber - Found legacy 'rootProject.extra' property. Please use the version catalog (libs.versions.toml). Offending line: ${match.value}")
            }
        }

        if (violations.isNotEmpty()) {
            val violationReport = violations.joinToString("\n")
            throw GradleException(
                """
                |❌ DEPENDENCY VALIDATION FAILED!
                |
                |Found ${violations.size} violation(s). All modules must use the Gradle Version Catalog (libs.versions.toml).
                |
                |$violationReport
                |
                |📋 Required Actions:
                |1. Remove all hardcoded dependency versions.
                |2. Remove all references to `rootProject.extra[...]`.
                |3. Define all dependencies in `gradle/libs.versions.toml` and reference them via the `libs` object.
                |
                |💡 Example Fix:
                |   ❌ implementation("io.mockk:mockk:1.13.12")
                |   ❌ implementation("io.mockk:mockk:${'$'}{rootProject.extra["mockkVersion"]}")
                |   ✅ implementation(libs.mockk)
                """.trimMargin()
            )
        } else {
            println("✅ All modules comply with the Gradle Version Catalog!")
        }
    }
}

// Add validation to subprojects that have check/build tasks
subprojects {
    afterEvaluate {
        tasks.findByName("check")?.dependsOn(rootProject.tasks.named("validateDependencies"))
        tasks.findByName("build")?.dependsOn(rootProject.tasks.named("validateDependencies"))
    }
}

/**
 * Dependency Version Validation Script
 *
 * This script validates that all modules follow the centralized dependency management pattern.
 * It checks for hardcoded versions and ensures compliance with project standards.
 */

tasks.register("validateDependencies") {
    group = "verification"
    description = "Validates that all modules use centralized dependency versions"

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

            // Check for hardcoded versions in dependencies
            val hardcodedVersionPattern = Regex(
                """(implementation|testImplementation|api|compileOnly|runtimeOnly)\s*\(\s*["'][^"']+:[^"']+:[\d.]+[^"']*["']\s*\)"""
            )

            val matches = hardcodedVersionPattern.findAll(content)
            matches.forEach { match ->
                val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
                violations.add("$relativePath:$lineNumber - Hardcoded version found: ${match.value}")
            }

            // Check for version catalog usage without centralized management
            val versionCatalogPattern = Regex("""libs\.[a-zA-Z0-9.]+""")
            val catalogMatches = versionCatalogPattern.findAll(content)
            catalogMatches.forEach { match ->
                val lineNumber = content.substring(0, match.range.first).count { it == '\n' } + 1
                violations.add("$relativePath:$lineNumber - Version catalog usage found (should use centralized versions): ${match.value}")
            }
        }

        if (violations.isNotEmpty()) {
            val violationReport = violations.joinToString("\n")
            throw GradleException(
                """
                |❌ DEPENDENCY VALIDATION FAILED!
                |
                |Found ${violations.size} violation(s):
                |
                |$violationReport
                |
                |📋 Required Actions:
                |1. Replace hardcoded versions with centralized versions from root build.gradle.kts
                |2. Use pattern: implementation("group:artifact:${'$'}{rootProject.extra["versionVariable"]}")
                |3. See docs/troubleshooting/dependency-management-guidelines.md for details
                |
                |💡 Example Fix:
                |   ❌ implementation("io.mockk:mockk:1.13.12")
                |   ✅ implementation("io.mockk:mockk:${'$'}{rootProject.extra["mockkVersion"]}")
                """.trimMargin()
            )
        } else {
            println("✅ All modules comply with centralized dependency management!")
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

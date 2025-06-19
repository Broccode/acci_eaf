import java.time.Duration

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotless) apply false
    id("dev.nx.gradle.project-graph") version "+"
}

allprojects {
    group = "com.axians.eaf"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

allprojects {
    apply(plugin = "dev.nx.gradle.project-graph")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.diffplug.spotless")

    afterEvaluate {
        if (plugins.hasPlugin("java") && !project.path.startsWith(":tools:")) {
            configure<JavaPluginExtension> {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }
        }
    }

    // Configure Spotless for Kotlin formatting
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(libs.versions.ktlint.get())
                .setEditorConfigPath("$rootDir/.editorconfig")
            trimTrailingWhitespace()
            indentWithSpaces(4)
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        timeout.set(Duration.ofMinutes(10))
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
}

// Configure specific project types
configure(subprojects.filter { it.path.startsWith(":apps:") }) {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
}

configure(subprojects.filter { it.path.startsWith(":libs:") }) {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
}

configure(subprojects.filter { it.path.startsWith(":tools:") }) {
    apply(plugin = "application")
}

// ============================================================================
// DEPENDENCY VALIDATION
// ============================================================================
// Apply validation script to enforce centralized dependency management
apply(from = "gradle/scripts/validate-dependencies.gradle.kts")

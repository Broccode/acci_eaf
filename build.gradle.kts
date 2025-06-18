plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    kotlin("plugin.jpa") version "2.0.21" apply false
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("com.diffplug.spotless") version "7.0.0.BETA4" apply false
    id("dev.nx.gradle.project-graph") version "+"
}

// ============================================================================
// CENTRALIZED DEPENDENCY VERSION MANAGEMENT
// ============================================================================
// All versions are centrally defined here and used across all modules
// For updates, only change here - automatic propagation to all modules

// Core Framework Versions
val kotlinVersion = "2.0.21"
val springBootVersion = "3.5.0"
val springFrameworkVersion = "6.2.7"
val springSecurityVersion = "6.2.1"
val springDataVersion = "3.2.1"

// Testing Framework Versions
val junitVersion = "5.11.4"
val junitPlatformVersion = "1.11.4"
val mockkVersion = "1.13.12"
val springMockkVersion = "4.0.2"
val testcontainersVersion = "1.19.7"
val archunitVersion = "1.3.0"
val assertjVersion = "3.24.2"

// Database & Persistence Versions
val postgresqlVersion = "42.7.2"
val flywayVersion = "10.8.1"
val hikariVersion = "5.1.0"

// Serialization & Communication Versions
val jacksonVersion = "2.16.1"
val natsVersion = "2.17.2"
val nimbusJoseVersion = "9.37.3"

// Utility & Logging Versions
val slf4jVersion = "2.0.9"
val kotlinCoroutinesVersion = "1.8.1"
val jakartaServletVersion = "6.0.0"

// Frontend Versions
val hillaVersion = "2.5.8"
val vaadinVersion = "24.4.12"

// Build & Code Quality Versions
val spotlessVersion = "7.0.0.BETA4"
val ktlintVersion = "1.3.1"

// Additional Testing & Utility Versions
val okhttpVersion = "4.12.0"

// CLI Tool Versions
val picocliVersion = "4.7.5"
val commonsLangVersion = "3.12.0"

allprojects {
    group = "com.axians.eaf"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    // ========================================================================
    // MAKE DEPENDENCY VERSIONS AVAILABLE FOR ALL SUBPROJECTS
    // ========================================================================
    // Core Framework
    extra["kotlinVersion"] = kotlinVersion
    extra["springBootVersion"] = springBootVersion
    extra["springFrameworkVersion"] = springFrameworkVersion
    extra["springSecurityVersion"] = springSecurityVersion
    extra["springDataVersion"] = springDataVersion

    // Testing
    extra["junitVersion"] = junitVersion
    extra["junitPlatformVersion"] = junitPlatformVersion
    extra["mockkVersion"] = mockkVersion
    extra["springMockkVersion"] = springMockkVersion
    extra["testcontainersVersion"] = testcontainersVersion
    extra["archunitVersion"] = archunitVersion
    extra["assertjVersion"] = assertjVersion

    // Database & Persistence
    extra["postgresqlVersion"] = postgresqlVersion
    extra["flywayVersion"] = flywayVersion
    extra["hikariVersion"] = hikariVersion

    // Serialization & Communication
    extra["jacksonVersion"] = jacksonVersion
    extra["natsVersion"] = natsVersion
    extra["nimbusJoseVersion"] = nimbusJoseVersion

    // Utility & Logging
    extra["slf4jVersion"] = slf4jVersion
    extra["kotlinCoroutinesVersion"] = kotlinCoroutinesVersion
    extra["jakartaServletVersion"] = jakartaServletVersion

    // Frontend
    extra["hillaVersion"] = hillaVersion
    extra["vaadinVersion"] = vaadinVersion

    // Build & Code Quality
    extra["spotlessVersion"] = spotlessVersion
    extra["ktlintVersion"] = ktlintVersion

    // Additional Testing & Utility
    extra["okhttpVersion"] = okhttpVersion

    // CLI Tool Versions
    extra["picocliVersion"] = picocliVersion
    extra["commonsLangVersion"] = commonsLangVersion
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
            ktlint("1.3.1")
                .setEditorConfigPath("$rootDir/.editorconfig")
            trimTrailingWhitespace()
            indentWithSpaces(4)
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.3.1")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
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
}

configure(subprojects.filter { it.path.startsWith(":tools:") }) {
    apply(plugin = "application")
}

// ============================================================================
// DEPENDENCY VALIDATION
// ============================================================================
// Apply validation script to enforce centralized dependency management
apply(from = "gradle/scripts/validate-dependencies.gradle.kts")

plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    kotlin("plugin.jpa") version "2.0.21" apply false
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("com.diffplug.spotless") version "7.0.0.BETA4" apply false
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
        if (plugins.hasPlugin("java")) {
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

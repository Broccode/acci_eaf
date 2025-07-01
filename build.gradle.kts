import java.time.Duration

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.spring) apply false
  alias(libs.plugins.kotlin.jpa) apply false
  alias(libs.plugins.spring.boot) apply false
  alias(libs.plugins.spring.dependency.management) apply false
  alias(libs.plugins.ktlint) apply false
  alias(libs.plugins.detekt) apply false
  id("dev.nx.gradle.project-graph") version "+"
}

allprojects {
  group = "com.axians.eaf"
  version = "0.1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    // Sonatype OSS for milestone/snapshot releases
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/staging/")
    }
  }


}

allprojects {
  apply(plugin = "dev.nx.gradle.project-graph")
}

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
  apply(plugin = "io.gitlab.arturbosch.detekt")

  // Access the central version catalog inside sub-project build scripts
  val catalog = rootProject.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

  // Force Kotlin version consistency across all configurations except detekt
  // Exclude detekt configurations to avoid version conflicts with detekt's embedded Kotlin compiler
  configurations.matching { !it.name.startsWith("detekt") }.all {
    resolutionStrategy {
      force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
      force("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
      force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
    }
  }

  extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension>("ktlint") {
    version.set(catalog.findVersion("ktlint").get().requiredVersion)
  }

  extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
    toolVersion = catalog.findVersion("detekt").get().requiredVersion
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
    ignoreFailures = false

    // Use baseline files if they exist
    baseline = file("detekt-baseline.xml")
  }

  // Configure detekt configurations to override dependency management
  afterEvaluate {
    configurations.matching { it.name.startsWith("detekt") }.all {
      resolutionStrategy {
        // Force resolution to override BOM versions
        force("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
        force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        force("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

        // Override any version from BOM or dependency management
        eachDependency {
          if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.21")
            because("detekt requires Kotlin 2.0.21 to work properly")
          }
        }
      }
    }
  }



  afterEvaluate {
    if (plugins.hasPlugin("java") && !project.path.startsWith(":tools:")) {
      configure<JavaPluginExtension> {
        toolchain {
          languageVersion.set(JavaLanguageVersion.of(21))
        }
      }
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

  tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
      html.required.set(true)
      xml.required.set(true)
      sarif.required.set(true)
      md.required.set(false)
    }
  }

  tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "21"
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

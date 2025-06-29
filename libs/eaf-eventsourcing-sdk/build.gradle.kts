plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.spring.dependency.management)
  `java-library`
}

dependencyManagement {
  imports {
    mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
  }
}

dependencies {
  // Spring Boot
  implementation(libs.spring.boot.starter)
  implementation(libs.spring.boot.starter.data.jpa)

  // Axon Framework 4.11.2 - Using individual modules
  implementation(libs.axon.messaging)
  implementation(libs.axon.modelling)
  implementation(libs.axon.eventsourcing)

  // Kotlin coroutines
  implementation(libs.kotlin.coroutines.core)
  implementation(libs.kotlin.coroutines.reactor)

  // Core EAF modules
  implementation(project(":libs:eaf-core"))

  // Database
  implementation(libs.postgresql)
  runtimeOnly(libs.flyway.core)
  runtimeOnly(libs.flyway.database.postgresql)

  // Serialization
  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.datatype.jsr310)

  // Testing
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.spring.boot.testcontainers)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.platform.launcher)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.testcontainers.postgresql)
  testImplementation(libs.mockk)
  testImplementation(libs.axon.test)
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

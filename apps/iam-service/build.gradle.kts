plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.kotlin.jpa)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
}

dependencies {
  implementation(libs.spring.boot.starter)
  implementation(libs.spring.boot.starter.web)
  implementation(libs.spring.boot.starter.security)
  implementation(libs.spring.boot.starter.data.jpa)
  implementation(libs.spring.boot.starter.validation)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.coroutines.core)
  implementation(libs.kotlin.coroutines.reactor)

  // EAF Core dependency
  implementation(project(":libs:eaf-core"))

  // Jackson Kotlin module for JSON serialization
  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.datatype.jsr310)

  runtimeOnly(libs.postgresql)

  developmentOnly(libs.spring.boot.docker.compose)

  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockk)
  testImplementation(libs.assertj.core)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.testcontainers.postgresql)
  testImplementation(libs.spring.boot.testcontainers)
  testImplementation(libs.spring.mockk)
  testImplementation(libs.spring.security.test)

  // ArchUnit for architectural testing
  testImplementation(libs.archunit.junit5)
}

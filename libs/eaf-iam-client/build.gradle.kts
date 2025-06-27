plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.spring.dependency.management)
}

dependencyManagement {
  imports {
    mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
  }
}

dependencies {
  // EAF Core
  implementation(project(":libs:eaf-core"))

  // Spring Framework
  implementation(libs.spring.web)
  implementation(libs.spring.webmvc)
  implementation(libs.spring.context)
  implementation(libs.spring.security.web)
  implementation(libs.spring.security.config)
  implementation(libs.spring.security.core)
  implementation(libs.spring.boot)
  implementation(libs.spring.boot.autoconfigure)

  // Servlet API
  implementation(libs.jakarta.servlet.api)

  // Kotlin
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.coroutines.core)
  implementation(libs.kotlin.coroutines.reactor)

  // JSON processing
  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.datatype.jsr310)

  // HTTP Client for IAM introspection
  implementation(libs.spring.webflux)
  implementation(libs.reactor.netty.http)

  // JWT processing
  implementation(libs.nimbus.jose.jwt)

  // Logging
  implementation(libs.slf4j.api)

  // Test dependencies
  testImplementation(libs.spring.boot.test)
  testImplementation(libs.spring.boot.test.autoconfigure)
  testImplementation(libs.spring.test)
  testImplementation(libs.spring.security.test)
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.mockk)
  testImplementation(libs.assertj.core)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.testcontainers.junit.jupiter)
  testImplementation(libs.testcontainers.core)
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "21"
  }
}

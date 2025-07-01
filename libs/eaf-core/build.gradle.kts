plugins {
  alias(libs.plugins.kotlin.jvm)
  `java-library`
}

dependencyManagement {
  imports {
    mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
  }
}

dependencies {
  // Core dependencies will be added as the module develops
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.reflect)

  // Spring Security for context management
  implementation(libs.spring.security.core)

  // Spring Context for configuration
  implementation(libs.spring.context)
  implementation(libs.spring.boot.autoconfigure)

  // Spring Web for web-related components
  implementation(libs.spring.web)

  // Jakarta Servlet API for filters
  implementation(libs.jakarta.servlet.api)

  // Jakarta Validation for configuration properties
  implementation("jakarta.validation:jakarta.validation-api")

  // Micrometer for metrics
  implementation(libs.micrometer.core)

  // Coroutines for async context propagation
  implementation(libs.kotlin.coroutines.core)

  // Axon Framework for correlation data provider
  implementation(libs.axon.messaging)

  // Jackson for JSON serialization
  implementation(libs.jackson.module.kotlin)

  // Logging
  implementation(libs.slf4j.api)

  // Testing dependencies using version catalog
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockk)
  testImplementation(libs.mockk.agent.jvm)
  testImplementation(libs.archunit.junit5)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.junit.platform.launcher)
  testImplementation(libs.spring.boot.test)
  testImplementation(libs.spring.boot.test.autoconfigure)
}

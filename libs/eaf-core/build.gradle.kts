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

  // Coroutines for async context propagation
  implementation(libs.kotlin.coroutines.core)

  // Logging
  implementation(libs.slf4j.api)

  // Testing dependencies using version catalog
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockk)
  testImplementation(libs.archunit.junit5)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.junit.platform.launcher)
}

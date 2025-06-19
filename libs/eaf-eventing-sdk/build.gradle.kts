plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
    `java-library`
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // EAF Core
    implementation(project(":libs:eaf-core"))

    // Spring Security for context management
    implementation(libs.spring.security.core)
    implementation(libs.spring.context)
    implementation(libs.spring.boot.autoconfigure)

    // Core dependencies
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines.core)

    // Spring Boot dependencies
    implementation(libs.spring.boot.starter)

    // NATS client
    implementation(libs.nats.client)

    // JSON processing
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // Database access for processed events tracking
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)

    // Logging
    implementation(libs.slf4j.api)

    // Testing dependencies
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlin.coroutines.test)

    // For integration tests with actual JDBC
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.spring.boot.starter.jdbc)
    testImplementation(libs.hikariCP)
}

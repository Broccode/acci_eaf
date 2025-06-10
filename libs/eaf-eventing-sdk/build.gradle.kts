plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // EAF Core
    implementation(project(":libs:eaf-core"))

    // Spring Security for context management
    implementation("org.springframework.security:spring-security-core:6.2.1")
    implementation("org.springframework:spring-context:6.1.2")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.2.1")

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
    testImplementation(libs.mockk)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.generic)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    // For integration tests with actual JDBC
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("com.zaxxer:HikariCP")
}

plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
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

    // Logging
    implementation(libs.slf4j.api)

    // Testing dependencies
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.generic)
    testImplementation(libs.archunit.junit5)
}

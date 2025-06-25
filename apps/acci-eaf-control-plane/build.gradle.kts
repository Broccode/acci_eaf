plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.vaadin)
}

group = "com.axians.eaf"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.vaadin.com/vaadin-prereleases") } // Hilla might need this
}

dependencies {
    // Force Spring Boot version to resolve conflicts
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.1"))

    // EAF SDKs
    implementation(project(":libs:eaf-core"))
    implementation(project(":libs:eaf-iam-client"))
    implementation(project(":libs:eaf-eventing-sdk"))

    // Hilla / Vaadin
    implementation(libs.vaadin.spring.boot.starter)

    // Spring Boot
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    // Monitoring & Metrics
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.resilience4j.spring.boot2)
    implementation(libs.resilience4j.micrometer)

    // Structured Logging
    implementation(libs.logstash.logback.encoder)

    // Database
    runtimeOnly(libs.postgresql)
    implementation(libs.flyway.core)

    // Kotlin
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.reactor)

    // Jackson
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    // NATS Client
    implementation(libs.nats.client)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.mockk)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.boot.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)

    // ArchUnit for architectural testing
    testImplementation(libs.archunit.junit5)
}

vaadin {
    productionMode = true
}

// Fix task dependency issue between Vaadin and Spotless
tasks.named("spotlessKotlin") {
    mustRunAfter("vaadinPrepareFrontend")
}

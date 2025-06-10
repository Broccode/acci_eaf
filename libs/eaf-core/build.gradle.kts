plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Core dependencies will be added as the module develops
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Security for context management
    implementation("org.springframework.security:spring-security-core:6.2.1")

    // Spring Context for configuration
    implementation("org.springframework:spring-context:6.1.2")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.2.1")

    // Coroutines for async context propagation
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Testing dependencies using version catalog
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

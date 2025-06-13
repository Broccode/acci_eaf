plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Core dependencies will be added as the module develops
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.extra["kotlinVersion"]}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${rootProject.extra["kotlinVersion"]}")

    // Spring Security for context management
    implementation("org.springframework.security:spring-security-core:${rootProject.extra["springSecurityVersion"]}")

    // Spring Context for configuration
    implementation("org.springframework:spring-context:${rootProject.extra["springFrameworkVersion"]}")
    implementation("org.springframework.boot:spring-boot-autoconfigure:${rootProject.extra["springBootVersion"]}")

    // Coroutines for async context propagation
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["kotlinCoroutinesVersion"]}")

    // Logging
    implementation("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")

    // Testing dependencies using version catalog
    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
    testImplementation("io.mockk:mockk:${rootProject.extra["mockkVersion"]}")
    testImplementation("com.tngtech.archunit:archunit-junit5:${rootProject.extra["archunitVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${rootProject.extra["kotlinVersion"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["kotlinCoroutinesVersion"]}")
}

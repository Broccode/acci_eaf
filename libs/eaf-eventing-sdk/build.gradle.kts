plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
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
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // Core dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["kotlinCoroutinesVersion"]}")

    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter")

    // NATS client
    implementation("io.nats:jnats:${rootProject.extra["natsVersion"]}")

    // JSON processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Database access for processed events tracking
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:${rootProject.extra["mockkVersion"]}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:testcontainers:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("com.tngtech.archunit:archunit-junit5:${rootProject.extra["archunitVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["kotlinCoroutinesVersion"]}")

    // For integration tests with actual JDBC
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("com.zaxxer:HikariCP")
}

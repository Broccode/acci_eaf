plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Vaadin dependencies
    implementation("com.vaadin:vaadin-spring-boot-starter:${rootProject.extra["vaadinVersion"]}")
    implementation("com.vaadin:vaadin-dev-server:${rootProject.extra["vaadinVersion"]}")

    // EAF SDK dependencies - all required for the pilot
    implementation(project(":libs:eaf-core"))
    implementation(project(":libs:eaf-eventsourcing-sdk"))
    implementation(project(":libs:eaf-eventing-sdk"))
    implementation(project(":libs:eaf-iam-client"))

    // NATS client for eventing (transitive dependency from eaf-eventing-sdk)
    implementation("io.nats:jnats:${rootProject.extra["natsVersion"]}")

    // Jackson Kotlin module for JSON serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    runtimeOnly("org.postgresql:postgresql")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
    testImplementation("io.mockk:mockk:${rootProject.extra["mockkVersion"]}")
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["kotlinCoroutinesVersion"]}")
    testImplementation("org.testcontainers:junit-jupiter:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.testcontainers:postgresql:${rootProject.extra["testcontainersVersion"]}")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("com.ninja-squad:springmockk:${rootProject.extra["springMockkVersion"]}")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")

    // ArchUnit for architectural testing
    testImplementation("com.tngtech.archunit:archunit-junit5:${rootProject.extra["archunitVersion"]}")
}

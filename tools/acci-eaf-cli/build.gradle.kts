// Plugins are applied by root build.gradle.kts

application {
    mainClass.set("com.axians.eaf.cli.AcciEafCliApplicationKt")
}

dependencies {
    implementation("info.picocli:picocli:${rootProject.extra["picocliVersion"]}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${rootProject.extra["jacksonVersion"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${rootProject.extra["jacksonVersion"]}")

    // For file operations and utilities
    implementation("org.apache.commons:commons-lang3:${rootProject.extra["commonsLangVersion"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
    testImplementation("io.mockk:mockk:${rootProject.extra["mockkVersion"]}")
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.axians.eaf.cli.AcciEafCliApplication"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

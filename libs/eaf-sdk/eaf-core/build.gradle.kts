plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Core dependencies will be added as the module develops
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.12")
}

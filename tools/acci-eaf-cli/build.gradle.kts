// Plugins are applied by root build.gradle.kts

application { mainClass.set("com.axians.eaf.cli.AcciEafCliApplicationKt") }

dependencies {
  implementation(libs.picocli)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.reflect)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.jackson.dataformat.yaml)

  // For file operations and utilities
  implementation(libs.commons.lang3)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.mockk)
  testImplementation(libs.assertj.core)
  testImplementation(libs.kotlin.test)
}

tasks.withType<Jar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  manifest { attributes["Main-Class"] = "com.axians.eaf.cli.AcciEafCliApplication" }
  from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

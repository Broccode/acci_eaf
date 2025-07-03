rootProject.name = "acci-eaf-monorepo"

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("com.fueledbycaffeine.spotlight") version "1.2.1"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

// Project includes are now managed by Spotlight via gradle/all-projects.txt

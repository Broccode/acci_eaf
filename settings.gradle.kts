rootProject.name = "acci-eaf-monorepo"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// Include backend applications
include(":apps:iam-service")

// Include backend libraries
include(":libs:eaf-sdk")
include(":libs:eaf-sdk:eaf-core")
include(":libs:eaf-sdk:eaf-eventing-sdk")
include(":libs:eaf-sdk:eaf-eventsourcing-sdk")

// Include tools
// include(":tools:acci-eaf-cli")  // Will be uncommented when ready

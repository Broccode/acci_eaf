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
include(":libs:eaf-core")
include(":libs:eaf-eventing-sdk")
include(":libs:eaf-eventsourcing-sdk")
include(":libs:eaf-iam-client")

// Include tools
// include(":tools:acci-eaf-cli")  // Will be uncommented when ready

package com.axians.eaf.testservice.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests to enforce hexagonal architecture rules.
 * Uncomment and customize the rules as needed.
 */
class ArchitectureTest {

    private val importedClasses = ClassFileImporter().importPackages("com.axians.eaf.testservice")

    @Test
    fun `domain layer should not depend on infrastructure layer`() {
        classes()
            .that().resideInAPackage("com.axians.eaf.testservice.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.axians.eaf.testservice.domain..",
                "java..",
                "kotlin..",
                "org.jetbrains.annotations.."
            )
            .check(importedClasses)
    }

    @Test
    fun `application layer should not depend on infrastructure layer`() {
        classes()
            .that().resideInAPackage("com.axians.eaf.testservice.application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.axians.eaf.testservice.domain..",
                "com.axians.eaf.testservice.application..",
                "java..",
                "kotlin..",
                "org.springframework.stereotype..",
                "org.jetbrains.annotations.."
            )
            .check(importedClasses)
    }

    // TODO: Add more architectural rules as needed
    // Examples:
    // - Controllers should only be in web package
    // - Repositories should only be in persistence package
    // - Services should be annotated with @Service
    // - Domain objects should not have Spring annotations
}
package com.axians.eaf.core.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Simple ArchUnit test to verify the setup works.
 */
class SimpleArchTest {

    @Test
    fun `domain layer should not depend on infrastructure when it exists`() {
        val classes = ClassFileImporter().importPackages("com.axians.eaf")

        val rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("Domain layer should not depend on infrastructure layer")
            .allowEmptyShould(true)

        // This test will pass if no domain layer exists yet, which is fine for MVP
        rule.check(classes)
    }

    @Test
    fun `Spring framework should not leak into domain layer`() {
        val classes = ClassFileImporter().importPackages("com.axians.eaf")

        val rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .because("Domain layer should be framework-agnostic")
            .allowEmptyShould(true)

        rule.check(classes)
    }
}

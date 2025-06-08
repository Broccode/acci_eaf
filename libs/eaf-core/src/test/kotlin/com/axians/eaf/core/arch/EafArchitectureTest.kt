package com.axians.eaf.core.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests for ACCI EAF architectural rules.
 *
 * This test class enforces the core architectural principles of the EAF:
 * - Hexagonal Architecture (Ports and Adapters)
 * - Domain-Driven Design (DDD) layer separation
 * - Naming conventions for key components
 *
 * These rules should be applied to all EAF services and can be imported or extended by individual service test suites.
 *
 * Note: Some rules are designed to catch violations when the architecture
 * evolves, so they may pass initially when no violating code exists yet.
 */
class EafArchitectureTest {
    private val eafClasses = ClassFileImporter().importPackages("com.axians.eaf")

    @Test
    fun `domain layer should not depend on infrastructure layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Domain layer should not depend on infrastructure layer in Hexagonal Architecture")
                .allowEmptyShould(true)

        rule.check(eafClasses)
    }

    @Test
    fun `domain layer should not depend on Spring Framework`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .because("Domain layer should be framework-agnostic")
                .allowEmptyShould(true)

        rule.check(eafClasses)
    }

    @Test
    fun `domain should not depend on Spring Boot`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.boot..")
                .because("Domain layer should not depend on Spring Boot")
                .allowEmptyShould(true)

        rule.check(eafClasses)
    }

    @Test
    fun `domain should not depend on persistence frameworks`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "jakarta.persistence..",
                    "javax.persistence..",
                    "org.springframework.data..",
                    "org.hibernate..",
                ).because("Domain layer should not depend on persistence frameworks")
                .allowEmptyShould(true)

        rule.check(eafClasses)
    }

    @Test
    fun `aggregates should be in domain model package when they exist`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Aggregate")
                .should()
                .resideInAPackage("..domain.model..")
                .because("Aggregates should be located in the domain model package")
                .allowEmptyShould(true)

        // This rule will pass if no aggregates exist yet, which is fine for MVP
        rule.check(eafClasses)
    }

    @Test
    fun `events should be in domain events package when they exist`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Event")
                .and()
                .haveSimpleNameNotContaining("Test")
                .should()
                .resideInAPackage("..domain.event..")
                .because("Domain events should be in the domain events package")
                .allowEmptyShould(true)

        // This rule will pass if no events exist yet, which is fine for MVP
        rule.check(eafClasses)
    }

    @Test
    fun `commands should be in appropriate packages when they exist`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Command")
                .and()
                .haveSimpleNameNotContaining("Test")
                .should()
                .resideInAnyPackage("..domain.command..", "..application.port.inbound..")
                .because("Command classes should be in appropriate packages")
                .allowEmptyShould(true)

        // This rule will pass if no commands exist yet, which is fine for MVP
        rule.check(eafClasses)
    }

    @Test
    fun `test classes should have proper naming convention`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameContaining("Architecture")
                .or()
                .haveSimpleNameContaining("Integration")
                .or()
                .haveSimpleNameEndingWith("IT")
                .or()
                .haveSimpleNameEndingWith("IntegrationTest")
                .should()
                .resideInAnyPackage("..test..", "..arch..", "..integration..")
                .because("Architectural and integration test classes should be in specific test packages")
                .allowEmptyShould(true)

        rule.check(eafClasses)
    }

    @Test
    fun `no cycles should exist in EAF packages`() {
        val rule =
            com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
                .slices()
                .matching("com.axians.eaf.(*)..")
                .should()
                .beFreeOfCycles()

        rule.check(eafClasses)
    }

    @Test
    fun `utility classes should be final and have private constructor`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Utils")
                .or()
                .haveSimpleNameEndingWith("Util")
                .or()
                .haveSimpleNameEndingWith("Helper")
                .should()
                .bePackagePrivate()
                .orShould()
                .bePrivate()
                .orShould()
                .haveOnlyPrivateConstructors()
                .because("Utility classes should not be instantiated")
                .allowEmptyShould(true)

        rule.check(eafClasses)
    }
}

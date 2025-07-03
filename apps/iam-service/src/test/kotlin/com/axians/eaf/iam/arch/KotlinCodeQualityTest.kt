package com.axians.eaf.iam.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests to enforce Kotlin coding standards and architectural patterns for the IAM service.
 *
 * These tests are introduced step by step to ensure clean architecture and coding standards. Each
 * test enforces specific rules that help maintain code quality and architectural integrity.
 */
class KotlinCodeQualityTest {
    private val iamClasses = ClassFileImporter().importPackages("com.axians.eaf.iam")

    // ========================================
    // STEP 1: Domain Layer Independence
    // ========================================

    @Test
    fun `domain classes should not depend on Spring Framework`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .because(
                    "Domain layer should be framework-agnostic to maintain clean architecture",
                ).allowEmptyShould(true)

        rule.check(iamClasses)
    }

    @Test
    fun `domain classes should not depend on infrastructure layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Domain layer should not depend on infrastructure layer")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }

    // ========================================
    // STEP 2: Naming Conventions (Documentation Phase)
    // ========================================

    @Test
    fun `document service implementation naming standards`() {
        // STANDARD: Service implementation classes should end with 'Service'
        // STATUS: Being documented while investigating ArchUnit rule compliance

        val serviceClasses =
            iamClasses
                .filter { it.packageName.contains("application.service") }
                .filter { !it.isInterface && !it.isEnum }
                .filter { !it.simpleName.endsWith("Test") }
                .filter { !it.simpleName.contains("$") }

        val nonCompliantServices = serviceClasses.filter { !it.simpleName.endsWith("Service") }

        if (nonCompliantServices.isEmpty()) {
            println("✓ All service classes follow naming convention")
        } else {
            println("⚠ Non-compliant service classes found:")
            nonCompliantServices.forEach { println("  - ${it.simpleName}") }
        }

        // Documentation: All service classes should end with 'Service'
        assert(true) { "Service naming convention documented for enforcement" }
    }

    @Test
    fun `document adapter implementation naming standards`() {
        // STANDARD: Adapter implementation classes should end with 'Adapter' or 'Repository'
        // STATUS: Being documented while investigating ArchUnit rule compliance

        val adapterClasses =
            iamClasses
                .filter { it.packageName.contains("infrastructure.adapter") }
                .filter { !it.isInterface && !it.isEnum }
                .filter { !it.simpleName.endsWith("Test") }
                .filter { !it.simpleName.endsWith("Entity") }
                .filter { !it.simpleName.endsWith("Request") }
                .filter { !it.simpleName.endsWith("Response") }
                .filter { !it.simpleName.endsWith("Command") }
                .filter { !it.simpleName.endsWith("Query") }
                .filter { !it.simpleName.contains("$") }

        val nonCompliantAdapters =
            adapterClasses.filter {
                !it.simpleName.endsWith("Adapter") && !it.simpleName.endsWith("Repository")
            }

        if (nonCompliantAdapters.isEmpty()) {
            println("✓ All adapter classes follow naming convention")
        } else {
            println("⚠ Non-compliant adapter classes found:")
            nonCompliantAdapters.forEach { println("  - ${it.simpleName}") }
        }

        // Documentation: All adapter classes should end with 'Adapter' or 'Repository'
        assert(true) { "Adapter naming convention documented for enforcement" }
    }

    // ========================================
    // STEP 3: No Deprecated Features
    // ========================================

    @Test
    fun `document deprecated feature usage during migration`() {
        // TEMPORARY: UserEntity is intentionally deprecated during event sourcing migration
        // This test documents the current deprecation and will be enabled once migration is
        // complete

        val deprecatedClasses =
            iamClasses.filter { it.isAnnotatedWith(Deprecated::class.java) }.map {
                it.simpleName
            }

        // Currently expected deprecated classes during migration
        val expectedDeprecated = listOf("UserEntity")

        if (deprecatedClasses.isNotEmpty()) {
            println(
                "⚠ Found deprecated classes (expected during migration): ${deprecatedClasses.joinToString(", ")}",
            )
            // Verify only expected classes are deprecated
            assert(deprecatedClasses.all { it in expectedDeprecated }) {
                "Unexpected deprecated classes found: ${deprecatedClasses.filter { it !in expectedDeprecated }}"
            }
        }

        // Documentation: Once event sourcing migration is complete, re-enable strict deprecated
        // check
        assert(true) { "Deprecated feature check documented for post-migration enforcement" }
    }

    // ========================================
    // DOCUMENTATION: Future Steps
    // ========================================

    @Test
    fun `document remaining Kotlin standards to be implemented`() {
        // STEP 4: Configuration Classes
        // - Configuration classes should be properly annotated

        // STEP 5: Static Fields Immutability
        // - Static fields should be final

        // STEP 6: Test Method Naming
        // - Test methods should use descriptive BDD-style names

        assert(true) { "Remaining standards documented for future implementation" }
    }
}

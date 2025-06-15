package com.axians.eaf.core.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests to enforce Kotlin coding standards based on Unity Technologies guide.
 *
 * These tests complement the existing architectural rules by focusing on
 * Kotlin-specific coding practices for improved readability, safety, and maintainability.
 */
class KotlinCodingStandardsTest {
    private val kotlinClasses =
        ClassFileImporter()
            .importPackages("com.axians.eaf")

    @Test
    fun `event classes should be immutable`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Event")
                .should()
                .bePackagePrivate()
                .orShould()
                .bePublic()
                .because("Event classes should be accessible for domain events")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `utility classes should have private constructors`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Utils")
                .should()
                .haveOnlyPrivateConstructors()
                .because("Utility classes should not be instantiated")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `domain classes should not depend on Spring Framework`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..")
                .because("Domain layer should be framework-agnostic")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `service classes should use proper naming`() {
        val rule =
            classes()
                .that()
                .resideInAPackage("..application.service..")
                .and()
                .areNotInterfaces()
                .should()
                .haveSimpleNameEndingWith("Service")
                .because("Service implementation classes should end with 'Service'")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `no deprecated features should be used`() {
        val rule =
            noClasses()
                .should()
                .dependOnClassesThat()
                .areAnnotatedWith(Deprecated::class.java)
                .because("Deprecated features should not be used")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `static fields should be final`() {
        val rule =
            fields()
                .that()
                .areStatic()
                .and()
                .arePublic()
                .should()
                .beFinal()
                .because("Static fields should be immutable")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `configuration classes should be annotated properly`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Configuration")
                .or()
                .haveSimpleNameEndingWith("Config")
                .should()
                .beAnnotatedWith("org.springframework.context.annotation.Configuration")
                .orShould()
                .beAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
                .because("Configuration classes should be properly annotated")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }

    @Test
    fun `test methods should be descriptive`() {
        val rule =
            methods()
                .that()
                .areAnnotatedWith("org.junit.jupiter.api.Test")
                .should()
                .haveNameMatching(".*should.*")
                .because("Test methods should use descriptive names")
                .allowEmptyShould(true)

        rule.check(kotlinClasses)
    }
}

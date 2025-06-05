package com.axians.eaf.iam.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests specific to the IAM Service.
 *
 * This test class extends the base EAF architectural rules with
 * IAM-specific requirements and constraints.
 */
class IamServiceArchitectureTest {
    private val iamClasses = ClassFileImporter().importPackages("com.axians.eaf.iam")

    // IAM-specific architectural rules

    @Test
    fun `IAM domain should not depend on application layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.iam.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.axians.eaf.iam.application..")
                .because("Domain layer should not depend on application layer")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }

    @Test
    fun `IAM domain should not depend on infrastructure layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.iam.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.axians.eaf.iam.infrastructure..")
                .because("Domain layer should not depend on infrastructure layer")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }

    @Test
    fun `IAM controllers should be in web layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .resideInAPackage("com.axians.eaf.iam.web..")
                .because("Controllers should be in the web layer")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }

    @Test
    fun `IAM repositories should be in infrastructure layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .should()
                .resideInAPackage("com.axians.eaf.iam.infrastructure..")
                .because("Repository implementations should be in infrastructure layer")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }

    @Test
    fun `IAM services should be in application layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Service")
                .and()
                .areNotInterfaces()
                .should()
                .resideInAPackage("com.axians.eaf.iam.application..")
                .because("Service implementations should be in application layer")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }

    @Test
    fun `IAM domain entities should not use JPA annotations`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.iam.domain..")
                .should()
                .beAnnotatedWith("jakarta.persistence.Entity")
                .because("Domain entities should be persistence-agnostic")
                .allowEmptyShould(true)

        rule.check(iamClasses)
    }
}

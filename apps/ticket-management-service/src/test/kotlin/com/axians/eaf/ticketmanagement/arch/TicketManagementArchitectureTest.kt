package com.axians.eaf.ticketmanagement.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests for the Ticket Management Service.
 *
 * Enforces hexagonal architecture and DDD principles specific to
 * the ticket management domain.
 */
class TicketManagementArchitectureTest {
    private val ticketClasses = ClassFileImporter().importPackages("com.axians.eaf.ticketmanagement")

    // Hexagonal Architecture Rules

    @Test
    fun `domain should not depend on application layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.axians.eaf.ticketmanagement.application..")
                .because("Domain layer should not depend on application layer")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `domain should not depend on infrastructure layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.axians.eaf.ticketmanagement.infrastructure..")
                .because("Domain layer should not depend on infrastructure layer")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `application should not depend on infrastructure layer`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.axians.eaf.ticketmanagement.infrastructure..")
                .because("Application layer should only depend on domain ports")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    // Component Placement Rules

    @Test
    fun `controllers should be in infrastructure inbound adapter layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound..")
                .because("Controllers are inbound adapters in hexagonal architecture")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `repositories should be in infrastructure outbound adapter layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .and()
                .areNotInterfaces()
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound..")
                .because("Repository implementations are outbound adapters")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `command handlers should be in application layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("CommandHandler")
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.application..")
                .because("Command handlers orchestrate domain operations")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `query handlers should be in application layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("QueryHandler")
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.application..")
                .because("Query handlers provide read operations")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    // DDD Rules

    @Test
    fun `aggregates should be in domain layer`() {
        val rule =
            classes()
                .that()
                .areAnnotatedWith("com.axians.eaf.eventsourcing.annotation.EafAggregate")
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain.aggregate..")
                .because("Aggregates are core domain entities")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `domain events should be in domain layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Event")
                .and()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain..")
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain.event..")
                .because("Domain events belong in the domain layer")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `commands should be in domain layer`() {
        val rule =
            classes()
                .that()
                .haveSimpleNameEndingWith("Command")
                .and()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain..")
                .should()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain.command..")
                .because("Commands are domain concepts")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    // Domain Purity Rules

    @Test
    fun `domain should not use Spring annotations`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain..")
                .should()
                .beAnnotatedWith("org.springframework.stereotype.Component")
                .orShould()
                .beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould()
                .beAnnotatedWith("org.springframework.stereotype.Repository")
                .because("Domain layer should be framework-agnostic")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `domain should not use JPA annotations`() {
        val rule =
            noClasses()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain..")
                .should()
                .beAnnotatedWith("jakarta.persistence.Entity")
                .orShould()
                .beAnnotatedWith("jakarta.persistence.Table")
                .because("Domain entities should be persistence-agnostic")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    // Ports and Adapters Rules

    @Test
    fun `inbound ports should be interfaces in domain layer`() {
        val rule =
            classes()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain.port.inbound..")
                .should()
                .beInterfaces()
                .because("Inbound ports define contracts for application services")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }

    @Test
    fun `outbound ports should be interfaces in domain layer`() {
        val rule =
            classes()
                .that()
                .resideInAPackage("com.axians.eaf.ticketmanagement.domain.port.outbound..")
                .and()
                .haveSimpleNameEndingWith("Repository")
                .or()
                .haveSimpleNameEndingWith("Publisher")
                .or()
                .haveSimpleNameEndingWith("Port")
                .should()
                .beInterfaces()
                .because("Outbound ports define contracts for infrastructure")
                .allowEmptyShould(true)

        rule.check(ticketClasses)
    }
}

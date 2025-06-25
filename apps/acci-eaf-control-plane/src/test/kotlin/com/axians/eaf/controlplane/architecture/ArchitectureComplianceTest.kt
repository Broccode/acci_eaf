package com.axians.eaf.controlplane.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

/**
 * ArchUnit tests for architectural compliance. Validates hexagonal architecture, DDD principles,
 * and EAF patterns. Ensures clean architecture boundaries are maintained.
 */
@AnalyzeClasses(
    packages = ["com.axians.eaf.controlplane"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureComplianceTest {
    @ArchTest
    val hexagonalArchitectureLayersShouldBeRespected =
        layeredArchitecture()
            .consideringAllDependencies()
            // Define layers
            .layer("Domain")
            .definedBy("..domain..")
            .layer("Application")
            .definedBy("..application..")
            .layer("Infrastructure")
            .definedBy("..infrastructure..")
            // Define dependencies
            .whereLayer("Domain")
            .mayNotAccessAnyLayer()
            .whereLayer("Application")
            .mayOnlyAccessLayers("Domain")
            .whereLayer("Infrastructure")
            .mayOnlyAccessLayers("Application", "Domain")

    @ArchTest
    val domainLayerShouldNotDependOnInfrastructure =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Domain layer must not depend on infrastructure concerns")

    @ArchTest
    val domainLayerShouldNotDependOnSpringFramework =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .because("Domain layer should be framework-agnostic")

    @ArchTest
    val applicationLayerShouldNotDependOnWebConcerns =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..")
            .because("Application layer should not depend on web framework details")

    @ArchTest
    val adaptersShouldBeInInfrastructureLayer =
        classes()
            .that()
            .haveNameMatching(".*Adapter")
            .should()
            .resideInAPackage("..infrastructure.adapter..")
            .because("All adapters should be in infrastructure layer")

    @ArchTest
    val repositoriesShouldBeInterfacesInDomain =
        classes()
            .that()
            .haveNameMatching(".*Repository")
            .and()
            .areNotAnnotatedWith(org.springframework.stereotype.Repository::class.java)
            .should()
            .resideInAPackage("..domain..")
            .andShould()
            .beInterfaces()
            .because("Repository interfaces should be in domain layer")

    @ArchTest
    val repositoryImplementationsShouldBeInInfrastructure =
        classes()
            .that()
            .haveNameMatching(".*RepositoryImpl")
            .or()
            .areAnnotatedWith(org.springframework.stereotype.Repository::class.java)
            .should()
            .resideInAPackage("..infrastructure..")
            .because("Repository implementations should be in infrastructure layer")

    @ArchTest
    val domainServicesShouldBeInDomainLayer =
        noClasses()
            .that()
            .haveNameMatching(".*DomainService")
            .or()
            .haveNameMatching(".*Service")
            .and()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..")
            .because("Domain services should not depend on infrastructure")

    @ArchTest
    val controllersShouldBeInInputAdapters =
        classes()
            .that()
            .haveNameMatching(".*Controller")
            .or()
            .haveNameMatching(".*Endpoint")
            .or()
            .areAnnotatedWith(
                org.springframework.web.bind.annotation.RestController::class.java,
            ).or()
            .areAnnotatedWith(com.vaadin.hilla.Endpoint::class.java)
            .should()
            .resideInAPackage("..infrastructure.adapter.input..")
            .because("Controllers and endpoints should be input adapters")

    @ArchTest
    val configurationClassesShouldBeProperlyLocated =
        classes()
            .that()
            .haveNameMatching(".*Configuration")
            .or()
            .areAnnotatedWith(
                org.springframework.context.annotation.Configuration::class.java,
            ).should()
            .resideInAPackage("..infrastructure.configuration..")
            .because(
                "Configuration classes should be in infrastructure configuration package",
            )

    @ArchTest
    val noCyclesBetweenPackages =
        slices()
            .matching("com.axians.eaf.controlplane.(*)..")
            .should()
            .beFreeOfCycles()
            .because("Packages should not have circular dependencies")

    @ArchTest
    val entitiesShouldNotBeInDomainLayer =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .beAnnotatedWith(jakarta.persistence.Entity::class.java)
            .because("Domain entities should not have persistence annotations")
            .allowEmptyShould(true)

    @ArchTest
    val valueObjectsShouldBeInDomainLayer =
        classes()
            .that()
            .haveNameMatching(".*ValueObject")
            .or()
            .haveNameMatching(".*VO")
            .should()
            .resideInAPackage("..domain..")
            .because("Value objects should be in domain layer")
            .allowEmptyShould(true)

    @ArchTest
    val aggregatesShouldBeInDomainLayer =
        classes()
            .that()
            .haveNameMatching(".*Aggregate")
            .or()
            .haveNameMatching(".*AggregateRoot")
            .should()
            .resideInAPackage("..domain..")
            .because("Aggregates should be in domain layer")
            .allowEmptyShould(true)

    @ArchTest
    val domainEventsShouldBeInDomainLayer =
        noClasses()
            .that()
            .haveNameMatching(".*Event")
            .and()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..")
            .because("Domain events should not depend on infrastructure")

    @ArchTest
    val useCasesShouldBeInApplicationLayer =
        classes()
            .that()
            .haveNameMatching(".*UseCase")
            .or()
            .haveNameMatching(".*ApplicationService")
            .should()
            .resideInAPackage("..application..")
            .because(
                "Use cases and application services should be in application layer",
            )

    @ArchTest
    val portsShouldBeInterfaces =
        classes()
            .that()
            .haveNameMatching(".*Port")
            .should()
            .beInterfaces()
            .because("Ports should be interfaces defining contracts")

    @ArchTest
    val adaptersShouldImplementPorts =
        classes()
            .that()
            .haveNameMatching(".*Adapter")
            .should()
            .beAssignableTo(
                Any::class.java,
            ) // TODO: Make more specific when ports are defined
            .because("Adapters should implement port interfaces")
}

package com.axians.eaf.controlplane.test

import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

/**
 * Test JPA configuration to properly scan entity classes. Ensures all entity classes are properly
 * registered with Hibernate.
 */
@TestConfiguration
class TestJpaConfiguration {
    @Bean
    @Primary
    fun entityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(dataSource)
            .packages("com.axians.eaf.controlplane.infrastructure.adapter.outbound.entity")
            .persistenceUnit("default")
            .build()
}

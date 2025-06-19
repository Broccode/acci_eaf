package com.axians.eaf.iam

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
class PostgresTestcontainerConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withMinimumRunningDuration(Duration.ofSeconds(10))
            .withStartupTimeout(Duration.ofMinutes(3))
            .withConnectTimeoutSeconds(60)
}

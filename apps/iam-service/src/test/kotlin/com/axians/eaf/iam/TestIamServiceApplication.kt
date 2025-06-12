package com.axians.eaf.iam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * A dedicated Spring Boot application class for running integration tests.
 *
 * This class is almost identical to the main [IamServiceApplication] but purposefully
 * does **not** exclude [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration].
 * This allows Spring Boot's test-specific mechanisms, like `@ServiceConnection`, to
 * auto-configure a `DataSource` bean pointing to a Testcontainers instance, which is
 * essential for running JPA-based integration tests.
 */
@SpringBootApplication(scanBasePackages = ["com.axians.eaf.iam"])
@EnableJpaRepositories(basePackages = ["com.axians.eaf.iam.infrastructure.adapter.outbound.persistence"])
@EntityScan(basePackages = ["com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity"])
class TestIamServiceApplication

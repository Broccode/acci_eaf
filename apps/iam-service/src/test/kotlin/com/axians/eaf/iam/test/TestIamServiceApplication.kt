package com.axians.eaf.iam.test

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * A dedicated Spring Boot application class for running integration tests.
 *
 * This class is almost identical to the main [IamServiceApplication] but purposefully
 * does **not** exclude [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration].
 * This allows Spring Boot's test-specific mechanisms, like `@ServiceConnection`, to
 * auto-configure a `DataSource` bean pointing to a Testcontainers instance, which is
 * essential for running JPA-based integration tests.
 *
 * Note: We rely on JpaConfig for repository scanning, so we don't duplicate @EnableJpaRepositories here.
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.axians.eaf.iam.application",
        "com.axians.eaf.iam.domain",
        "com.axians.eaf.iam.infrastructure",
        "com.axians.eaf.iam.web",
    ],
)
class TestIamServiceApplication

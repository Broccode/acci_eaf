package com.axians.eaf.iam.infrastructure.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.axians.eaf.iam.infrastructure.adapter.outbound.persistence"],
)
@EntityScan(
    basePackages = ["com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity"],
)
class JpaConfig

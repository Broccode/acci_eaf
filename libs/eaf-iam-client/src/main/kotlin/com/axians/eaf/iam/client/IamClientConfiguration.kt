package com.axians.eaf.iam.client

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/** Configuration for EAF IAM Client SDK. Provides beans needed for IAM service integration. */
@Configuration
class IamClientConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun restTemplate(): RestTemplate = RestTemplate()
}

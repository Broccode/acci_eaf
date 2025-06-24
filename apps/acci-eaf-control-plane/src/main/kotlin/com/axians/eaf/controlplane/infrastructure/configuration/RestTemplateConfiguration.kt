package com.axians.eaf.controlplane.infrastructure.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/** Configuration for REST template used for external service communication. */
@Configuration
class RestTemplateConfiguration {
    @Bean fun restTemplate(): RestTemplate = RestTemplate()
}

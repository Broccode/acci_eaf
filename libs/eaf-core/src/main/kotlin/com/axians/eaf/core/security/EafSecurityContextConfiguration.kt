package com.axians.eaf.core.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Auto-configuration for EAF security context holder.
 * This configuration is automatically activated when Spring Security is on the classpath.
 */
@Configuration
@ConditionalOnClass(SecurityContextHolder::class)
class EafSecurityContextConfiguration {
    /**
     * Provides the default implementation of EafSecurityContextHolder as a Spring bean.
     */
    @Bean
    fun eafSecurityContextHolder(): EafSecurityContextHolder = DefaultEafSecurityContextHolder()
}

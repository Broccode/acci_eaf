package com.axians.eaf.core.tenancy.integration

import com.axians.eaf.core.security.DefaultEafSecurityContextHolder
import com.axians.eaf.core.security.EafSecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Auto-configuration for tenant context integration between Spring Security and
 * TenantContextHolder.
 *
 * This configuration automatically sets up the SecurityTenantContextBridge and
 * TenantContextSynchronizationFilter when the appropriate conditions are met.
 */
@Configuration
@AutoConfiguration
@ConditionalOnClass(SecurityContextHolder::class, OncePerRequestFilter::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "eaf.tenancy.integration",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(TenantContextIntegrationProperties::class)
class TenantContextIntegrationAutoConfiguration {
    private val logger =
        LoggerFactory.getLogger(TenantContextIntegrationAutoConfiguration::class.java)

    /** Provides a default EafSecurityContextHolder if none is configured. */
    @Bean
    @ConditionalOnMissingBean(EafSecurityContextHolder::class)
    fun defaultEafSecurityContextHolder(): EafSecurityContextHolder {
        logger.debug("Creating default EafSecurityContextHolder")
        return DefaultEafSecurityContextHolder()
    }

    /** Creates the SecurityTenantContextBridge component. */
    @Bean
    @ConditionalOnMissingBean(SecurityTenantContextBridge::class)
    fun securityTenantContextBridge(securityContextHolder: EafSecurityContextHolder): SecurityTenantContextBridge {
        logger.debug("Creating SecurityTenantContextBridge")
        return SecurityTenantContextBridge(securityContextHolder)
    }

    /** Creates the TenantContextSynchronizationFilter. */
    @Bean
    @ConditionalOnMissingBean(TenantContextSynchronizationFilter::class)
    fun tenantContextSynchronizationFilter(bridge: SecurityTenantContextBridge): TenantContextSynchronizationFilter {
        logger.debug("Creating TenantContextSynchronizationFilter")
        return TenantContextSynchronizationFilter(bridge)
    }

    /** Registers the TenantContextSynchronizationFilter with proper ordering. */
    @Bean
    @ConditionalOnMissingBean(name = ["tenantContextFilterRegistration"])
    fun tenantContextFilterRegistration(
        filter: TenantContextSynchronizationFilter,
        properties: TenantContextIntegrationProperties,
    ): FilterRegistrationBean<TenantContextSynchronizationFilter> {
        logger.debug(
            "Registering TenantContextSynchronizationFilter with order: {}",
            properties.filterOrder,
        )

        val registration = FilterRegistrationBean(filter)
        registration.order = properties.filterOrder
        registration.setName("tenantContextSynchronizationFilter")

        // Configure URL patterns
        if (properties.urlPatterns.isNotEmpty()) {
            properties.urlPatterns.forEach { pattern -> registration.addUrlPatterns(pattern) }
        } else {
            registration.addUrlPatterns("/*") // Default to all URLs
        }

        return registration
    }

    /** Creates SecurityAwareTenantExecutor for @Async method tenant propagation. */
    @Bean
    @ConditionalOnMissingBean(SecurityAwareTenantExecutor::class)
    @ConditionalOnProperty(
        prefix = "eaf.tenancy.integration",
        name = ["async.enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun securityAwareTenantExecutor(bridge: SecurityTenantContextBridge): SecurityAwareTenantExecutor {
        logger.debug("Creating SecurityAwareTenantExecutor")
        return SecurityAwareTenantExecutor(bridge)
    }

    /** Creates TenantContextTaskDecorator for async operations. */
    @Bean
    @ConditionalOnMissingBean(TenantContextTaskDecorator::class)
    @ConditionalOnProperty(
        prefix = "eaf.tenancy.integration",
        name = ["async.enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun tenantContextTaskDecorator(bridge: SecurityTenantContextBridge): TenantContextTaskDecorator {
        logger.debug("Creating TenantContextTaskDecorator")
        return TenantContextTaskDecorator(bridge)
    }

    /** Creates TenantContextScheduledExecutor for scheduled tasks. */
    @Bean
    @ConditionalOnMissingBean(TenantContextScheduledExecutor::class)
    @ConditionalOnProperty(
        prefix = "eaf.tenancy.integration",
        name = ["scheduled.enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun tenantContextScheduledExecutor(): TenantContextScheduledExecutor {
        logger.debug("Creating TenantContextScheduledExecutor")
        return TenantContextScheduledExecutor()
    }

    /** Creates TenantContextSecurityValidator for security and rate limiting. */
    @Bean
    @ConditionalOnMissingBean(TenantContextSecurityValidator::class)
    @ConditionalOnProperty(
        prefix = "eaf.tenancy.integration",
        name = ["security.enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun tenantContextSecurityValidator(
        properties: TenantContextIntegrationProperties,
    ): TenantContextSecurityValidator {
        logger.debug("Creating TenantContextSecurityValidator")
        return TenantContextSecurityValidator(properties)
    }

    /** Configuration for test profiles that might need different behavior. */
    @Profile("test")
    @Bean
    @ConditionalOnMissingBean(name = ["testTenantContextBridge"])
    fun testTenantContextBridge(securityContextHolder: EafSecurityContextHolder): SecurityTenantContextBridge {
        logger.debug("Creating test-specific SecurityTenantContextBridge")
        return SecurityTenantContextBridge(securityContextHolder)
    }
}

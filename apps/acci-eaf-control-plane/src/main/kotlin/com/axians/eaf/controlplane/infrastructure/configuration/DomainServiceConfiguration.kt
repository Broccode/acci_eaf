package com.axians.eaf.controlplane.infrastructure.configuration

import com.axians.eaf.controlplane.domain.port.AuditEventPublisher
import com.axians.eaf.controlplane.domain.port.AuditRepository
import com.axians.eaf.controlplane.domain.service.AuditService
import com.axians.eaf.core.security.EafSecurityContextHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

/**
 * Configuration for domain services that need to be managed by Spring.
 *
 * This configuration bridges the gap between pure domain services and Spring's dependency injection
 * container, following DDD principles while enabling infrastructure concerns like dependency
 * injection.
 */
@Configuration
class DomainServiceConfiguration {
    /**
     * Creates the AuditService domain service as a Spring bean.
     *
     * The AuditService is a core domain service that needs to be injectable into infrastructure
     * components like endpoints and security aspects.
     */
    @Bean
    @Lazy
    fun auditService(
        @Lazy auditRepository: AuditRepository,
        auditEventPublisher: AuditEventPublisher,
        securityContextHolder: EafSecurityContextHolder,
    ): AuditService =
        AuditService(
            auditRepository = auditRepository,
            auditEventPublisher = auditEventPublisher,
            securityContextHolder = securityContextHolder,
            asyncPublishing = true,
            includeRequestDetails = true,
        )
}

package com.axians.eaf.controlplane.infrastructure.configuration

import com.vaadin.flow.spring.security.VaadinWebSecurity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@EnableWebSecurity
@Configuration
class SecurityConfiguration : VaadinWebSecurity() {
    override fun configure(http: HttpSecurity) {
        super.configure(http)
        setLoginView(http, "/login")
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        val user: UserDetails =
            User
                .withUsername("user")
                .password("{noop}password")
                .roles("USER")
                .build()
        return InMemoryUserDetailsManager(user)
    }
}

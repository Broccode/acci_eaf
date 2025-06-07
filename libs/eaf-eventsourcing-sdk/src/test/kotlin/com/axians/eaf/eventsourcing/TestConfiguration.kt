package com.axians.eaf.eventsourcing

import com.axians.eaf.eventsourcing.adapter.JdbcEventStoreRepository
import com.axians.eaf.eventsourcing.port.EventStoreRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

@TestConfiguration
class TestConfiguration {
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }

    @Bean
    fun eventStoreRepository(jdbcTemplate: NamedParameterJdbcTemplate): EventStoreRepository =
        JdbcEventStoreRepository(jdbcTemplate)
}

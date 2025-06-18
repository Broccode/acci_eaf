package com.axians.eaf.ticketmanagement.infrastructure.adapter.outbound.persistence.user

import com.axians.eaf.ticketmanagement.domain.aggregate.user.Authority
import com.axians.eaf.ticketmanagement.domain.aggregate.user.User
import com.axians.eaf.ticketmanagement.domain.port.user.UserRepository
import org.springframework.stereotype.Repository

@Repository
class InMemoryUserRepository : UserRepository {
    private val users =
        mapOf(
            "user" to
                User(
                    login = "user",
                    password = "{noop}password",
                    authorities = setOf(Authority("ROLE_USER")),
                ),
            "admin" to
                User(
                    login = "admin",
                    password = "{noop}password",
                    authorities = setOf(Authority("ROLE_USER"), Authority("ROLE_ADMIN")),
                ),
        )

    override fun findOneByLogin(login: String): User? = users[login]
}

package com.axians.eaf.ticketmanagement.domain.port.user

import com.axians.eaf.ticketmanagement.domain.aggregate.user.User

interface UserRepository {
    fun findOneByLogin(login: String): User?
}

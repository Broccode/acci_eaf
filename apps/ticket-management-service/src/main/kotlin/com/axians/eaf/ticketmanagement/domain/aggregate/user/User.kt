package com.axians.eaf.ticketmanagement.domain.aggregate.user

data class User(
    val login: String,
    val password: String,
    val authorities: Set<Authority>,
)

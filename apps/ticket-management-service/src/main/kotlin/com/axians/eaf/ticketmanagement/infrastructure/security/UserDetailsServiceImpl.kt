package com.axians.eaf.ticketmanagement.infrastructure.security

import com.axians.eaf.ticketmanagement.domain.port.user.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Implements the Spring Security [UserDetailsService] interface.
 *
 * This service is responsible for loading user-specific data. It is used by the
 * framework to handle authentication and authorization. For the stateful Hilla UI,
 * this service is crucial for the form-based login process.
 */
@Service("userDetailsService")
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
) : UserDetailsService {
    /**
     * Loads a user by their username and returns a [UserDetails] object.
     *
     * In this implementation, it fetches the user from the application's local
     * user repository. This is a key part of integrating with EAF's domain-driven
     * structure. In a more complex scenario, this could involve calls to a remote
     * IAM service.
     *
     * @param username The username of the user to load.
     * @return A [UserDetails] object representing the user.
     * @throws UsernameNotFoundException if the user could not be found.
     */
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findOneByLogin(username)
                ?: throw UsernameNotFoundException("User $username not found")

        val grantedAuthorities = user.authorities.map { SimpleGrantedAuthority(it.name) }.toSet()

        return User(user.login, user.password, grantedAuthorities)
    }
}

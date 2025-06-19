package com.axians.eaf.ticketmanagement

import com.axians.eaf.ticketmanagement.infrastructure.adapter.inbound.hilla.UserInfoService
import com.axians.eaf.ticketmanagement.test.TestTicketManagementApplication
import com.axians.eaf.ticketmanagement.test.TicketManagementTestcontainerConfiguration
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    classes = [TestTicketManagementApplication::class],
    properties = [
        "spring.autoconfigure.exclude=com.vaadin.flow.spring.SpringBootAutoConfiguration,com.vaadin.flow.spring.SpringSecurityAutoConfiguration",
    ],
)
@Testcontainers
@ActiveProfiles("test")
@Import(
    TicketManagementTestcontainerConfiguration::class,
    com.axians.eaf.ticketmanagement.infrastructure.security.SecurityUtils::class,
)
class StatelessSecurityIntegrationTest {
    @MockkBean
    private lateinit var userDetailsService: UserDetailsService

    @Autowired
    private lateinit var userInfoService: UserInfoService

    @Test
    @WithMockUser("testuser", roles = ["USER"])
    fun `given authenticated user, when getUserInfo is called, then user details are returned`() {
        // Arrange
        val username = "testuser"
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        // Note: password is not needed here as authentication is mocked
        val userDetails = User(username, "", authorities)

        every { userDetailsService.loadUserByUsername(username) } returns userDetails

        // Act
        val userInfo = userInfoService.getUserInfo()

        // Assert
        assertThat(userInfo).isNotNull
        assertThat(userInfo.username).isEqualTo(username)
        assertThat(userInfo.name).isEqualTo(username)
        assertThat(userInfo.anonymous).isFalse()
        assertThat(userInfo.roles).contains("ROLE_USER")
    }

    @Test
    fun `given unauthenticated user, when getUserInfo is called, then anonymous user is returned`() {
        // Act
        val userInfo = userInfoService.getUserInfo()

        // Assert
        assertThat(userInfo).isNotNull
        assertThat(userInfo.anonymous).isTrue()
        assertThat(userInfo.username).isNull()
        assertThat(userInfo.name).isNull()
        assertThat(userInfo.roles).isEmpty()
    }
}

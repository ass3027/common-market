package com.helpme.commonmarket.auth.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.helpme.commonmarket.auth.dto.LoginRequest
import com.helpme.commonmarket.config.CustomUserDetailsService
import com.helpme.commonmarket.config.JwtTokenUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AuthController::class)
class AuthControllerBehaviorTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenUtil: JwtTokenUtil,
    private val userDetailsService: CustomUserDetailsService
) {

    @TestConfiguration
    class ControllerTestConfig {
        @Bean
        fun authenticationManager() = mockk<AuthenticationManager>()
        
        @Bean
        fun jwtTokenUtil() = mockk<JwtTokenUtil>()
        
        @Bean
        fun customUserDetailsService() = mockk<CustomUserDetailsService>()
    }

    @Test
    fun `successful login returns complete JWT response structure`() {
        // Arrange
        val credentials = LoginRequest("1", "admin123")
        val userDetails = createUserWithRole("1", "ROLE_ADMIN")
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

        mockSuccessfulAuthentication(credentials.username, userDetails, jwtToken)

        // Act & Assert - Focus on complete response structure
        mockMvc.perform(loginRequest(credentials))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value(jwtToken))
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.userId").value("1"))
            .andExpect(jsonPath("$.roles").isArray)
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
    }

    @Test
    fun `invalid credentials return proper error response`() {
        // Arrange
        val invalidCredentials = LoginRequest("1", "wrongpassword")
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")

        // Act & Assert - Focus on error handling behavior
        mockMvc.perform(loginRequest(invalidCredentials))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid username or password"))
    }

    @Test
    fun `authentication service failure returns generic error message`() {
        // Arrange
        val credentials = LoginRequest("1", "admin123")
        every { authenticationManager.authenticate(any()) } returns mockk()
        every { userDetailsService.loadUserByUsername("1") } throws RuntimeException("Service error")

        // Act & Assert - Focus on graceful error handling
        mockMvc.perform(loginRequest(credentials))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Authentication failed"))
    }

    @Test
    fun `different user roles are correctly represented in response`() {
        // Arrange
        val userCredentials = LoginRequest("2", "user123")
        val userDetails = createUserWithRole("2", "ROLE_USER")
        val userToken = "user.jwt.token"

        mockSuccessfulAuthentication(userCredentials.username, userDetails, userToken)

        // Act & Assert - Focus on role-based response behavior
        mockMvc.perform(loginRequest(userCredentials))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value(userToken))
            .andExpect(jsonPath("$.userId").value("2"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
    }

    @Test
    fun `user with multiple roles returns all roles in response`() {
        // Arrange
        val credentials = LoginRequest("3", "multiuser123")
        val userDetails = createUserWithRoles("3", listOf("ROLE_USER", "ROLE_ADMIN"))
        val token = "multi.role.token"

        mockSuccessfulAuthentication(credentials.username, userDetails, token)

        // Act & Assert - Focus on multiple role handling
        mockMvc.perform(loginRequest(credentials))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roles").isArray)
            .andExpect(jsonPath("$.roles.length()").value(2))
    }

    @Test
    fun `malformed request returns validation error`() {
        // Arrange
        val invalidJson = """{"invalidField": "value"}"""

        // Act & Assert - Focus on input validation behavior
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `empty credentials are handled appropriately`() {
        // Arrange
        val emptyCredentials = LoginRequest("", "")
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")

        // Act & Assert - Focus on empty input handling
        mockMvc.perform(loginRequest(emptyCredentials))
            .andExpect(status().isBadRequest)
    }

    // Helper methods to reduce test code duplication and focus on behavior
    private fun createUserWithRole(username: String, role: String): UserDetails {
        return User.builder()
            .username(username)
            .password("encodedPassword")
            .authorities(listOf(SimpleGrantedAuthority(role)))
            .build()
    }

    private fun createUserWithRoles(username: String, roles: List<String>): UserDetails {
        return User.builder()
            .username(username)
            .password("encodedPassword")
            .authorities(roles.map { SimpleGrantedAuthority(it) })
            .build()
    }

    private fun mockSuccessfulAuthentication(username: String, userDetails: UserDetails, token: String) {
        every { authenticationManager.authenticate(any()) } returns mockk()
        every { userDetailsService.loadUserByUsername(username) } returns userDetails
        every { jwtTokenUtil.generateToken(userDetails) } returns token
    }

    private fun loginRequest(credentials: LoginRequest) = post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(credentials))
}
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
class AuthControllerTest @Autowired constructor(
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
        val loginRequest = LoginRequest("1", "admin123")
        val userDetails: UserDetails = User.builder()
            .username("1")
            .password("encodedPassword")
            .authorities(listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
            .build()
        val expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

        every { authenticationManager.authenticate(any()) } returns mockk()
        every { userDetailsService.loadUserByUsername("1") } returns userDetails
        every { jwtTokenUtil.generateToken(userDetails) } returns expectedToken

        // Act & Assert - Focus on complete response structure and behavior
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value(expectedToken))
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.userId").value("1"))
            .andExpect(jsonPath("$.roles").isArray)
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
    }

    @Test
    fun `invalid credentials return proper error response`() {
        // Arrange
        val loginRequest = LoginRequest("1", "wrongpassword")
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")

        // Act & Assert - Focus on error handling behavior
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid username or password"))
    }

    @Test
    fun `login should return 400 for authentication exception`() {
        val loginRequest = LoginRequest("1", "admin123")

        every { authenticationManager.authenticate(any()) } returns mockk()
        every { userDetailsService.loadUserByUsername("1") } throws RuntimeException("Service error")

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Authentication failed"))
    }

    @Test
    fun `login should handle user with USER role`() {
        val loginRequest = LoginRequest("2", "user123")
        val userDetails: UserDetails = User.builder()
            .username("2")
            .password("encodedPassword")
            .authorities(listOf(SimpleGrantedAuthority("ROLE_USER")))
            .build()
        val expectedToken = "user.jwt.token"

        every { authenticationManager.authenticate(any()) } returns mockk()
        every { userDetailsService.loadUserByUsername("2") } returns userDetails
        every { jwtTokenUtil.generateToken(userDetails) } returns expectedToken

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value(expectedToken))
            .andExpect(jsonPath("$.userId").value("2"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
    }

    @Test
    fun `login should validate request format`() {
        val invalidJson = """{"invalidField": "value"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
    }
}
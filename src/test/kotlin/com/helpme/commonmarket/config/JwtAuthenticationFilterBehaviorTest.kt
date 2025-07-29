package com.helpme.commonmarket.config

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtAuthenticationFilterBehaviorTest {

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    private lateinit var jwtTokenUtil: JwtTokenUtil
    private lateinit var customUserDetailsService: CustomUserDetailsService
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        jwtTokenUtil = mockk()
        customUserDetailsService = mockk()
        jwtAuthenticationFilter = JwtAuthenticationFilter(jwtTokenUtil, customUserDetailsService)
        
        request = mockk()
        response = mockk()
        filterChain = mockk()
        
        // Clear security context before each test
        SecurityContextHolder.clearContext()
        
        // Mock filter chain to continue processing
        every { filterChain.doFilter(request, response) } returns Unit
    }

    @Test
    fun `valid JWT token creates authenticated security context`() {
        // Arrange
        val validToken = "valid.jwt.token"
        val username = "1"
        val roles = listOf("ROLE_ADMIN")

        every { request.getHeader("Authorization") } returns "Bearer $validToken"
        every { jwtTokenUtil.getUsernameFromToken(validToken) } returns username
        every { jwtTokenUtil.validateToken(validToken) } returns true
        every { jwtTokenUtil.getRolesFromToken(validToken) } returns roles

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on security context state
        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication, "Authentication should be set in security context")
        assertEquals(username, authentication.name, "Authentication should have correct username")
        assertEquals(1, authentication.authorities.size, "Should have correct number of authorities")
        assertEquals("ROLE_ADMIN", authentication.authorities.first().authority, "Should have correct role")
    }

    @Test
    fun `missing authorization header leaves security context empty`() {
        // Arrange
        every { request.getHeader("Authorization") } returns null

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on security context state
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication, "Security context should remain empty without authorization header")
    }

    @Test
    fun `malformed authorization header leaves security context empty`() {
        // Arrange
        every { request.getHeader("Authorization") } returns "InvalidFormat token"

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on security context state
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication, "Security context should remain empty with malformed header")
    }

    @Test
    fun `invalid JWT token leaves security context empty`() {
        // Arrange
        val invalidToken = "invalid.jwt.token"
        val username = "1"

        every { request.getHeader("Authorization") } returns "Bearer $invalidToken"
        every { jwtTokenUtil.getUsernameFromToken(invalidToken) } returns username
        every { jwtTokenUtil.validateToken(invalidToken) } returns false

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on security context state
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication, "Security context should remain empty with invalid token")
    }

    @Test
    fun `malformed JWT token leaves security context empty`() {
        // Arrange
        val malformedToken = "malformed.token"

        every { request.getHeader("Authorization") } returns "Bearer $malformedToken"
        every { jwtTokenUtil.getUsernameFromToken(malformedToken) } throws RuntimeException("Invalid token")

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on security context state
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication, "Security context should remain empty with malformed token")
    }

    @Test
    fun `multiple roles are correctly set in security context`() {
        // Arrange
        val validToken = "valid.jwt.token"
        val username = "1"
        val multipleRoles = listOf("ROLE_USER", "ROLE_ADMIN")

        every { request.getHeader("Authorization") } returns "Bearer $validToken"
        every { jwtTokenUtil.getUsernameFromToken(validToken) } returns username
        every { jwtTokenUtil.validateToken(validToken) } returns true
        every { jwtTokenUtil.getRolesFromToken(validToken) } returns multipleRoles

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on multiple roles in security context
        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication, "Authentication should be set")
        assertEquals(2, authentication.authorities.size, "Should have multiple authorities")
        
        val authorityNames = authentication.authorities.map { it.authority }.toSet()
        assertEquals(setOf("ROLE_USER", "ROLE_ADMIN"), authorityNames, "Should contain all roles")
    }

    @Test
    fun `existing authentication is preserved`() {
        // Arrange - Pre-set authentication
        val existingAuth = mockk<org.springframework.security.core.Authentication>()
        SecurityContextHolder.getContext().authentication = existingAuth

        val validToken = "valid.jwt.token"
        val username = "1"

        every { request.getHeader("Authorization") } returns "Bearer $validToken"
        every { jwtTokenUtil.getUsernameFromToken(validToken) } returns username

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on preserving existing authentication
        val authentication = SecurityContextHolder.getContext().authentication
        assertEquals(existingAuth, authentication, "Existing authentication should be preserved")
    }

    @Test
    fun `token validation exception is handled gracefully`() {
        // Arrange
        val validToken = "valid.jwt.token"
        val username = "1"

        every { request.getHeader("Authorization") } returns "Bearer $validToken"
        every { jwtTokenUtil.getUsernameFromToken(validToken) } returns username
        every { jwtTokenUtil.validateToken(validToken) } throws RuntimeException("Validation error")

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on graceful error handling
        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication, "Security context should remain empty when validation fails")
    }

    @Test
    fun `empty roles are handled properly`() {
        // Arrange
        val validToken = "valid.jwt.token"
        val username = "1"
        val emptyRoles = emptyList<String>()

        every { request.getHeader("Authorization") } returns "Bearer $validToken"
        every { jwtTokenUtil.getUsernameFromToken(validToken) } returns username
        every { jwtTokenUtil.validateToken(validToken) } returns true
        every { jwtTokenUtil.getRolesFromToken(validToken) } returns emptyRoles

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        // Assert - Focus on empty roles handling
        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication, "Authentication should still be set with empty roles")
        assertEquals(username, authentication.name, "Should have correct username")
        assertEquals(0, authentication.authorities.size, "Should have no authorities")
    }
}
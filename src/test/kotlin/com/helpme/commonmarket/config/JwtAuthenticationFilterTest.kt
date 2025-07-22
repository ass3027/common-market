package com.helpme.commonmarket.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtAuthenticationFilterTest {

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
        
        // Clear security context
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `filter should set authentication for valid JWT token`() {
        val token = "valid.jwt.token"
        val username = "1"
        val roles = listOf("ROLE_ADMIN")

        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenUtil.getUsernameFromToken(token) } returns username
        every { jwtTokenUtil.validateToken(token) } returns true
        every { jwtTokenUtil.getRolesFromToken(token) } returns roles
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(username, authentication.name)
        assertEquals(1, authentication.authorities.size)
        assertEquals("ROLE_ADMIN", authentication.authorities.first().authority)

        verify(exactly = 1) { jwtTokenUtil.getUsernameFromToken(token) }
        verify(exactly = 1) { jwtTokenUtil.validateToken(token) }
        verify(exactly = 1) { jwtTokenUtil.getRolesFromToken(token) }
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should not set authentication for missing Authorization header`() {
        every { request.getHeader("Authorization") } returns null
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)

        verify(exactly = 0) { jwtTokenUtil.getUsernameFromToken(any()) }
        verify(exactly = 0) { jwtTokenUtil.validateToken(any()) }
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should not set authentication for invalid Authorization header format`() {
        every { request.getHeader("Authorization") } returns "InvalidFormat token"
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)

        verify(exactly = 0) { jwtTokenUtil.getUsernameFromToken(any()) }
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should not set authentication for malformed JWT token`() {
        val invalidToken = "malformed.token"

        every { request.getHeader("Authorization") } returns "Bearer $invalidToken"
        every { jwtTokenUtil.getUsernameFromToken(invalidToken) } throws RuntimeException("Invalid token")
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)

        verify(exactly = 1) { jwtTokenUtil.getUsernameFromToken(invalidToken) }
        verify(exactly = 0) { jwtTokenUtil.validateToken(any()) }
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should not set authentication for invalid JWT token`() {
        val token = "invalid.jwt.token"
        val username = "1"

        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenUtil.getUsernameFromToken(token) } returns username
        every { jwtTokenUtil.validateToken(token) } returns false
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)

        verify(exactly = 1) { jwtTokenUtil.getUsernameFromToken(token) }
        verify(exactly = 1) { jwtTokenUtil.validateToken(token) }
        verify(exactly = 0) { jwtTokenUtil.getRolesFromToken(any()) }
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should handle multiple roles correctly`() {
        val token = "valid.jwt.token"
        val username = "1"
        val roles = listOf("ROLE_USER", "ROLE_ADMIN")

        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenUtil.getUsernameFromToken(token) } returns username
        every { jwtTokenUtil.validateToken(token) } returns true
        every { jwtTokenUtil.getRolesFromToken(token) } returns roles
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNotNull(authentication)
        assertEquals(2, authentication.authorities.size)
        
        val authorityNames = authentication.authorities.map { it.authority }.toSet()
        assertEquals(setOf("ROLE_USER", "ROLE_ADMIN"), authorityNames)
    }

    @Test
    fun `filter should not override existing authentication`() {
        val token = "valid.jwt.token"
        val username = "1"
        
        // Set existing authentication
        SecurityContextHolder.getContext().authentication = mockk()

        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenUtil.getUsernameFromToken(token) } returns username
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        verify(exactly = 1) { jwtTokenUtil.getUsernameFromToken(token) }
        verify(exactly = 0) { jwtTokenUtil.validateToken(any()) }
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `filter should handle validation exception gracefully`() {
        val token = "valid.jwt.token"
        val username = "1"

        every { request.getHeader("Authorization") } returns "Bearer $token"
        every { jwtTokenUtil.getUsernameFromToken(token) } returns username
        every { jwtTokenUtil.validateToken(token) } throws RuntimeException("Validation error")
        every { filterChain.doFilter(request, response) } returns Unit

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertNull(authentication)

        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }
}
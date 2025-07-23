package com.helpme.commonmarket.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.util.ReflectionTestUtils
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtTokenUtilBehaviorTest {

    private lateinit var jwtTokenUtil: JwtTokenUtil

    @BeforeEach
    fun setUp() {
        jwtTokenUtil = JwtTokenUtil()
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation2024")
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpiration", 86400L) // 24 hours
    }

    @Test
    fun `generated token contains correct user information`() {
        // Arrange
        val userDetails = createUserWithRole("1", "ROLE_ADMIN")

        // Act
        val token = jwtTokenUtil.generateToken(userDetails)

        // Assert - Focus on token content and behavior
        assertTrue(token.isNotEmpty(), "Token should be generated")
        assertTrue(token.contains("."), "JWT should have proper format")
        assertEquals(3, token.split(".").size, "JWT should have 3 parts")
        
        // Verify token contains correct information
        assertEquals("1", jwtTokenUtil.getUsernameFromToken(token))
        assertEquals(listOf("ROLE_ADMIN"), jwtTokenUtil.getRolesFromToken(token))
    }

    @Test
    fun `token expiration is set to future date`() {
        // Arrange
        val userDetails = createUserWithRole("1", "ROLE_USER")
        val beforeGeneration = Date()

        // Act
        val token = jwtTokenUtil.generateToken(userDetails)
        val expiration = jwtTokenUtil.getExpirationDateFromToken(token)

        // Assert - Focus on expiration behavior
        assertTrue(expiration.after(beforeGeneration), "Token should expire in the future")
        assertTrue(jwtTokenUtil.validateToken(token), "Newly generated token should be valid")
    }

    @Test
    fun `valid token passes validation`() {
        // Arrange
        val userDetails = createUserWithRole("2", "ROLE_USER")
        val token = jwtTokenUtil.generateToken(userDetails)

        // Act & Assert - Focus on validation behavior
        assertTrue(jwtTokenUtil.validateToken(token), "Valid token should pass validation")
        assertTrue(jwtTokenUtil.validateToken(token, userDetails), "Token should validate against correct user")
    }

    @Test
    fun `token validates against correct user only`() {
        // Arrange
        val user1 = createUserWithRole("1", "ROLE_ADMIN")
        val user2 = createUserWithRole("2", "ROLE_USER")
        val token = jwtTokenUtil.generateToken(user1)

        // Act & Assert - Focus on user matching behavior
        assertTrue(jwtTokenUtil.validateToken(token, user1), "Token should validate against correct user")
        assertFalse(jwtTokenUtil.validateToken(token, user2), "Token should not validate against different user")
    }

    @Test
    fun `multiple roles are correctly stored and retrieved`() {
        // Arrange
        val userDetails = createUserWithMultipleRoles("3", listOf("ROLE_USER", "ROLE_ADMIN"))

        // Act
        val token = jwtTokenUtil.generateToken(userDetails)
        val retrievedRoles = jwtTokenUtil.getRolesFromToken(token)

        // Assert - Focus on multiple role handling behavior
        assertEquals(2, retrievedRoles.size, "Should retrieve all roles")
        assertTrue(retrievedRoles.contains("ROLE_USER"), "Should contain USER role")
        assertTrue(retrievedRoles.contains("ROLE_ADMIN"), "Should contain ADMIN role")
    }

    @Test
    fun `expired token is rejected`() {
        // Arrange - Create token with very short expiration
        val shortExpirationUtil = createJwtUtilWithExpiration(-1L) // Already expired
        val userDetails = createUserWithRole("1", "ROLE_USER")
        val expiredToken = shortExpirationUtil.generateToken(userDetails)

        // Act & Assert - Focus on expiration behavior
        assertFalse(jwtTokenUtil.validateToken(expiredToken), "Expired token should be invalid")
    }

    @Test
    fun `malformed token is rejected`() {
        // Arrange
        val malformedToken = "not.a.valid.jwt.token"

        // Act & Assert - Focus on malformed token behavior
        assertFalse(jwtTokenUtil.validateToken(malformedToken), "Malformed token should be invalid")
    }

    @Test
    fun `token works immediately after generation`() {
        // Arrange
        val userDetails = createUserWithRole("1", "ROLE_USER")

        // Act
        val token = jwtTokenUtil.generateToken(userDetails)

        // Assert - Focus on immediate usability
        assertTrue(jwtTokenUtil.validateToken(token), "Token should be valid immediately")
        assertEquals("1", jwtTokenUtil.getUsernameFromToken(token), "Username should be extractable immediately")
        assertTrue(jwtTokenUtil.getExpirationDateFromToken(token).after(Date()), "Should not be expired")
    }

    @Test
    fun `empty or null roles are handled gracefully`() {
        // Arrange
        val userDetailsWithoutRoles = User.builder()
            .username("4")
            .password("password")
            .authorities(emptyList())
            .build()

        // Act
        val token = jwtTokenUtil.generateToken(userDetailsWithoutRoles)
        val roles = jwtTokenUtil.getRolesFromToken(token)

        // Assert - Focus on edge case behavior
        assertTrue(roles.isEmpty(), "Should handle empty roles gracefully")
        assertEquals("4", jwtTokenUtil.getUsernameFromToken(token), "Username should still be extractable")
    }

    // Helper methods to create test data and reduce duplication
    private fun createUserWithRole(username: String, role: String): UserDetails {
        return User.builder()
            .username(username)
            .password("encodedPassword")
            .authorities(listOf(SimpleGrantedAuthority(role)))
            .build()
    }

    private fun createUserWithMultipleRoles(username: String, roles: List<String>): UserDetails {
        return User.builder()
            .username(username)
            .password("password")
            .authorities(roles.map { SimpleGrantedAuthority(it) })
            .build()
    }

    private fun createJwtUtilWithExpiration(expirationSeconds: Long): JwtTokenUtil {
        val util = JwtTokenUtil()
        ReflectionTestUtils.setField(util, "secret", "mySecretKeyForJWTTokenGenerationAndValidation2024")
        ReflectionTestUtils.setField(util, "jwtExpiration", expirationSeconds)
        return util
    }
}
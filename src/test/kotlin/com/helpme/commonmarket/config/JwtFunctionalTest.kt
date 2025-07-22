package com.helpme.commonmarket.config

import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtFunctionalTest {

    @Test
    fun `JWT implementation should work end-to-end`() {
        // Setup JWT utility
        val jwtTokenUtil = JwtTokenUtil()
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation2024")
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpiration", 86400L)

        // Create user details
        val userDetails = User.builder()
            .username("1")
            .password("encodedPassword")
            .authorities(listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
            .build()

        // Test token generation
        val token = jwtTokenUtil.generateToken(userDetails)
        assertTrue(token.isNotEmpty(), "Token should be generated")

        // Test token validation
        assertTrue(jwtTokenUtil.validateToken(token), "Token should be valid")

        // Test claims extraction
        assertEquals("1", jwtTokenUtil.getUsernameFromToken(token), "Username should match")
        val roles = jwtTokenUtil.getRolesFromToken(token)
        assertEquals(1, roles.size, "Should have one role")
        assertEquals("ROLE_ADMIN", roles[0], "Role should match")

        // Test token validation with user
        assertTrue(jwtTokenUtil.validateToken(token, userDetails), "Token should validate against user")

        println("✅ JWT End-to-End Test Passed")
        println("   Token: ${token.take(50)}...")
        println("   Username: ${jwtTokenUtil.getUsernameFromToken(token)}")
        println("   Roles: ${jwtTokenUtil.getRolesFromToken(token)}")
    }
}
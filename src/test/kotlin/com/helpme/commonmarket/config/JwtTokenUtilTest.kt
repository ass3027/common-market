package com.helpme.commonmarket.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.util.ReflectionTestUtils
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtTokenUtilTest {

    private lateinit var jwtTokenUtil: JwtTokenUtil
    private lateinit var userDetails: UserDetails

    @BeforeEach
    fun setUp() {
        jwtTokenUtil = JwtTokenUtil()
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "mySecretKeyForJWTTokenGenerationAndValidation2024")
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpiration", 86400L) // 24 hours

        userDetails = User.builder()
            .username("1")
            .password("encodedPassword")
            .authorities(listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
            .build()
    }

    @Test
    fun `generateToken should create valid JWT token`() {
        val token = jwtTokenUtil.generateToken(userDetails)

        assertTrue(token.isNotEmpty())
        assertTrue(token.contains(".")) // JWT has dots separating header.payload.signature
        assertEquals(3, token.split(".").size) // JWT should have 3 parts
    }

    @Test
    fun `getUsernameFromToken should extract correct username`() {
        val token = jwtTokenUtil.generateToken(userDetails)

        val extractedUsername = jwtTokenUtil.getUsernameFromToken(token)

        assertEquals("1", extractedUsername)
    }

    @Test
    fun `getRolesFromToken should extract correct roles`() {
        val token = jwtTokenUtil.generateToken(userDetails)

        val extractedRoles = jwtTokenUtil.getRolesFromToken(token)

        assertEquals(1, extractedRoles.size)
        assertEquals("ROLE_ADMIN", extractedRoles[0])
    }

    @Test
    fun `getExpirationDateFromToken should return future date`() {
        val token = jwtTokenUtil.generateToken(userDetails)
        val now = Date()

        val expiration = jwtTokenUtil.getExpirationDateFromToken(token)

        assertTrue(expiration.after(now))
    }

    @Test
    fun `validateToken should return true for valid token and matching user`() {
        val token = jwtTokenUtil.generateToken(userDetails)

        val isValid = jwtTokenUtil.validateToken(token, userDetails)

        assertTrue(isValid)
    }

    @Test
    fun `validateToken should return false for token with wrong username`() {
        val token = jwtTokenUtil.generateToken(userDetails)
        val differentUser = User.builder()
            .username("2")
            .password("password")
            .authorities(emptyList())
            .build()

        val isValid = jwtTokenUtil.validateToken(token, differentUser)

        assertFalse(isValid)
    }

    @Test
    fun `validateToken should return true for valid token without user comparison`() {
        val token = jwtTokenUtil.generateToken(userDetails)

        val isValid = jwtTokenUtil.validateToken(token)

        assertTrue(isValid)
    }

    @Test
    fun `validateToken should return false for invalid token format`() {
        val invalidToken = "invalid.token.format"

        val isValid = jwtTokenUtil.validateToken(invalidToken)

        assertFalse(isValid)
    }

    @Test
    fun `validateToken should return false for expired token`() {
        // Set a very short expiration time
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpiration", -1L) // Already expired
        val expiredToken = jwtTokenUtil.generateToken(userDetails)

        // Reset to normal expiration for validation
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpiration", 86400L)

        val isValid = jwtTokenUtil.validateToken(expiredToken)

        assertFalse(isValid)
    }

    @Test
    fun `generateToken should include multiple roles`() {
        val multiRoleUser = User.builder()
            .username("3")
            .password("password")
            .authorities(listOf(
                SimpleGrantedAuthority("ROLE_USER"),
                SimpleGrantedAuthority("ROLE_ADMIN")
            ))
            .build()

        val token = jwtTokenUtil.generateToken(multiRoleUser)
        val roles = jwtTokenUtil.getRolesFromToken(token)

        assertEquals(2, roles.size)
        assertTrue(roles.contains("ROLE_USER"))
        assertTrue(roles.contains("ROLE_ADMIN"))
    }

    @Test
    fun `getUsernameFromToken should throw exception for malformed token`() {
        val malformedToken = "not.a.valid.jwt.token"

        assertThrows<Exception> {
            jwtTokenUtil.getUsernameFromToken(malformedToken)
        }
    }

    @Test
    fun `token should be valid immediately after generation`() {
        val token = jwtTokenUtil.generateToken(userDetails)
        val now = Date()
        val expiration = jwtTokenUtil.getExpirationDateFromToken(token)

        assertTrue(expiration.after(now))
        assertTrue(jwtTokenUtil.validateToken(token))
        assertEquals("1", jwtTokenUtil.getUsernameFromToken(token))
    }
}
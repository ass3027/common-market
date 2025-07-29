package com.helpme.commonmarket.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.helpme.commonmarket.auth.dto.LoginRequest
import com.helpme.commonmarket.product.dto.ProductDTO
import com.helpme.commonmarket.user.entity.User
import com.helpme.commonmarket.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class JwtSecurityBehaviorTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenUtil: JwtTokenUtil

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        // Create test users for authentication scenarios
        createTestUsers()
    }

    @Test
    fun `complete authentication flow works end to end`() {
        // Act - Login to get JWT token
        val loginResponse = performLogin("1", "admin123")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.userId").value("1"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
            .andReturn()

        // Extract token and verify it's functional
        val token = extractTokenFromResponse(loginResponse.response.contentAsString)
        assertTrue(jwtTokenUtil.validateToken(token), "Generated token should be valid")
        assertEquals("1", jwtTokenUtil.getUsernameFromToken(token), "Token should contain correct user ID")
    }

    @Test
    fun `public endpoints are accessible without authentication`() {
        // Act & Assert - Public endpoints should work without JWT
        mockMvc.perform(get("/api/v1/product"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `protected endpoints require valid JWT token`() {
        // Arrange
        val productData = createProductData()

        // Act & Assert - Protected endpoint should require authentication
        mockMvc.perform(
            post("/api/v1/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Unauthorized"))
    }

    @Test
    fun `valid JWT token grants access to protected endpoints`() {
        // Arrange - Get valid JWT token
        val token = loginAndGetToken("1", "admin123")
        val productData = createProductData()

        // Act & Assert - Should access protected endpoint with valid token
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `role-based access control works correctly`() {
        // Arrange - Get tokens for different user roles
        val adminToken = loginAndGetToken("1", "admin123")
        val userToken = loginAndGetToken("2", "user123")
        val productData = createProductData()

        // Act & Assert - Admin should have access
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isCreated)

        // Act & Assert - Regular user should be denied
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `invalid credentials are properly rejected`() {
        // Act & Assert - Invalid credentials should be rejected
        performLogin("1", "wrongpassword")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid username or password"))
    }

    @Test
    fun `malformed JWT tokens are rejected`() {
        // Arrange
        val invalidToken = "invalid.jwt.token"
        val productData = createProductData()

        // Act & Assert - Malformed token should be rejected
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `expired JWT tokens are rejected`() {
        // Arrange - Create token with very short expiration
        val expiredToken = createExpiredToken()
        val productData = createProductData()

        // Act & Assert - Expired token should be rejected
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $expiredToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `malformed authorization header is handled gracefully`() {
        // Arrange
        val productData = createProductData()

        // Act & Assert - Malformed header should be handled gracefully
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "InvalidFormat token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData))
        )
            .andExpect(status().isUnauthorized)
    }

    // Helper methods to reduce test complexity and focus on behavior
    private fun createTestUsers() {
        val adminUser = User(
            id = "test1",
            name = "Admin User",
            email = "admin@test.com",
            password = passwordEncoder.encode("admin123"),
            role = "ADMIN"
        )
        
        val regularUser = User(
            id = "test2",
            name = "Regular User",
            email = "user@test.com",
            password = passwordEncoder.encode("user123"),
            role = "USER"
        )
        
        userRepository.saveAll(listOf(adminUser, regularUser))
    }

    private fun performLogin(username: String, password: String) = mockMvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(LoginRequest(username, password)))
    )

    private fun loginAndGetToken(username: String, password: String): String {
        val result = performLogin(username, password).andReturn()
        return extractTokenFromResponse(result.response.contentAsString)
    }

    private fun extractTokenFromResponse(responseContent: String): String {
        val response = objectMapper.readTree(responseContent)
        return response.get("token").asText()
    }

    private fun createProductData() = ProductDTO.Req(
        name = "Test Product",
        price = 100L,
        sellerId = 1L,
        imageUrl = "http://test.com/image.jpg",
        content = "Test description"
    )

    private fun createExpiredToken(): String {
        val shortLivedUtil = JwtTokenUtil()
        org.springframework.test.util.ReflectionTestUtils.setField(
            shortLivedUtil, 
            "secret", 
            "mySecretKeyForJWTTokenGenerationAndValidation2024"
        )
        org.springframework.test.util.ReflectionTestUtils.setField(shortLivedUtil, "jwtExpiration", -1L)

        val userDetails = org.springframework.security.core.userdetails.User.builder()
            .username("1")
            .password("password")
            .authorities(listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
            .build()

        return shortLivedUtil.generateToken(userDetails)
    }

    private fun assertTrue(condition: Boolean, message: String) {
        kotlin.test.assertTrue(condition, message)
    }

    private fun assertEquals(expected: Any?, actual: Any?, message: String) {
        kotlin.test.assertEquals(expected, actual, message)
    }
}
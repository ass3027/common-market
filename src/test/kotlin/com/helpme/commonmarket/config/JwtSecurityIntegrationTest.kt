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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class JwtSecurityIntegrationTest {

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

        // Create test users
        val adminUser = User(
            id = 0,
            name = "Admin User",
            email = "admin@test.com",
            password = passwordEncoder.encode("admin123"),
            role = "ADMIN"
        )
        
        val regularUser = User(
            id = 0,
            name = "Regular User", 
            email = "user@test.com",
            password = passwordEncoder.encode("user123"),
            role = "USER"
        )
        
        userRepository.saveAll(listOf(adminUser, regularUser))
    }

    @Test
    fun `should successfully login and return JWT token`() {
        val loginRequest = LoginRequest("1", "admin123")

        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.userId").value("1"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
            .andReturn()

        val responseContent = result.response.contentAsString
        val response = objectMapper.readTree(responseContent)
        val token = response.get("token").asText()

        // Verify token is valid
        assert(jwtTokenUtil.validateToken(token))
        assert(jwtTokenUtil.getUsernameFromToken(token) == "1")
        assert(jwtTokenUtil.getRolesFromToken(token).contains("ROLE_ADMIN"))
    }

    @Test
    fun `should access public endpoints without authentication`() {
        mockMvc.perform(get("/api/v1/product"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)

        mockMvc.perform(get("/actuator/health"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 401 for protected endpoints without JWT token`() {
        val productReq = ProductDTO.Req(
            name = "Test Product",
            price = 100L,
            sellerId = 1L,
            imageUrl = "http://test.com/image.jpg",
            content = "Test description"
        )

        mockMvc.perform(
            post("/api/v1/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value("Authentication required to access this resource"))
    }

    @Test
    fun `should access protected endpoints with valid JWT token`() {
        // First login to get token
        val loginRequest = LoginRequest("1", "admin123")
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        val loginResponse = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginResponse.get("token").asText()

        // Use token to access protected endpoint
        val productReq = ProductDTO.Req(
            name = "Test Product",
            price = 100L,
            sellerId = 1L,
            imageUrl = "http://test.com/image.jpg",
            content = "Test description"
        )

        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isCreated)
    }

    @Test
    fun `should deny access for USER role to admin endpoints`() {
        // Login as regular user
        val loginRequest = LoginRequest("2", "user123")
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        val loginResponse = objectMapper.readTree(loginResult.response.contentAsString)
        val token = loginResponse.get("token").asText()

        // Try to access admin endpoint
        val productReq = ProductDTO.Req(
            name = "Test Product",
            price = 100L,
            sellerId = 1L,
            imageUrl = "http://test.com/image.jpg",
            content = "Test description"
        )

        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 for invalid JWT token`() {
        val invalidToken = "invalid.jwt.token"
        val productReq = ProductDTO.Req(
            name = "Test Product",
            price = 100L,
            sellerId = 1L,
            imageUrl = "http://test.com/image.jpg",
            content = "Test description"
        )

        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 401 for expired JWT token`() {
        // Create a token with very short expiration
        val shortLivedUtil = JwtTokenUtil()
        val testSecret = "mySecretKeyForJWTTokenGenerationAndValidation2024"
        org.springframework.test.util.ReflectionTestUtils.setField(shortLivedUtil, "secret", testSecret)
        org.springframework.test.util.ReflectionTestUtils.setField(shortLivedUtil, "jwtExpiration", -1L)

        val userDetails = org.springframework.security.core.userdetails.User.builder()
            .username("1")
            .password("password")
            .authorities(listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
            .build()

        val expiredToken = shortLivedUtil.generateToken(userDetails)

        val productReq = ProductDTO.Req(
            name = "Test Product",
            price = 100L,
            sellerId = 1L,
            imageUrl = "http://test.com/image.jpg",
            content = "Test description"
        )

        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "Bearer $expiredToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should handle malformed Authorization header`() {
        val productReq = ProductDTO.Req(
            name = "Test Product",
            price = 100L,
            sellerId = 1L,
            imageUrl = "http://test.com/image.jpg",
            content = "Test description"
        )

        // Test with malformed header (missing Bearer)
        mockMvc.perform(
            post("/api/v1/product")
                .header("Authorization", "InvalidFormat token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
    }
}
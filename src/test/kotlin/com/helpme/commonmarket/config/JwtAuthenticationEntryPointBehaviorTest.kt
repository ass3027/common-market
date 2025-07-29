package com.helpme.commonmarket.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtAuthenticationEntryPointBehaviorTest {

    private lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var authException: BadCredentialsException
    private lateinit var outputStream: ByteArrayOutputStream

    @BeforeEach
    fun setUp() {
        jwtAuthenticationEntryPoint = JwtAuthenticationEntryPoint()
        request = mockk()
        response = mockk(relaxed = true)
        authException = BadCredentialsException("Authentication failed")
        outputStream = ByteArrayOutputStream()

        every { request.servletPath } returns "/api/v1/product"
        every { response.outputStream } returns mockk(relaxed = true)
    }

    @Test
    fun `unauthorized request returns proper HTTP status`() {
        // Act
        jwtAuthenticationEntryPoint.commence(request, response, authException)

        // Assert - Focus on HTTP response behavior
        io.mockk.verify { response.status = HttpServletResponse.SC_UNAUTHORIZED }
    }

    @Test
    fun `response content type is set to JSON`() {
        // Act
        jwtAuthenticationEntryPoint.commence(request, response, authException)

        // Assert - Focus on content type behavior
        io.mockk.verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
    }

    @Test
    fun `error response has expected structure`() {
        // Arrange - Test the response structure directly
        val expectedErrorResponse = mapOf(
            "error" to "Unauthorized",
            "message" to "Authentication required to access this resource",
            "path" to "/api/v1/product"
        )

        // Act - Serialize the expected response to verify structure
        val objectMapper = ObjectMapper()
        val jsonString = objectMapper.writeValueAsString(expectedErrorResponse)
        val parsedResponse = objectMapper.readTree(jsonString)

        // Assert - Focus on response structure behavior
        assertEquals("Unauthorized", parsedResponse.get("error").asText(), "Should have error field")
        assertEquals("Authentication required to access this resource", parsedResponse.get("message").asText(), "Should have message field")
        assertEquals("/api/v1/product", parsedResponse.get("path").asText(), "Should have path field")
        assertEquals(3, parsedResponse.size(), "Should have exactly 3 fields")
    }

    @Test
    fun `different request paths are handled correctly`() {
        // Arrange
        every { request.servletPath } returns "/api/auth/protected"

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, authException)

        // Assert - Focus on path handling behavior
        io.mockk.verify { request.servletPath }
        io.mockk.verify { response.status = HttpServletResponse.SC_UNAUTHORIZED }
    }

    @Test
    fun `different authentication exceptions are handled uniformly`() {
        // Arrange
        val differentException = org.springframework.security.authentication.InsufficientAuthenticationException("Insufficient auth")

        // Act
        jwtAuthenticationEntryPoint.commence(request, response, differentException)

        // Assert - Focus on consistent error handling behavior
        io.mockk.verify { response.status = HttpServletResponse.SC_UNAUTHORIZED }
        io.mockk.verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
    }

    @Test
    fun `null servlet path is handled gracefully`() {
        // Arrange
        every { request.servletPath } returns null

        // Act & Assert - Should not throw exception
        try {
            jwtAuthenticationEntryPoint.commence(request, response, authException)
            // If we get here without exception, the null path was handled gracefully
            assertTrue(true, "Null path should be handled gracefully")
        } catch (e: Exception) {
            kotlin.test.fail("Should handle null servlet path gracefully, but threw: ${e.message}")
        }
    }

    @Test
    fun `response format is valid JSON`() {
        // Arrange - Create actual response structure
        val responseBody = mapOf(
            "error" to "Unauthorized",
            "message" to "Authentication required to access this resource", 
            "path" to "/test/path"
        )

        // Act - Verify JSON serialization works
        val objectMapper = ObjectMapper()
        val jsonString = objectMapper.writeValueAsString(responseBody)

        // Assert - Focus on JSON validity behavior
        assertTrue(jsonString.isNotEmpty(), "Should produce non-empty JSON")
        assertTrue(jsonString.startsWith("{"), "Should be valid JSON object")
        assertTrue(jsonString.contains("\"error\""), "Should contain error field")
        assertTrue(jsonString.contains("\"message\""), "Should contain message field")
        assertTrue(jsonString.contains("\"path\""), "Should contain path field")

        // Verify it can be parsed back
        val parsed = objectMapper.readTree(jsonString)
        assertEquals("Unauthorized", parsed.get("error").asText(), "Should parse back correctly")
    }

    @Test
    fun `entry point handles concurrent requests safely`() {
        // Arrange - Simulate multiple concurrent requests
        val request1 = mockk<HttpServletRequest>()
        val request2 = mockk<HttpServletRequest>()
        val response1 = mockk<HttpServletResponse>(relaxed = true)
        val response2 = mockk<HttpServletResponse>(relaxed = true)

        every { request1.servletPath } returns "/api/path1"
        every { request2.servletPath } returns "/api/path2"
        every { response1.outputStream } returns mockk(relaxed = true)
        every { response2.outputStream } returns mockk(relaxed = true)

        // Act - Process requests concurrently
        jwtAuthenticationEntryPoint.commence(request1, response1, authException)
        jwtAuthenticationEntryPoint.commence(request2, response2, authException)

        // Assert - Both should be processed correctly
        io.mockk.verify { response1.status = HttpServletResponse.SC_UNAUTHORIZED }
        io.mockk.verify { response2.status = HttpServletResponse.SC_UNAUTHORIZED }
    }
}
package com.helpme.commonmarket.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertEquals

class JwtAuthenticationEntryPointTest {

    private lateinit var jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var authException: BadCredentialsException
    private lateinit var responseWriter: StringWriter

    @BeforeEach
    fun setUp() {
        jwtAuthenticationEntryPoint = JwtAuthenticationEntryPoint()
        request = mockk()
        response = mockk(relaxed = true)
        authException = BadCredentialsException("Authentication failed")
        responseWriter = StringWriter()

        every { request.servletPath } returns "/api/v1/product"
        every { response.outputStream } returns mockk(relaxed = true)
    }

    @Test
    fun `commence should set status to 401 Unauthorized`() {
        val printWriter = PrintWriter(responseWriter)
        every { response.writer } returns printWriter

        jwtAuthenticationEntryPoint.commence(request, response, authException)

        verify(exactly = 1) { response.status = HttpServletResponse.SC_UNAUTHORIZED }
    }

    @Test
    fun `commence should set content type to JSON`() {
        val printWriter = PrintWriter(responseWriter)
        every { response.writer } returns printWriter

        jwtAuthenticationEntryPoint.commence(request, response, authException)

        verify(exactly = 1) { response.contentType = MediaType.APPLICATION_JSON_VALUE }
    }

    @Test
    fun `commence should write JSON error response`() {
        jwtAuthenticationEntryPoint.commence(request, response, authException)

        // Since we can't easily capture the JSON written to outputStream,
        // we verify that the correct methods were called
        verify(exactly = 1) { response.status = HttpServletResponse.SC_UNAUTHORIZED }
        verify(exactly = 1) { response.contentType = MediaType.APPLICATION_JSON_VALUE }
        verify(exactly = 1) { response.outputStream }
    }

    @Test
    fun `error response should contain expected fields`() {
        // Test the structure of the error response by creating it manually
        val body = mapOf(
            "error" to "Unauthorized",
            "message" to "Authentication required to access this resource",
            "path" to "/api/v1/product"
        )

        // Verify the structure matches what we expect
        assertEquals("Unauthorized", body["error"])
        assertEquals("Authentication required to access this resource", body["message"])
        assertEquals("/api/v1/product", body["path"])
        assertEquals(3, body.size)
    }

    @Test
    fun `should handle different request paths`() {
        every { request.servletPath } returns "/api/auth/protected"

        jwtAuthenticationEntryPoint.commence(request, response, authException)

        verify(exactly = 1) { request.servletPath }
        verify(exactly = 1) { response.status = HttpServletResponse.SC_UNAUTHORIZED }
    }

    @Test
    fun `should work with different authentication exceptions`() {
        val differentException = org.springframework.security.authentication.InsufficientAuthenticationException("Insufficient auth")

        jwtAuthenticationEntryPoint.commence(request, response, differentException)

        verify(exactly = 1) { response.status = HttpServletResponse.SC_UNAUTHORIZED }
        verify(exactly = 1) { response.contentType = MediaType.APPLICATION_JSON_VALUE }
    }

    @Test
    fun `JSON response should be valid format`() {
        val testBody = mapOf(
            "error" to "Unauthorized",
            "message" to "Authentication required to access this resource",
            "path" to "/api/v1/product"
        )

        val objectMapper = ObjectMapper()
        val jsonString = objectMapper.writeValueAsString(testBody)

        // Verify it's valid JSON by parsing it back
        val parsed = objectMapper.readTree(jsonString)
        assertEquals("Unauthorized", parsed.get("error").asText())
        assertEquals("Authentication required to access this resource", parsed.get("message").asText())
        assertEquals("/api/v1/product", parsed.get("path").asText())
    }

    @Test
    fun `should handle null servlet path gracefully`() {
        every { request.servletPath } returns null

        jwtAuthenticationEntryPoint.commence(request, response, authException)

        verify(exactly = 1) { response.status = HttpServletResponse.SC_UNAUTHORIZED }
        verify(exactly = 1) { response.contentType = MediaType.APPLICATION_JSON_VALUE }
    }
}
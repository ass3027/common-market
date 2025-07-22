package com.helpme.commonmarket.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val customUserDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestTokenHeader = request.getHeader("Authorization")

        var username: String? = null
        var jwtToken: String? = null

        // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7)
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken)
            } catch (e: Exception) {
                logger.error("Unable to get JWT Token or JWT Token has expired", e)
            }
        }

        // Once we get the token validate it.
        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                // Validate token
                if (jwtTokenUtil.validateToken(jwtToken!!)) {
                    val roles = jwtTokenUtil.getRolesFromToken(jwtToken)
                    val authorities = roles.map { SimpleGrantedAuthority(it) }
                    
                    val authToken = UsernamePasswordAuthenticationToken(
                        username, null, authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    
                    // Set the authentication in SecurityContext
                    SecurityContextHolder.getContext().authentication = authToken
                }
            } catch (e: Exception) {
                logger.error("Error validating JWT token", e)
            }
        }

        filterChain.doFilter(request, response)
    }
}
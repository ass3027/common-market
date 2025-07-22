package com.helpme.commonmarket.auth.controller

import com.helpme.commonmarket.auth.dto.LoginRequest
import com.helpme.commonmarket.auth.dto.LoginResponse
import com.helpme.commonmarket.config.CustomUserDetailsService
import com.helpme.commonmarket.config.JwtTokenUtil
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenUtil: JwtTokenUtil,
    private val userDetailsService: CustomUserDetailsService
) {

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<*> {
        return try {
            // Authenticate user
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.username,
                    loginRequest.password
                )
            )

            // Load user details
            val userDetails: UserDetails = userDetailsService.loadUserByUsername(loginRequest.username)
            
            // Generate JWT token
            val token = jwtTokenUtil.generateToken(userDetails)
            
            // Extract roles
            val roles = userDetails.authorities.map { it.authority }
            
            val response = LoginResponse(
                token = token,
                userId = userDetails.username,
                roles = roles
            )
            
            ResponseEntity.ok(response)
        } catch (_: BadCredentialsException) {
            ResponseEntity.badRequest().body(mapOf("error" to "Invalid username or password"))
        } catch (_: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to "Authentication failed"))
        }
    }
}
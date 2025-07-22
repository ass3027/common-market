package com.helpme.commonmarket.auth.dto

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val type: String = "Bearer",
    val userId: String,
    val roles: List<String>
)
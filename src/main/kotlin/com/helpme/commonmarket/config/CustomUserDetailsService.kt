package com.helpme.commonmarket.config

import com.helpme.commonmarket.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val userId = try {
            username.toLong()
        } catch (e: NumberFormatException) {
            throw UsernameNotFoundException("Invalid user ID format: $username")
        }

        val user = userRepository.findById(userId).orElseThrow {
            UsernameNotFoundException("User not found with id: $userId")
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role}"))

        return User.builder()
            .username(user.id.toString())
            .password(user.password)
            .authorities(authorities)
            .build()
    }
}
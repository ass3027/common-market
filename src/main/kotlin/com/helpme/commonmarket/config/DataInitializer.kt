package com.helpme.commonmarket.config

import com.helpme.commonmarket.user.entity.User
import com.helpme.commonmarket.user.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // Create test users only if they don't exist
        if (!userRepository.existsById(1L)) {
            val admin = User(
                id = 0,
                name = "Admin User",
                email = "admin@example.com", 
                password = passwordEncoder.encode("admin123"),
                role = "ADMIN"
            )
            userRepository.save(admin)
            println("Created admin user - ID: 1, Password: admin123")
        }

        if (!userRepository.existsById(2L)) {
            val user = User(
                id = 0,
                name = "Regular User",
                email = "user@example.com",
                password = passwordEncoder.encode("user123"), 
                role = "USER"
            )
            userRepository.save(user)
            println("Created regular user - ID: 2, Password: user123")
        }
    }
}
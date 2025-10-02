package com.helpme.commonmarket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Order(2)
    @Bean
    fun staticResourceFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/static/**", "/assets/**")
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/actuator/health", permitAll)
                authorize(HttpMethod.GET, "/api/v1/product/**", permitAll)
                authorize(HttpMethod.POST, "/api/auth/login", permitAll)

                arrayOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE).forEach {
                    authorize(it, "/api/v1/product/**", hasRole("ADMIN"))
                }
            }
            exceptionHandling { authenticationEntryPoint = jwtAuthenticationEntryPoint }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }

        return http.build()
    }


    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        //TODO 이해하기
//        JwtAuthenticationProvider
        return authConfig.authenticationManager
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
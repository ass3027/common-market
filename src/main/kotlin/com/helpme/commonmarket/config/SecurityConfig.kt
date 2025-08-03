package com.helpme.commonmarket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Order(2)
    @Bean
    open fun staticResourceFilterChain(http: HttpSecurity): SecurityFilterChain {
        val staticResourcePatterns = arrayOf("/static/**", "/assets/**")
        http {
            securityMatcher(*staticResourcePatterns)
            csrf { disable() }
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
        }
        return http.build()
    }

    @Order(1)
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests { authorize(anyRequest, authenticated) }
        }
        return http.build()
    }
//    @Bean
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//        return http
//            .csrf { it.disable() }
//            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
//            .authorizeHttpRequests { auth ->
//                auth
//                    // Public endpoints
//                    .requestMatchers("/actuator/health").permitAll()
//                    .requestMatchers(HttpMethod.GET, "/api/v1/product/**").permitAll()
//                    .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
//
//                    // Admin endpoints
//                    .requestMatchers(HttpMethod.POST, "/api/v1/product/**").hasRole("ADMIN")
//                    .requestMatchers(HttpMethod.PUT, "/api/v1/product/**").hasRole("ADMIN")
//                    .requestMatchers(HttpMethod.DELETE, "/api/v1/product/**").hasRole("ADMIN")
//
//                    // Any other request needs authentication
//                    .anyRequest().authenticated()
//            }
//            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
//            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
//            .build()
//    }


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
package com.helpme.commonmarket.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfig {
    @Bean
    fun objectMapper(): ObjectMapper {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTimeSerializer = LocalDateTimeSerializer(formatter)
        val localDateTimeDeserializer = LocalDateTimeDeserializer(formatter)

        return ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule().apply {
                this.addSerializer(LocalDateTime::class.java, localDateTimeSerializer)
                this.addDeserializer(LocalDateTime::class.java, localDateTimeDeserializer)
            })
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

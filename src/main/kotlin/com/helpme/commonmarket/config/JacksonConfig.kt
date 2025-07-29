package com.helpme.commonmarket.config

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfig {
    
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        return Jackson2ObjectMapperBuilderCustomizer {
            it.apply {
                // Configure LocalDateTime serialization/deserialization
                serializerByType(LocalDateTime::class.java, LocalDateTimeSerializer(dateTimeFormatter))
                deserializerByType(LocalDateTime::class.java, LocalDateTimeDeserializer(dateTimeFormatter))
                
                // Configure LocalDate serialization/deserialization
                serializerByType(LocalDate::class.java, LocalDateSerializer(dateFormatter))
                deserializerByType(LocalDate::class.java, LocalDateDeserializer(dateFormatter))
                
                // Add Kotlin module for better Kotlin support
                modules(KotlinModule.Builder().build())
            }
        }
    }
}

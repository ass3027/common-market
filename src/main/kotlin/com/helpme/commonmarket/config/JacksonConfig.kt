package com.helpme.commonmarket.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
class JacksonConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        val javaTimeModule = JavaTimeModule()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTimeSerializer = LocalDateTimeSerializer(formatter)
        val localDateTimeDeserializer = LocalDateTimeDeserializer(formatter)

        javaTimeModule.addSerializer(LocalDateTime::class.java, localDateTimeSerializer)
        javaTimeModule.addDeserializer(LocalDateTime::class.java, localDateTimeDeserializer)

        objectMapper.registerModule(javaTimeModule)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        objectMapper.registerKotlinModule()
        return objectMapper
    }
}
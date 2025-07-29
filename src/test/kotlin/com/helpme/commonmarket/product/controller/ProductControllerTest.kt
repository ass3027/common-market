package com.helpme.commonmarket.product.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.helpme.commonmarket.config.JacksonConfig
import com.helpme.commonmarket.product.dto.ProductDTO
import com.helpme.commonmarket.product.service.ProductService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@WebMvcTest(controllers = [ProductController::class], excludeAutoConfiguration = [org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class])
class ProductControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val productService: ProductService
) {

    @TestConfiguration
    class ControllerTestConfig {
        @Bean
        fun productService() = mockk<ProductService>(relaxed = true)

        @Bean
        fun objectMapper() = JacksonConfig().jackson2ObjectMapperBuilderCustomizer()
        
    }

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val fixedDateTime = LocalDateTime.of(2023, 1, 15, 10, 30, 0)

    private val dummyProductRes = ProductDTO.Res(
        id = 1L,
        name = "Test Product",
        price = 10000L,
        sellerId = 1L,
        imageUrl = "http://example.com/image.jpg",
        content = "This is a test product description.",
        createDt = fixedDateTime,
        updateDt = fixedDateTime
    )

    @Test
    fun `GET api v1 products는 상품 목록을 반환해야 한다`() {
        val pageable = PageRequest.of(0, 10)
        val productPage = PageImpl(listOf(dummyProductRes), pageable, 1)

        every { productService.getProducts(any(), any()) } returns productPage

        mockMvc.perform(get("/api/v1/product"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(dummyProductRes.id))
            .andExpect(jsonPath("$.content[0].name").value(dummyProductRes.name))
            .andExpect(jsonPath("$.content[0].createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.content[0].updateDt").value(fixedDateTime.format(formatter)))
    }

    @Test
    fun `GET api v1 products productId는 단일 상품을 반환해야 한다`() {
        every { productService.getProduct(1L) } returns dummyProductRes

        mockMvc.perform(get("/api/v1/product/1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(dummyProductRes.id))
            .andExpect(jsonPath("$.name").value(dummyProductRes.name))
            .andExpect(jsonPath("$.createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.updateDt").value(fixedDateTime.format(formatter)))
    }

    @Test
    fun `POST api v1 products는 새로운 상품을 생성해야 한다`() {
        val productReq = ProductDTO.Req(
            name = "New Product",
            price = 20000L,
            sellerId = 2L,
            imageUrl = "http://example.com/new.jpg",
            content = "New product description."
        )

        every { productService.createProduct(any()) } returns dummyProductRes

        mockMvc.perform(post("/api/v1/product")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(dummyProductRes.id))
            .andExpect(jsonPath("$.name").value(dummyProductRes.name))
            .andExpect(jsonPath("$.createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.updateDt").value(fixedDateTime.format(formatter)))
    }

    @Test
    fun `PUT api v1 products productId는 상품을 업데이트해야 한다`() {
        val productUpdateReq = ProductDTO.UpdateReq(
            id = 1L,
            name = "Updated Product",
            price = 15000L,
            sellerId = null,
            imageUrl = null,
            content = "Updated description."
        )

        every { productService.updateProduct(any()) } returns dummyProductRes

        mockMvc.perform(put("/api/v1/product")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productUpdateReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(dummyProductRes.id))
            .andExpect(jsonPath("$.name").value(dummyProductRes.name))
            .andExpect(jsonPath("$.createDt").value(fixedDateTime.format(formatter)))
            .andExpect(jsonPath("$.updateDt").value(fixedDateTime.format(formatter)))
    }

    @Test
    fun `DELETE api v1 products productId는 상품을 삭제해야 한다`() {
        every { productService.deleteProduct(1L) } returns Unit

        mockMvc.perform(delete("/api/v1/product/1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST api v1 products는 인증없이 호출시 401을 반환해야 한다`() {
        val productReq = ProductDTO.Req(
            name = "New Product",
            price = 20000L,
            sellerId = 2L,
            imageUrl = "http://example.com/new.jpg",
            content = "New product description."
        )

        mockMvc.perform(post("/api/v1/product")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST api v1 products는 USER 권한으로 호출시 403을 반환해야 한다`() {
        val productReq = ProductDTO.Req(
            name = "New Product",
            price = 20000L,
            sellerId = 2L,
            imageUrl = "http://example.com/new.jpg",
            content = "New product description."
        )

        mockMvc.perform(post("/api/v1/product")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(productReq))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isForbidden)
    }
}
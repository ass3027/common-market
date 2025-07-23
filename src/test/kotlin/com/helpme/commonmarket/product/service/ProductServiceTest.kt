package com.helpme.commonmarket.product.service

import com.helpme.commonmarket.product.dto.ProductDTO
import com.helpme.commonmarket.product.entity.Product
import com.helpme.commonmarket.product.mapper.toEntity
import com.helpme.commonmarket.product.mapper.toResDTO
import com.helpme.commonmarket.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional


class ProductServiceTest {

    private val productRepository: ProductRepository = mockk()
    private val productService = ProductService(productRepository)

    private val dummyProduct = Product(
        id = 1L,
        name = "Test Product",
        price = 10000L,
        sellerId = 1L,
        imageUrl = "http://example.com/image.jpg",
        content = "This is a test product description.",
        createDt = LocalDateTime.now(),
        updateDt = LocalDateTime.now()
    )

    private val dummyProductRes = dummyProduct.toResDTO()

    @Test
    fun `getProduct은 주어진 ID의 상품을 반환해야 한다`() {
        // Given
        every { productRepository.findById(1L) } returns Optional.of(dummyProduct)

        // When
        val result = productService.getProduct(1L)

        // Then
        assertEquals(dummyProductRes, result)
    }

    @Test
    fun `getProduct은 상품을 찾을 수 없을 때 IllegalArgumentException을 던져야 한다`() {
        // Given
        every { productRepository.findById(any()) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.getProduct(99L)
        }
        assertEquals("Product not found with id: 99", exception.message)
    }

    @Test
    fun `getProducts는 상품 페이지를 반환해야 한다`() {
        // Given
        val pageable = PageRequest.of(0, 10)
        val productList = listOf(dummyProduct)
        val productPage = PageImpl(productList, pageable, 1)

        every { productRepository.findAll(pageable) } returns productPage

        // When
        val result = productService.getProducts(null, pageable)

        // Then
        assertEquals(1, result.totalElements)
        assertEquals(dummyProductRes, result.content[0])
    }

    @Test
    fun `createProduct는 새로운 상품을 생성해야 한다`() {
        // Given
        val productReq = ProductDTO.Req(
            name = "New Product",
            price = 20000L,
            sellerId = 2L,
            imageUrl = "http://example.com/new.jpg",
            content = "New product description."
        )
        val productToSave = productReq.toEntity()
        val savedProduct = productToSave.copy(id = 2L, createDt = LocalDateTime.now(), updateDt = LocalDateTime.now())

        every { productRepository.save(any<Product>()) } returns savedProduct

        // When
        val result = productService.createProduct(productReq)

        // Then
        assertEquals(savedProduct.toResDTO(), result)
    }

    @Test
    fun `updateProduct는 기존 상품을 업데이트해야 한다`() {
        // Given
        val productUpdateReq = ProductDTO.UpdateReq(
            id = 1L,
            name = "Updated Product",
            price = 15000L,
            sellerId = null,
            imageUrl = null,
            content = "Updated description."
        )
        val updatedProductEntity = dummyProduct.copy(
            name = productUpdateReq.name!!,
            price = productUpdateReq.price!!,
            content = productUpdateReq.content!!,
            updateDt = LocalDateTime.now().plusMinutes(1) // Simulate update
        )

        every { productRepository.findById(1L) } returns Optional.of(dummyProduct)
        every { productRepository.save(any<Product>()) } returns updatedProductEntity

        // When
        val result = productService.updateProduct(productUpdateReq)

        // Then
        assertEquals(updatedProductEntity.toResDTO(), result)
    }

    @Test
    fun `updateProduct는 상품을 찾을 수 없을 때 IllegalArgumentException을 던져야 한다`() {
        // Given
        every { productRepository.findById(any()) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.updateProduct(ProductDTO.UpdateReq(id = 99L, name = null, price = null, sellerId = null, imageUrl = null, content = null))
        }
        assertEquals("Product not found with id: 99", exception.message)
    }

    @Test
    fun `deleteProduct는 주어진 ID의 상품을 삭제해야 한다`() {
        // Given
        every { productRepository.existsById(1L) } returns true
        every { productRepository.deleteById(1L) } returns Unit

        // When
        productService.deleteProduct(1L)

        // Then - Successful deletion doesn't throw an exception
        // The fact that no exception was thrown indicates successful deletion
    }

    @Test
    fun `deleteProduct는 상품을 찾을 수 없을 때 IllegalArgumentException을 던져야 한다`() {
        // Given
        every { productRepository.existsById(any()) } returns false

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            productService.deleteProduct(99L)
        }
        assertEquals("Product not found with id: 99", exception.message)
    }
}
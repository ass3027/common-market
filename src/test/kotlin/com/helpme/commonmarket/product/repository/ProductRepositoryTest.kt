package com.helpme.commonmarket.product.repository

import com.helpme.commonmarket.product.entity.Product
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.LocalDateTime

@DataJpaTest
class ProductRepositoryTest @Autowired constructor(
    val productRepository: ProductRepository,
    val entityManager: TestEntityManager
) {

    @Test
    fun `상품을 저장하고 조회할 수 있다`() {
        // Given
        val product = Product(
            name = "Test Product",
            price = 10000L,
            sellerId = 1L,
            imageUrl = "http://example.com/image.jpg",
            content = "This is a test product description."
        )

        // When
        val savedProduct = productRepository.save(product)
        entityManager.flush()
        entityManager.clear()

        // Then
        val foundProduct = productRepository.findById(requireNotNull(savedProduct.id)).orElse(null)
        assertThat(foundProduct).isNotNull()
        assertThat(foundProduct.id).isNotNull()
        assertThat(foundProduct.name).isEqualTo("Test Product")
        assertThat(foundProduct.price).isEqualTo(10000L)
        assertThat(foundProduct.sellerId).isEqualTo(1L)
        assertThat(foundProduct.imageUrl).isEqualTo("http://example.com/image.jpg")
        assertThat(foundProduct.content).isEqualTo("This is a test product description.")
        assertThat(foundProduct.createDt).isNotNull()
        assertThat(foundProduct.updateDt).isNotNull()
        assertThat(foundProduct.createDt).isBeforeOrEqualTo(LocalDateTime.now())
        assertThat(foundProduct.updateDt).isBeforeOrEqualTo(LocalDateTime.now())
    }

    @Test
    fun `상품을 업데이트할 수 있다`() {
        // Given
        val product = Product(
            name = "Original Product",
            price = 5000L,
            sellerId = 2L,
            imageUrl = "http://example.com/original.jpg",
            content = "Original description."
        )
        val savedProduct = productRepository.save(product)
        entityManager.flush()
        entityManager.clear()

        val productBeforeUpdate = productRepository.findById(requireNotNull(savedProduct.id)).orElse(null)!!
        val originalCreateDt = productBeforeUpdate.createDt
        val originalUpdateDt = productBeforeUpdate.updateDt

        // When
        val updatedName = "Updated Product"
        val updatedPrice = 12000L
        val updatedContent = "Updated description."

        val productToUpdate = productBeforeUpdate.copy(
            name = updatedName,
            price = updatedPrice,
            content = updatedContent
        )
        productRepository.save(productToUpdate) // Save the updated product
        entityManager.flush()
        entityManager.clear()

        // Fetch the product again from the database to ensure @PreUpdate was applied
        val fetchedUpdatedProduct = productRepository.findById(requireNotNull(savedProduct.id)).orElse(null)

        // Then
        assertThat(fetchedUpdatedProduct).isNotNull()
        assertThat(fetchedUpdatedProduct.id).isEqualTo(savedProduct.id)
        assertThat(fetchedUpdatedProduct.name).isEqualTo(updatedName)
        assertThat(fetchedUpdatedProduct.price).isEqualTo(updatedPrice)
        assertThat(fetchedUpdatedProduct.content).isEqualTo(updatedContent)
        assertThat(fetchedUpdatedProduct.createDt).isEqualTo(originalCreateDt) // createDt should not change
        assertThat(fetchedUpdatedProduct.updateDt).isAfter(originalUpdateDt) // updateDt should be updated
    }

    @Test
    fun `상품을 ID로 찾을 수 있다`() {
        // Given
        val product = Product(
            name = "Find Me",
            price = 7000L,
            sellerId = 3L,
            imageUrl = "http://example.com/findme.jpg",
            content = "Content to find."
        )
        val savedProduct = productRepository.save(product)
        entityManager.flush()
        entityManager.clear()

        // When
        val foundProduct = productRepository.findById(requireNotNull(savedProduct.id)).orElse(null)

        // Then
        assertThat(foundProduct).isNotNull()
        assertThat(foundProduct.id).isEqualTo(savedProduct.id)
        assertThat(foundProduct.name).isEqualTo("Find Me")
    }

    @Test
    fun `상품을 삭제할 수 있다`() {
        // Given
        val product = Product(
            name = "Delete Me",
            price = 2000L,
            sellerId = 4L,
            imageUrl = "http://example.com/delete.jpg",
            content = "Content to delete."
        )
        val savedProduct = productRepository.save(product)
        entityManager.flush()
        entityManager.clear()

        // When
        productRepository.deleteById(requireNotNull(savedProduct.id))
        entityManager.flush()
        entityManager.clear()

        // Then
        val foundProduct = productRepository.findById(requireNotNull(savedProduct.id)).orElse(null)
        assertThat(foundProduct).isNull()
    }
}
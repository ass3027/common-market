package com.helpme.commonmarket.util

import com.helpme.commonmarket.product.entity.Product
import com.helpme.commonmarket.product.repository.ProductRepository
import net.datafaker.Faker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.junit.jupiter.api.Tag

@SpringBootTest(properties = ["spring.test.database.replace=none"])
@ActiveProfiles("dev")
@Tag("data-generation")
class FakeDataInitializer {

    @Autowired
    private lateinit var productRepository: ProductRepository

    private val faker = Faker()

    @Test
    fun generateDummyProducts() {
        val products = mutableListOf<Product>()
        (1..5_000_000).forEach { i ->
            products.add(createDummyProduct())
            if (products.size == 1000) {
                productRepository.saveAll(products)
                products.clear()
                println("insert product $i")
            }
        }
        if (products.isNotEmpty()) {
            productRepository.saveAll(products)
        }
    }

    private fun createDummyProduct(): Product {
        return Product(
            name = faker.commerce().productName(),
            price = faker.number().numberBetween(1000, 1000000).toLong(),
            sellerId = faker.number().numberBetween(1, 100).toLong(),
            imageUrl = faker.internet().image(),
            content = faker.lorem().sentence()
        )
    }
}

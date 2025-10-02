package com.helpme.commonmarket.product.service

import com.helpme.commonmarket.product.dto.ProductDTO
import com.helpme.commonmarket.product.mapper.toEntity
import com.helpme.commonmarket.product.mapper.toResDTO
import com.helpme.commonmarket.product.repository.ProductRepository
import com.helpme.commonmarket.product.specs.ProductSpecification
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(private val productRepository: ProductRepository) {

    @Cacheable(value = ["products"], key = "#id")
    @Transactional(readOnly = true)
    fun getProduct(id: Long): ProductDTO.Res {
        val product = productRepository.findById(id).orElseThrow {
            IllegalArgumentException("Product not found with id: $id")
        }
        return product.toResDTO()
    }

    @Transactional(readOnly = true)
    fun getProducts(filter: Map<String, String>?, pageable: Pageable): Page<ProductDTO.Res> {
        return if (filter.isNullOrEmpty()) {
            productRepository.findAll(pageable).map { it.toResDTO() }
        } else {
            val spec = ProductSpecification.byFilter(filter)
            productRepository.findAll(spec, pageable).map { it.toResDTO() }
        }
    }

    @CacheEvict(value = ["productsList"], allEntries = true)
    fun createProduct(productReq: ProductDTO.Req): ProductDTO.Res {
        val product = productReq.toEntity()
        val savedProduct = productRepository.save(product)
        return savedProduct.toResDTO()
    }

    @Caching(evict = [
        CacheEvict(value = ["products"], key = "#productUpdateReq.id"),
        CacheEvict(value = ["productsList"], allEntries = true)
    ])
    fun updateProduct(productUpdateReq: ProductDTO.UpdateReq): ProductDTO.Res {
        val product = productRepository.findById(productUpdateReq.id).orElseThrow {
            IllegalArgumentException("Product not found with id: ${productUpdateReq.id}")
        }

        productUpdateReq.name?.let { product.name = it }
        productUpdateReq.price?.let { product.price = it }
        productUpdateReq.sellerId?.let { product.sellerId = it }
        productUpdateReq.imageUrl?.let { product.imageUrl = it }

        val updatedProduct = productRepository.save(product)
        return updatedProduct.toResDTO()
    }

    @Caching(evict = [
        CacheEvict(value = ["products"], key = "#id"),
        CacheEvict(value = ["productsList"], allEntries = true)
    ])
    fun deleteProduct(id: Long) {
        if (!productRepository.existsById(id)) {
            throw IllegalArgumentException("Product not found with id: $id")
        }
        productRepository.deleteById(id)
    }
}
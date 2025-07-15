package com.helpme.commonmarket.product.controller

import com.helpme.commonmarket.product.dto.ProductDTO
import com.helpme.commonmarket.product.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/products")
class ProductController(private val productService: ProductService) {

    @GetMapping
    fun getProducts(@PageableDefault(size = 10, sort = ["id"]) pageable: Pageable): ResponseEntity<Page<ProductDTO.Res>> {
        val products = productService.getProducts(pageable)
        return ResponseEntity.ok(products)
    }

    @GetMapping("/{productId}")
    fun getProduct(@PathVariable productId: Long): ResponseEntity<ProductDTO.Res> {
        val product = productService.getProduct(productId)
        return ResponseEntity.ok(product)
    }

    @PostMapping
    fun createProduct(@RequestBody productReq: ProductDTO.Req): ResponseEntity<ProductDTO.Res> {
        val createdProduct = productService.createProduct(productReq)
        return ResponseEntity(createdProduct, HttpStatus.CREATED)
    }

    @PutMapping("/{productId}")
    fun updateProduct(@PathVariable productId: Long, @RequestBody productUpdateReq: ProductDTO.UpdateReq): ResponseEntity<ProductDTO.Res> {
        val updatedProduct = productService.updateProduct(productId, productUpdateReq)
        return ResponseEntity.ok(updatedProduct)
    }

    @DeleteMapping("/{productId}")
    fun deleteProduct(@PathVariable productId: Long): ResponseEntity<Void> {
        productService.deleteProduct(productId)
        return ResponseEntity.noContent().build()
    }
}
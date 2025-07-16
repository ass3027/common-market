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
@RequestMapping("/api/v1/product")
class ProductController(private val productService: ProductService) {

//    init {
//        val productReq = ProductDTO.Req(
//            name = "Test Product",
//            price = 10000L,
//            sellerId = 99L,
//            imageUrl = "http://example.com/image.jpg",
//            content = "This is a test product description."
//        )
//        productService.createProduct(productReq)
//    }

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

    @PutMapping
    fun updateProduct(@RequestBody productUpdateReq: ProductDTO.UpdateReq): ResponseEntity<ProductDTO.Res> {
        val updatedProduct = productService.updateProduct(productUpdateReq)
        return ResponseEntity.ok(updatedProduct)
    }

    @DeleteMapping("/{productId}")
    fun deleteProduct(@PathVariable productId: Long): ResponseEntity<Void> {
        productService.deleteProduct(productId)
        return ResponseEntity.noContent().build()
    }
}
package com.helpme.commonmarket.product.repository

import com.helpme.commonmarket.product.entity.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>

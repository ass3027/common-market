package com.helpme.commonmarket.product.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var name: String,

    var price: Long,

    var sellerId: Long, // 판매자 ID

    var imageUrl: String? = null
)

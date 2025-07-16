package com.helpme.commonmarket.product.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@Entity
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    var name: String = "",

    var price: Long = 0L,

    var sellerId: Long = 0L, // 판매자 ID

    var imageUrl: String? = null,

    var content: String? = null,

    var createDt: LocalDateTime? = null,

    var updateDt: LocalDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        createDt = LocalDateTime.now()
        updateDt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updateDt = LocalDateTime.now()
    }
}

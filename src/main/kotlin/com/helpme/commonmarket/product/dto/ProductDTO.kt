package com.helpme.commonmarket.product.dto

import java.time.LocalDateTime

class ProductDTO {

    data class Res(
       val id: Long,
       val name: String,
       val price: Long,
       val sellerId: Long,
       val imageUrl: String?,
       val content: String?,
       val createDt: LocalDateTime?,
       val updateDt: LocalDateTime?
    )

    data class Req(
        val name: String,
        val price: Long,
        val sellerId: Long,
        val imageUrl: String?,
        val content: String?
    )

    data class UpdateReq(
        val id: Long,
        val name: String?,
        val price: Long?,
        val sellerId: Long?,
        val imageUrl: String?,
        val content: String?
    )
}
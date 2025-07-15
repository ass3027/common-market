package com.helpme.commonmarket.product.dto

class ProductDTO {

    data class Res(
       val id: Long?,
       val name: String,
       val price: Long,
       val sellerId: Long,
       val imageUrl: String?
    )

    data class Req(
        val name: String,
        val price: Long,
        val sellerId: Long,
        val imageUrl: String?
    )

    data class UpdateReq(
        val name: String?,
        val price: Long?,
        val sellerId: Long?,
        val imageUrl: String?
    )
}
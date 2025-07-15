package com.helpme.commonmarket.product.mapper

import com.helpme.commonmarket.product.dto.ProductDTO
import com.helpme.commonmarket.product.entity.Product

fun Product.toResDTO(): ProductDTO.Res {
    return ProductDTO.Res(
        id = this.id,
        name = this.name,
        price = this.price,
        sellerId = this.sellerId,
        imageUrl = this.imageUrl
    )
}

fun ProductDTO.Req.toEntity(): Product {
    return Product(
        name = this.name,
        price = this.price,
        sellerId = this.sellerId,
        imageUrl = this.imageUrl
    )
}

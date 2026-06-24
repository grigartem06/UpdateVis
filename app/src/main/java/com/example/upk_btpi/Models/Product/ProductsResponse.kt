package com.example.upk_btpi.Models.Product

import ProductDto
import com.google.gson.annotations.SerializedName

data class ProductsResponse(
    @SerializedName("products")
    val products: List<ProductDto> = emptyList()
)

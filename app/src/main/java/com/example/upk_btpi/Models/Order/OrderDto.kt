package com.example.upk_btpi.Models.Order

import com.example.upk_btpi.Models.Product.ProductDto
import com.google.gson.annotations.SerializedName

data class OrderDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("executorId")
    val executorId: String? = null,

    @SerializedName("customerId")
    val customerId: String? = null,

    @SerializedName("date")
    val date: String? = null,

    @SerializedName("statusName")
    val statusName: String?=null,

    @SerializedName("customersComment")
    val customersComment: String? = null,

    @SerializedName("userComment")
    val userComment: String? = null,

    @SerializedName("productDto")
    val  productDto: ProductDto
)
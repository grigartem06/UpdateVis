package com.example.upk_btpi.Models

import com.google.gson.annotations.SerializedName

data class ProductOrderDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("productName")
    val productName: String? = null,

    @SerializedName("productCost")
    val productCost: Double = 0.0
)

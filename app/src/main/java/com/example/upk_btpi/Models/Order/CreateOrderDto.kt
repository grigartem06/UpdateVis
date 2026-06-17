package com.example.upk_btpi.Models.Order

import com.google.gson.annotations.SerializedName

data class CreateOrderDto(
    @SerializedName("productId")
    val productId: String,

    @SerializedName("statusOrderId")
    val statusOrderId: String,

    @SerializedName("customersComment")
    val customersComment: String? = null,

    @SerializedName("userComment")
    val userComment: String?=null
)

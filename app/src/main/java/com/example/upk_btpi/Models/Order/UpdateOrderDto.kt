package com.example.upk_btpi.Models.Order

import com.google.gson.annotations.SerializedName

data class UpdateOrderDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("productId")
    val productId: String,

    @SerializedName("statusOrderId")
    val statusOrderId: String,

    @SerializedName("customersComment")
    val customersComment: String? = null,

    @SerializedName("userComment")
    val userComment: String? = null
)

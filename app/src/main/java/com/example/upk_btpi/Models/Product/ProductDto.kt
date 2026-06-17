package com.example.upk_btpi.Models.Product

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("ypkId")
    val ypkId: String,

    @SerializedName("productName")
    val productName: String,

    @SerializedName("productCost")
    val productCost: Double,

    @SerializedName("productInfo")
    val productInfo: String?=null,

    @SerializedName("isProduct")
    val isProduct: Boolean,

    @SerializedName("photoPath")
    val photoPath: String? =null,

    @SerializedName("photoUrl")
    val photoUrl: String? = null,

    @SerializedName("address")
    val adress: String?= null,

    @SerializedName("statusProductId")
    val statusProductId: String?=null
)

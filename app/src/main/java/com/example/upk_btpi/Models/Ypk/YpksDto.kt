package com.example.upk_btpi.Models.Ypk

import com.google.gson.annotations.SerializedName

data class YpksDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("ypkName")
    val ypkName: String?=null,
    @SerializedName("description")
    val description: String?=null,
    @SerializedName("isActive")
    val isActive: Boolean
)

package com.example.upk_btpi.Models.Ypk

import com.google.gson.annotations.SerializedName

data class UpdateYpkDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("ypkName")
    val ypkName: String? =null,
    @SerializedName("description")
    val description: String?=null
)

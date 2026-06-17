package com.example.upk_btpi.Models.Ypk

import com.google.gson.annotations.SerializedName

data class CreateYpkDto(
    @SerializedName("ypkName")
    val ypkName: String,
    @SerializedName("description")
    val description: String?=null
)

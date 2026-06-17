package com.example.upk_btpi.Models.StatusProduct

import com.google.gson.annotations.SerializedName

data class StatusProductDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("statusName")
    val statusName: String? = null
)



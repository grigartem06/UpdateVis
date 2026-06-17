package com.example.upk_btpi.Models.Ypk

import com.google.gson.annotations.SerializedName

data class YpkResponse(
    @SerializedName("ypks")
    val ypks: List<YpksDto> = emptyList()
)

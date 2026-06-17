package com.example.upk_btpi.Models.StatusProduct

import com.google.gson.annotations.SerializedName

data class StatusProductResponse(
    @SerializedName("statusProducts")
    val statusProducts: List<StatusProductDto>
)

package com.example.upk_btpi.Models.StatusOrder

import com.google.gson.annotations.SerializedName

data class StatusOrderResponse (
    @SerializedName("statusOrders")
    val statusOrders: List<StatusOrderDto> = emptyList()
)

package com.example.upk_btpi.Models.Order

import com.example.upk_btpi.Models.Order.OrderDto
import com.google.gson.annotations.SerializedName

data class OrdersResponse(
    @SerializedName("orders")
    val orders: List<OrderDto> = emptyList()
)

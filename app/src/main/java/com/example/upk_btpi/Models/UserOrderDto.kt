package com.example.upk_btpi.Models

import com.example.upk_btpi.Models.Role.RoleDto
import com.google.gson.annotations.SerializedName

data class UserOrderDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("fullName")
    val fullName: String? = null,

    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,

    @SerializedName("role")
    val role: RoleDto? = null
)

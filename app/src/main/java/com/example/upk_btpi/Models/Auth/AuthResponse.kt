package com.example.upk_btpi.Models.Auth

import com.google.gson.annotations.SerializedName

// Ответ от сервера
data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String? =null,

    @SerializedName("refreshToken")
    val refreshToken: String? = null
)
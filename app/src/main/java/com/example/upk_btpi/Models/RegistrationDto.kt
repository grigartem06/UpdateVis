package com.example.upk_btpi.Models

import com.google.gson.annotations.SerializedName

// Запрос на регистрацию
data class RegistrationDto(
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("password")
    val password: String
)

package com.example.upk_btpi.Models

import com.google.gson.annotations.SerializedName


data class LoginDto(
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("password")
    val password: String
)

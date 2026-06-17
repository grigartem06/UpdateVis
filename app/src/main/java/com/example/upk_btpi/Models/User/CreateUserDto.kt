package com.example.upk_btpi.Models.User

import com.google.gson.annotations.SerializedName

data class CreateUserDto(
    @SerializedName("fullname")
    val fullname: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String,
    @SerializedName("userInfo")
    val userInfo: String? =null,
    @SerializedName("roleId")
    val roleId: String
)
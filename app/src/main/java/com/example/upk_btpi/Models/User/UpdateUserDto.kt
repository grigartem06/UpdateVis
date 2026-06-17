package com.example.upk_btpi.Models.User

import com.google.gson.annotations.SerializedName

data class UpdateUserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("oldPassword")
    val oldPassword: String? = null,
    @SerializedName("newPassword")
    val newPassword: String? = null,
    @SerializedName("fullname")
    val fullname: String? = null,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("userInfo")
    val userInfo: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean,
    @SerializedName("roleId")
    var roleId: String
)
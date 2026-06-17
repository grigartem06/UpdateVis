package com.example.upk_btpi.Models.Role

import com.google.gson.annotations.SerializedName

data class RoleDto (
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val roleName: String
)
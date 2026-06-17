package com.example.upk_btpi.Models.User

import com.example.upk_btpi.Models.Role.RoleDto
import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("users")
    val users: List<UserDto> = emptyList()
)
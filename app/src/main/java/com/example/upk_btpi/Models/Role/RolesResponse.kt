package com.example.upk_btpi.Models.Role

import com.google.gson.annotations.SerializedName

data class RolesResponse(
    @SerializedName("roles")
    val roles: List<RoleDto> = emptyList()
)

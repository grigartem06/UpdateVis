package com.example.upk_btpi.Models.User

import com.example.upk_btpi.Models.Role.RoleDto
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id")
    var id: String,
    @SerializedName("fullname")
    var fullname: String?=null,
    @SerializedName("hashPassword")
    var hashPassword: String?=null,
    @SerializedName("phoneNumber")
    var phoneNumber: String?= null,
    @SerializedName("userInfo")
    var userInfo: String? = null,
    @SerializedName("isActive")
    var isActive: Boolean,
    @SerializedName("avatarPath")
    var avatarPath: String?=null,
    @SerializedName("avatarUrl")
    var avatarUrl: String? = null,
    @SerializedName("role")
    var role: RoleDto,
    @SerializedName("ypk")
    var ypk: YpksDto

)

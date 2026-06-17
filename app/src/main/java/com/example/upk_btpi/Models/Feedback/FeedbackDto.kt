package com.example.upk_btpi.Models.Feedback

import com.example.upk_btpi.Models.User.UserDto
import com.google.gson.annotations.SerializedName

data class FeedbackDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("comment")
    val comment: String? = null,

    @SerializedName("raiting")
    val raiting: Int,

    @SerializedName("user")
    val user: UserDto,

    @SerializedName("imagePath")
    val imagePath: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null

)
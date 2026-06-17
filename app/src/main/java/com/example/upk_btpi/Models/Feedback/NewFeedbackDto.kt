package com.example.upk_btpi.Models.Feedback

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import java.io.File

data class NewFeedbackDto(
    @SerializedName("Comment")
    val Comment: String,

    @SerializedName("Raiting")
    val Raiting: Int,

    @SerializedName("Image")
    val Image: MultipartBody.Part?= null
)

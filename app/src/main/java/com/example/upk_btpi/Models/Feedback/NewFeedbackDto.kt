package com.example.upk_btpi.Models.Feedback

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import java.io.File

data class NewFeedbackDto(
    @SerializedName("comment")
    val comment: String,

    @SerializedName("raiting")
    val raiting: Int


)

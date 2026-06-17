package com.example.upk_btpi.Models.Feedback

import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.google.gson.annotations.SerializedName

data class FeedbackResponse(
    @SerializedName("feedbacks")
    val feedbacks: List<FeedbackDto> = emptyList()
)

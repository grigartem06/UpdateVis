import com.google.gson.annotations.SerializedName

data class UpdateFeedbackDto(
    @SerializedName("id") val id: String,
    @SerializedName("comment") val comment: String,
    @SerializedName("raiting") val raiting: Int
)
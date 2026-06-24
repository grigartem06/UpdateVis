import com.google.gson.annotations.SerializedName

data class SelectedProductRequest(
    @SerializedName("productId")
    val productId: String
)
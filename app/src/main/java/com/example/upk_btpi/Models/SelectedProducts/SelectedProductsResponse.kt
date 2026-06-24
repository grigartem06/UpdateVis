import com.google.gson.annotations.SerializedName

data class SelectedProductsResponse(
    @SerializedName("selectedProducts")
    val selectedProducts: List<SelectedProductDto> = emptyList()
)
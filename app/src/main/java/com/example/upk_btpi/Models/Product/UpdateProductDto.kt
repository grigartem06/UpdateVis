import com.google.gson.annotations.SerializedName

data class UpdateProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("ProductName") val productName: String,
    @SerializedName("YpkId") val ypkId: String,
    @SerializedName("StatusProductId") val statusProductId: String,
    @SerializedName("ProductCost") val productCost: Double,
    @SerializedName("ProductInfo") val productInfo: String,
    @SerializedName("IsProduct") val isProduct: Boolean,
    @SerializedName("Photo") val photo: String?,
    @SerializedName("Address") val address: String
)
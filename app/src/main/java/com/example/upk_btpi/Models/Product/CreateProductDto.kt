import com.google.gson.annotations.SerializedName

data class CreateProductDto(
    @SerializedName("ProductName") val ProductName: String,
    @SerializedName("ProductInfo") val ProductInfo: String,
    @SerializedName("ProductCost") val ProductCost: Double,
    @SerializedName("IsProduct") val IsProduct: Boolean,
    @SerializedName("Address") val Address: String,  // ✅ Было Adress
    @SerializedName("Photo") val Photo: String? = null,
    @SerializedName("YpkId") val YpkId: String,
    @SerializedName("StatusProductId") val StatusProductId: String
)
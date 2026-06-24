import com.example.upk_btpi.Models.Role.RoleDto
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val fullName: String? = null,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("userInfo")
    val userInfo: String? = null,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null
)
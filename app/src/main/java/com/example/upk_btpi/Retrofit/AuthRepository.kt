package com.example.upk_btpi.Retrofit

import OrderDto
import ProductDto
import SelectedProductsResponse
import UserDto
import android.content.Context
import android.net.Uri
import com.example.upk_btpi.Models.Auth.AuthResponse
import com.example.upk_btpi.Models.Auth.RefreshTokenRequest
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Models.Feedback.FeedbackResponse
import com.example.upk_btpi.Models.Feedback.NewFeedbackDto
import com.example.upk_btpi.Models.LoginDto
import com.example.upk_btpi.Models.Order.CreateOrderDto
import com.example.upk_btpi.Models.Order.OrdersResponse
import com.example.upk_btpi.Models.Order.UpdateOrderDto
import com.example.upk_btpi.Models.Product.ProductsResponse
import com.example.upk_btpi.Models.RegistrationDto
import com.example.upk_btpi.Models.Role.RoleDto
import com.example.upk_btpi.Models.Role.RolesResponse
import com.example.upk_btpi.Models.StatusOrder.StatusOrderResponse
import com.example.upk_btpi.Models.StatusProduct.StatusProductResponse
import com.example.upk_btpi.Models.User.CreateUserDto
import com.example.upk_btpi.Models.User.UserResponse
import com.example.upk_btpi.Models.Ypk.CreateYpkDto
import com.example.upk_btpi.Models.Ypk.UpdateYpkDto
import com.example.upk_btpi.Models.Ypk.YpkResponse
import com.example.upk_btpi.Models.Ypk.YpksDto
import com.example.upk_btpi.Utils.ErrorHandler
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

/**
 * Репозиторий для работы с API авторизации и основными сущностями
 */
class AuthRepository {

    // ==================== AUTH ====================

    /**
     * Получение данных текущего пользователя
     */
    suspend fun getCurrentUser(): Result<UserDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getCurrentUser()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { error -> println("❌ [getCurrentUser] Ошибка: ${ErrorHandler.handleException(error)}") }
    }

    /**
     * Получение полной информации о текущем пользователе
     */
    suspend fun getFullCurrentUser(): Result<UserDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getFullCurrentUser()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { error ->
            println("❌ [getFullCurrentUser] Ошибка: ${ErrorHandler.handleException(error)}")
        }
    }

    /**
     * Обновление данных текущего пользователя
     */
    suspend fun updateCurrentUser(
        fullname: String,
        phoneNumber: String,
        userInfo: String?,
        avatarUri: Uri?,
        context: android.content.Context
    ): Result<Unit> {
        return runCatching {
            val fullnameBody = fullname.toRequestBody("text/plain".toMediaType())
            val phoneNumberBody = phoneNumber.toRequestBody("text/plain".toMediaType())
            val userInfoBody = userInfo?.toRequestBody("text/plain".toMediaType())

            val avatarPart = avatarUri?.let { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
                    tempFile.outputStream().use { output -> inputStream?.copyTo(output) }
                    val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
                    MultipartBody.Part.createFormData("avatar", tempFile.name, requestBody)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            val response = RetrofitClient.apiService.updateCurrentUser(
                fullname = fullnameBody,
                phoneNumber = phoneNumberBody,
                userInfo = userInfoBody,
                avatar = avatarPart
            )

            require(response.isSuccessful) {
                ErrorHandler.handleApiError(response)
            }
        }.onFailure { error ->
            println("❌ [updateCurrentUser] Ошибка: ${ErrorHandler.handleException(error)}")
        }
    }

    /**
     * Получение всех пользователей (для админа)
     */
    suspend fun getAllUsers(): Result<com.example.upk_btpi.Models.User.UserResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getUserAll()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { error ->
            println("❌ [getAllUsers] Ошибка: ${ErrorHandler.handleException(error)}")
        }
    }

    suspend fun register(fullName: String, phoneNumber: String, password: String): Result<AuthResponse> {
        return runCatching {
            val request = RegistrationDto(fullName, phoneNumber, password)
            val response = RetrofitClient.apiService.register(request)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("register", it) }
    }

    suspend fun login(phoneNumber: String, password: String): Result<AuthResponse> {
        return runCatching {
            val request = LoginDto(phoneNumber, password)
            val response = RetrofitClient.apiService.login(request)
            when {
                response.isSuccessful && response.body() != null -> response.body()!!
                response.code() == 401 -> throw Exception("Неверный номер телефона или пароль")
                response.code() == 403 -> throw Exception("Аккаунт заблокирован или не активен")
                else -> throw Exception(ErrorHandler.handleApiError(response))
            }
        }.onFailure { logError("login", it) }
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<AuthResponse> {
        return runCatching {
            val request = RefreshTokenRequest(refreshToken)
            val response = RetrofitClient.apiService.loginViaToken(request)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("refreshAccessToken", it) }
    }

    suspend fun logout(): Result<Unit> {
        return runCatching {
            // Если нет метода logout в API, просто очищаем токены
            AuthInterceptor.clearTokens()
        }.onFailure { logError("logout", it) }
    }

    // ==================== SELECTED PRODUCTS ====================

    suspend fun getAllSelectedProducts(): Result<SelectedProductsResponse> {
        return runCatching {
            // Заглушка, если метод ещё не добавлен в ApiService
            throw NotImplementedError("Метод getAllSelectedProducts ещё не реализован в API")
        }.onFailure { logError("getAllSelectedProducts", it) }
    }

    suspend fun addToSelectedProducts(productId: String): Result<Unit> {
        return runCatching {
            throw NotImplementedError("Метод addToSelectedProducts ещё не реализован в API")
        }.onFailure { logError("addToSelectedProducts", it) }
    }

    suspend fun deleteSelectedProduct(favouriteId: String): Result<Unit> {
        return runCatching {
            throw NotImplementedError("Метод deleteSelectedProduct ещё не реализован в API")
        }.onFailure { logError("deleteSelectedProduct", it) }
    }

    // ==================== FEEDBACK ====================

    suspend fun getAllFeedback(): Result<FeedbackResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllFeedback()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllFeedback", it) }
    }

    suspend fun getFeedbackById(feedbackId: String): Result<FeedbackDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getFeedbackById(feedbackId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getFeedbackById", it) }
    }

    suspend fun deleteFeedbackById(feedbackId: String): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.deleteFeedbackById(feedbackId)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("deleteFeedbackById", it) }
    }

    suspend fun addNewFeedback(comment: String, raiting: Int, imageUri: Uri? = null, context: Context): Result<Unit> {
        return runCatching {
            // ✅ Создаём JSON запрос вместо multipart
            val request = NewFeedbackDto(
                comment = comment,
                raiting = raiting
            )

            val response = RetrofitClient.apiService.addNewFeedback(request)
            require(response.isSuccessful) {
                ErrorHandler.handleApiError(response)
            }
        }.onFailure { logError("addNewFeedback", it) }
    }

    suspend fun updateFeedback(id: String, comment: String, rating: Int): Result<Unit> {
        return runCatching {
            val idBody = id.toRequestBody("text/plain".toMediaType())
            val commentBody = comment.toRequestBody("text/plain".toMediaType())
            val ratingBody = rating.toString().toRequestBody("text/plain".toMediaType())

            val response = RetrofitClient.apiService.updateFeedback(
                id = idBody,
                comment = commentBody,
                rating = ratingBody
            )
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("updateFeedback", it) }
    }

    // ==================== ORDERS ====================

    suspend fun getOrdersForManager(): Result<OrdersResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getOrdersForManager()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getOrdersForManager", it) }
    }

    suspend fun getAllOrders(): Result<OrdersResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllOrders()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllOrders", it) }
    }

    suspend fun getOrderById(orderId: String): Result<OrderDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getOrderById(orderId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getOrderById", it) }
    }

    suspend fun deleteOrderById(orderId: String): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.deleteOrderById(orderId)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("deleteOrderById", it) }
    }

    suspend fun addNewOrder(createOrderDto: CreateOrderDto): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.addNewOrder(createOrderDto)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("addNewOrder", it) }
    }

    suspend fun updateOrder(updateOrderDto: UpdateOrderDto): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.updateOrder(updateOrderDto)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("updateOrder", it) }
    }

    suspend fun getOrdersForUser(): Result<OrdersResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getOrdersForUser()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getOrdersForUser", it) }
    }

    // ==================== PRODUCTS ====================

    suspend fun getAllProducts(): Result<ProductsResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllProducts()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllProducts", it) }
    }

    suspend fun getAllEditingProducts(): Result<ProductsResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllEdetingProducts()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllEditingProducts", it) }
    }

    suspend fun getProductById(productId: String): Result<ProductDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getProductById(productId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getProductById", it) }
    }

    suspend fun getProductsByYpk(ypkId: String): Result<ProductsResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getProductsByYpk(ypkId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getProductsByYpk", it) }
    }

    suspend fun deleteProductById(productId: String): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.deleteProductById(productId)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("deleteProductById", it) }
    }

    suspend fun createProduct(
        productName: String,
        productInfo: String?,
        productCost: Double,
        isProduct: Boolean,
        address: String?,
        ypkId: String,
        statusProductId: String?,
        photoUri: Uri? = null,
        context: Context
    ): Result<String> {
        return runCatching {
            val productNameBody = productName.toRequestBody("text/plain".toMediaType())
            val productInfoBody = productInfo?.toRequestBody("text/plain".toMediaType())
            val productCostBody = productCost.toString().toRequestBody("text/plain".toMediaType())
            val isProductBody = isProduct.toString().toRequestBody("text/plain".toMediaType())
            val addressBody = address?.toRequestBody("text/plain".toMediaType())
            val ypkIdBody = ypkId.toRequestBody("text/plain".toMediaType())
            val statusProductIdBody = statusProductId?.toRequestBody("text/plain".toMediaType())
            val photoPart = photoUri?.let { createImagePart(it, context) }

            val response = RetrofitClient.apiService.createProduct(
                productName = productNameBody,
                productInfo = productInfoBody,
                productCost = productCostBody,
                isProduct = isProductBody,
                address = addressBody,
                ypkId = ypkIdBody,
                statusProductId = statusProductIdBody,
                photo = photoPart
            )
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("createProduct", it) }
    }

    suspend fun updateProduct(
        id: String,
        productName: String,
        productInfo: String,
        productCost: Double,
        isProduct: Boolean,
        address: String,
        ypkId: String,
        statusProductId: String,
        photoUri: Uri? = null,
        context: Context
    ): Result<Unit> {
        return runCatching {
            val idBody = id.toRequestBody("text/plain".toMediaType())
            val productNameBody = productName.toRequestBody("text/plain".toMediaType())
            val productInfoBody = productInfo.toRequestBody("text/plain".toMediaType())
            val productCostBody = productCost.toString().toRequestBody("text/plain".toMediaType())
            val isProductBody = isProduct.toString().toRequestBody("text/plain".toMediaType())
            val addressBody = address.toRequestBody("text/plain".toMediaType())
            val ypkIdBody = ypkId.toRequestBody("text/plain".toMediaType())
            val statusProductIdBody = statusProductId.toRequestBody("text/plain".toMediaType())
            val photoPart = photoUri?.let { createImagePart(it, context) }

            val response = RetrofitClient.apiService.updateProduct(
                id = idBody,
                productName = productNameBody,
                productInfo = productInfoBody,
                productCost = productCostBody,
                isProduct = isProductBody,
                address = addressBody,
                photo = photoPart,
                ypkId = ypkIdBody,
                statusProductId = statusProductIdBody
            )
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("updateProduct", it) }
    }

    // ==================== ROLES ====================

    suspend fun getAllRoles(): Result<RolesResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllRoles()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllRoles", it) }
    }

    suspend fun getRoleById(roleId: String): Result<RoleDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getRoleById(roleId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getRoleById", it) }
    }

    // ==================== STATUS PRODUCT ====================

    suspend fun getAllStatusProduct(): Result<StatusProductResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllStatusProduct()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllStatusProduct", it) }
    }

    // ==================== STATUS ORDER ====================

    suspend fun getAllStatusOrder(): Result<StatusOrderResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllStatusOrder()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllStatusOrder", it) }
    }

    // ==================== USER ====================

    suspend fun getUserAll(): Result<UserResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getUserAll()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getUserAll", it) }
    }

    suspend fun getUserByID(userId: String): Result<UserDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getUserByID(userId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getUserByID", it) }
    }

    suspend fun deleteUserByID(userId: String): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.deleteUserByID(userId)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("deleteUserByID", it) }
    }

    suspend fun createUser(request: CreateUserDto): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.createUser(request)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("createUser", it) }
    }

    suspend fun updateUser(
        id: String,
        oldPassword: String?,
        newPassword: String?,
        fullname: String,
        phoneNumber: String,
        userInfo: String,
        isActive: Boolean,
        avatarUri: Uri? = null,
        context: Context
    ): Result<Unit> {
        return runCatching {
            val idBody = id.toRequestBody("text/plain".toMediaType())
            val oldPasswordBody = oldPassword?.toRequestBody("text/plain".toMediaType())
            val newPasswordBody = newPassword?.toRequestBody("text/plain".toMediaType())
            val fullnameBody = fullname.toRequestBody("text/plain".toMediaType())
            val phoneNumberBody = phoneNumber.toRequestBody("text/plain".toMediaType())
            val userInfoBody = userInfo.toRequestBody("text/plain".toMediaType())
            val isActiveBody = isActive.toString().toRequestBody("text/plain".toMediaType())
            val avatarPart = avatarUri?.let { createImagePart(it, context) }

            val response = RetrofitClient.apiService.updateUser(
                id = idBody,
                oldPassword = oldPasswordBody,
                newPassword = newPasswordBody,
                fullname = fullnameBody,
                phoneNumber = phoneNumberBody,
                userInfo = userInfoBody,
                avatar = avatarPart
            )
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("updateUser", it) }
    }

    suspend fun updateUserForAdmin(
        id: String,
        fullname: String,
        phoneNumber: String,
        roleId: String,
        userInfo: String,
        ypkId: String,
        avatarUri: Uri? = null,
        context: Context
    ): Result<Unit> {
        return runCatching {
            val idBody = id.toRequestBody("text/plain".toMediaType())
            val fullnameBody = fullname.toRequestBody("text/plain".toMediaType())
            val phoneNumberBody = phoneNumber.toRequestBody("text/plain".toMediaType())
            val roleIdBody = roleId.toRequestBody("text/plain".toMediaType())
            val userInfoBody = userInfo.toRequestBody("text/plain".toMediaType())
            val ypkIdBody = ypkId.toRequestBody("text/plain".toMediaType())
            val avatarPart = avatarUri?.let { createImagePart(it, context) }

            val response = RetrofitClient.apiService.updateUserForAdmin(
                id = idBody,
                fullname = fullnameBody,
                phoneNumber = phoneNumberBody,
                roleId = roleIdBody,
                userInfo = userInfoBody,
                ypkId = ypkIdBody,
                avatar = avatarPart
            )
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("updateUserForAdmin", it) }
    }

    // ==================== YPK ====================

    suspend fun getAllYpk(): Result<YpkResponse> {
        return runCatching {
            val response = RetrofitClient.apiService.getAllYpk()
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getAllYpk", it) }
    }

    suspend fun getYpkById(ypkId: String): Result<YpksDto> {
        return runCatching {
            val response = RetrofitClient.apiService.getYpkById(ypkId)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("getYpkById", it) }
    }

    suspend fun deleteYpkById(ypkId: String): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.deleteYpkById(ypkId)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("deleteYpkById", it) }
    }

    suspend fun addNewYpk(request: CreateYpkDto): Result<String> {
        return runCatching {
            val response = RetrofitClient.apiService.addNewYpk(request)
            require(response.isSuccessful && response.body() != null) {
                ErrorHandler.handleApiError(response)
            }
            response.body()!!
        }.onFailure { logError("addNewYpk", it) }
    }

    suspend fun updateYpk(request: UpdateYpkDto): Result<Unit> {
        return runCatching {
            val response = RetrofitClient.apiService.updateYpk(request)
            require(response.isSuccessful) { ErrorHandler.handleApiError(response) }
        }.onFailure { logError("updateYpk", it) }
    }

    // ==================== PRIVATE HELPERS ====================

    private fun createImagePart(uri: Uri, context: Context): MultipartBody.Part {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Не удалось открыть InputStream для $uri")

        val tempFile = File.createTempFile("upload_image", ".jpg", context.cacheDir).apply {
            outputStream().use { output -> inputStream.copyTo(output) }
        }

        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("avatar", tempFile.name, requestBody)
    }

    private fun logError(methodName: String, error: Throwable) {
        println("❌ [$methodName] Ошибка: ${error.message}")
        error.printStackTrace()
    }
}
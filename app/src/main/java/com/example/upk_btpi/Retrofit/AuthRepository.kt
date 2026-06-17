package com.example.upk_btpi.Retrofit

import android.content.Context
import android.net.Uri
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.findFirstRoot
import com.example.upk_btpi.Adapters.FeedbackAdapter
import com.example.upk_btpi.Models.Auth.AuthResponse
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Models.Feedback.FeedbackResponse
import com.example.upk_btpi.Models.Feedback.NewFeedbackDto
import com.example.upk_btpi.Models.Feedback.NewFeedbackResponse
import com.example.upk_btpi.Models.LoginDto
import com.example.upk_btpi.Models.Order.CreateOrderDto
import com.example.upk_btpi.Models.Order.OrderDto
import com.example.upk_btpi.Models.Order.OrdersResponse
import com.example.upk_btpi.Models.Product.CreateProductDto
import com.example.upk_btpi.Models.Product.ProductDto
import com.example.upk_btpi.Models.Product.ProductsResponse
import com.example.upk_btpi.Models.RegistrationDto
import com.example.upk_btpi.Models.StatusProduct.StatusProductResponse
import com.example.upk_btpi.Models.User.UserDto
import com.example.upk_btpi.Models.User.UserResponse
import com.example.upk_btpi.Models.Ypk.CreateYpkDto
import com.example.upk_btpi.Models.Ypk.YpkResponse
import com.example.upk_btpi.Models.Ypk.YpksDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException

class AuthRepository {
    suspend fun register(fullName: String, phoneNumber: String, password: String): Result<AuthResponse> {
        return try {
            val request = RegistrationDto(fullName, phoneNumber, password)
            val response = RetrofitClient.apiService.register(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) { Result.success(body) }
                else { Result.success(AuthResponse(null, null))
                }
            } else {
                // Сервер вернул ошибку
                val errorBody = try { response.errorBody()?.string() }
                catch (e: Exception) { "Не удалось прочитать ошибку" }
                println("❌ ОШИБКА СЕРВЕРА:")
                println("   Код: ${response.code()}")
                println("   Тело ошибки: $errorBody")

                Result.failure(Exception("Ошибка ${response.code()}: $errorBody"))
            }

        } catch (e: IOException) {
            println("❌ ОШИБКА СЕТИ:")
            println("   ${e.message}")
            e.printStackTrace()
            Result.failure(Exception(e.message))
        } catch (e: Exception) {
            println("❌ НЕИЗВЕСТНАЯ ОШИБКА:")
            println("   ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Ошибка: ${e.message ?: "Неизвестная ошибка"}"))
        }
    }
    suspend fun login(phoneNumber: String, password: String): Result<AuthResponse> {
        return try {
            val request = LoginDto(phoneNumber, password)
            val response = RetrofitClient.apiService.login(request)

            println("📤 ВХОД: phoneNumber=$phoneNumber")
            println("📥 ОТВЕТ: код=${response.code()}, токен=${response.body()?.accessToken?.take(20)}...")

            if (response.isSuccessful && response.body() != null) { Result.success(response.body()!!) }
            else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        } catch (e: IOException) { Result.failure(Exception("Нет соединения с сервером")) }
        catch (e: Exception) { Result.failure(e) }
    }
    private  fun  handleResponse(response: Response<AuthResponse>): Result<AuthResponse> {
        return if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            Result.success(body)
        } else {
            val errorBody = try { response.errorBody()?.string() }
            catch (e: Exception) { "Не удалось прочитать ошибку" }
            Result.failure(Exception("Ошибка ${response.code()}: $errorBody"))
        }
    }

    suspend fun getUserByID(userId: String): Result<UserDto> {
        return try {
            val response = RetrofitClient.apiService.getUserByID(userId)
            if (response.isSuccessful && response.body() != null) { Result.success(response.body()!!) }
            else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getAllProducts(): Result<ProductsResponse>
    {
        return  try {
            val response = RetrofitClient.apiService.getAllProducts()
            if(response.isSuccessful && response.body()!= null) {
                val products = response.body()!!
                Result.success(products)
            }
            else {
                val error = response.errorBody()?.string() ?: "неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getAllEdetingProducts(): Result<ProductsResponse>
    {
        return  try {
            val response = RetrofitClient.apiService.getAllEdetingProducts()
            if(response.isSuccessful && response.body()!= null) {
                val products = response.body()!!
                Result.success(products)
            }
            else {
                val error = response.errorBody()?.string() ?: "неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getProductById(productId: String): Result<ProductDto> {
        return try {
            val response = RetrofitClient.apiService.getProductById(productId)

            if (response.isSuccessful && response.body() != null) {
                val product = response.body()!!
                println("✅ Продукт получен: ${product.productName}")
                Result.success(product)
            } else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                println("❌ ОШИБКА: ${response.code()} - $error")
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        } catch (e: Exception) {
            println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createNewOrder(createOrderDto: CreateOrderDto): Result<Unit> {
        return try {
            val response = RetrofitClient.apiService.addNewOrder(createOrderDto)
            if (response.isSuccessful && response.body()!=null) {
                Result.success(Unit)
            } else {
                val error = response.errorBody()?.toString() ?: "неизвестная ошибка"
                Result.failure(Exception(error))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getAllOrders(): Result<OrdersResponse> {
        return try {
            val response = RetrofitClient.apiService.getAllOrders()
            if(response.isSuccessful && response.body()!= null)  {
                val orders = response.body()!!
                Result.success(orders)
            }
            else{
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getOrdersForRoles(role: String): Result<OrdersResponse> {
        return try {
            val response = when (role) {
                "Admin" -> RetrofitClient.apiService.getAllOrders()
                "Manager" -> RetrofitClient.apiService.getOrdersForManager()
                "DefaultUser" -> RetrofitClient.apiService.getOrdersForUser()
                else -> return Result.failure(Exception("Неизвестная роль: $role"))
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getALLUsers(): Result<UserResponse> {
        return try {
                val response = RetrofitClient.apiService.getUserAll()
            if(response.isSuccessful && response.body()!= null) {
                val users = response.body()!!
                Result.success(users)
            }
            else{
                val error = response.errorBody()?.string() ?: "неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAllUpks(): Result<YpkResponse> {
        return try {
            val response = RetrofitClient.apiService.getAllYpk()
            if(response.isSuccessful && response.body()!=null) {
                val upks = response.body()!!
                Result.success(upks)
            }
            else {
                val error = response.errorBody()?.string() ?: "неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAllFeedbacks() : Result<FeedbackResponse> {
        return try {
            val response = RetrofitClient.apiService.getAllFeedback()
            if(response.isSuccessful && response.body() != null) {
                val feedbacks = response.body()!!
                Result.success(feedbacks)
            }
            else{
                val error = response.errorBody()?.string() ?: "неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }



    suspend fun getAllStatusProducts(): Result<StatusProductResponse> {
        return try {
            println("📤 ЗАПРОС: GET /api/StatusProduct/All")

            val response = RetrofitClient.apiService.getAllStatusProduct()

            if (response.isSuccessful && response.body() != null) {
                val statusProducts = response.body()!!
                println("✅ Получено статусов: ${statusProducts.statusProducts.size}")
                Result.success(statusProducts)
            } else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }


    suspend fun getOrderById(orderId: String): Result<OrderDto> {
        return try {
            val response = RetrofitClient.apiService.getOrderById(orderId)

            if (response.isSuccessful && response.body() != null) {
                val order = response.body()!!
                println("✅ Продукт получен: ${order.id}")
                Result.success(order)
            } else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                println("❌ ОШИБКА: ${response.code()} - $error")
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        } catch (e: Exception) {
            println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getFeedbackById(feedbackId: String): Result<FeedbackDto> {
        return  try {
            val response = RetrofitClient.apiService.getFeedbackById(feedbackId)
            if(response.isSuccessful && response.body() !=null) {
                val feedback = response.body()!!
                Result.success(feedback)
            }
            else{
                val error = response.errorBody()?.string()?:"неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))}
        }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getUpkById(upkId: String): Result<YpksDto> {
        return try {
            val response = RetrofitClient.apiService.getYpkById(upkId)
            if (response.isSuccessful && response.body() != null) {
                val upk = response.body()!!
                Result.success(upk)
            } else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getProductsByYpk(ypkId: String): Result<ProductsResponse> {
        return try {
            val response = RetrofitClient.apiService.getProductsByYpk(ypkId)

            if (response.isSuccessful && response.body() != null) {
                val product = response.body()!!
                Result.success(product)
            } else {
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                println("❌ ОШИБКА: ${response.code()} - $error")
                Result.failure(Exception("Ошибка ${response.code()}: $error"))
            }
        } catch (e: Exception) {
            println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
            Result.failure(e)
        }
    }



        suspend fun addNewFeedback(comment: String, raiting: Int, imageUri: Uri? = null, context: Context): Result<String> {
            return try {
                // Создаем RequestBody для текстовых полей
                val commentBody = comment.toRequestBody("text/plain".toMediaType())
                val raitingBody = raiting.toString().toRequestBody("text/plain".toMediaType())

                // Создаем Part для изображения (если есть)
                val imagePart = imageUri?.let { uri -> createImagePart(uri, context) }

                val response = RetrofitClient.apiService.addNewFeedback(
                    comment = commentBody,
                    raiting = raitingBody,
                    image = imagePart
                )

                if (response.isSuccessful && response.body() != null) {
                    val feedbackId = response.body()!!
                    println("✅ Feedback создан с ID: $feedbackId")
                    Result.success(feedbackId)
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    println("❌ ОШИБКА: ${response.code()} - $error")
                    Result.failure(Exception("Ошибка ${response.code()}: $error"))
                }

            } catch (e: Exception) {
                println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }

        // Вспомогательный метод для создания image part
        private fun createImagePart(uri: Uri, context: Context): MultipartBody.Part {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("feedback_image", ".jpg", context.cacheDir)

            inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }

            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaType())
            return MultipartBody.Part.createFormData("Image", tempFile.name, requestBody)
        }


    suspend fun addNewYpk(newYpk: CreateYpkDto ): Result<String> {
        return try {
            val response = RetrofitClient.apiService.addNewYpk(newYpk)
            if(response.isSuccessful&& response.body()!=null){
                val newYpkId  = response.body()!!
                Result.success(newYpkId) }
            else{
                val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                println("❌ ОШИБКА: ${response.code()} - $error")
                Result.failure(Exception("Ошибка ${response.code()}: $error")) }
        }catch (e: Exception) {
            println("❌ ИСКЛЮЧЕНИЕ: ${e.message}")
            Result.failure(e) }
    }

}
package com.example.upk_btpi.Retrofit

import android.media.Image
import com.example.upk_btpi.Models.Auth.AuthResponse
import com.example.upk_btpi.Models.Auth.RefreshTokenRequest
import com.example.upk_btpi.Models.Feedback.FeedbackDto
import com.example.upk_btpi.Models.Feedback.FeedbackResponse
import com.example.upk_btpi.Models.Feedback.NewFeedbackDto
import com.example.upk_btpi.Models.Feedback.NewFeedbackResponse
import com.example.upk_btpi.Models.LoginDto
import com.example.upk_btpi.Models.Order.CreateOrderDto
import com.example.upk_btpi.Models.Order.OrdersResponse
import com.example.upk_btpi.Models.Order.OrderDto
import com.example.upk_btpi.Models.Order.UpdateOrderDto
import com.example.upk_btpi.Models.Product.CreateProductDto
import com.example.upk_btpi.Models.Product.ProductDto
import com.example.upk_btpi.Models.Product.ProductsResponse
import com.example.upk_btpi.Models.RegistrationDto
import com.example.upk_btpi.Models.Role.RoleDto
import com.example.upk_btpi.Models.Role.RolesResponse
import com.example.upk_btpi.Models.StatusOrder.StatusOrderResponse
import com.example.upk_btpi.Models.StatusProduct.StatusProductResponse
import com.example.upk_btpi.Models.User.UpdateUserDto
import com.example.upk_btpi.Models.User.UpdateUserForAdminDto
import com.example.upk_btpi.Models.User.UserResponse
import com.example.upk_btpi.Models.User.CreateUserDto
import com.example.upk_btpi.Models.User.UserDto
import com.example.upk_btpi.Models.Ypk.CreateYpkDto
import com.example.upk_btpi.Models.Ypk.UpdateYpkDto
import com.example.upk_btpi.Models.Ypk.YpkResponse
import com.example.upk_btpi.Models.Ypk.YpksDto
import kotlinx.serialization.BinaryFormat
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    //Auth
    @POST("api/Auth/register")
    suspend fun register(@Body request: RegistrationDto): Response<AuthResponse>
    @POST("api/Auth/login")
    suspend fun login(@Body request: LoginDto): Response<AuthResponse>
    @POST("api/Auth/refresh")
    suspend fun refreshAccessToken(@Body request: RefreshTokenRequest): Response<AuthResponse>


    //FeedBack
    @GET("api/Feedback/All")
    suspend fun getAllFeedback() : Response<FeedbackResponse>
    @GET("api/Feedback/{id}")
    suspend fun getFeedbackById(@Path("id") feedbackId: String) : Response<FeedbackDto>
    @DELETE("api/Feedback/{id}")
    suspend fun deleteFeedbackById(@Path("id") feedbackId: String) : Response<Unit>
    @Multipart
    @POST("api/Feedback")
    suspend fun addNewFeedback(
        @Part("Comment") comment: RequestBody,
        @Part("Raiting") raiting: RequestBody,
        @Part image: MultipartBody.Part? = null
    ) : Response <String>
    @Multipart
    @PUT("api/Feedback")
    suspend fun updateFeedback(
        @Part("Id")id: RequestBody,
        @Part("Comment") comment: RequestBody,
        @Part("Raiting") rating: RequestBody
    ): Response<Unit>


    //Order
    @GET("api/Order/manager")
    suspend fun getOrdersForManager() : Response<OrdersResponse>
    @GET("api/Order/manager/History")
    suspend fun getOrdersForManagerHistory() : Response<OrdersResponse>
    @GET("api/Order/user")
    suspend fun getOrdersForUser() : Response<OrdersResponse>
    @GET("api/Order/user/History")
    suspend fun getOrdersForUserHistory() : Response<OrdersResponse>
    @GET("api/Order/All")
    suspend fun getAllOrders() : Response<OrdersResponse>
    @GET("api/Order/{id}")
    suspend fun getOrderById(@Path ("id") orderId: String ) : Response<OrderDto>
    @DELETE("api/Order/{id}")
    suspend fun deleteOrderById(@Path("id") orderId: String): Response<Unit>
    @POST("api/Order")
    suspend fun addNewOrder(@Body request: CreateOrderDto) : Response<Unit>
    @PUT("api/Order")
    suspend fun updateOrder(@Body request: UpdateOrderDto) : Response<Unit>


    //Product
    @GET("api/Product/All")
    suspend fun getAllProducts() : Response<ProductsResponse>
    @GET("api/Product/All/created")
    suspend fun getAllEdetingProducts() : Response<ProductsResponse>
    @GET("api/Product/{id}")
    suspend fun getProductById(@Path ("id") productId: String) : Response<ProductDto>
    @GET("api/Product/byYpk/{id}")
    suspend fun getProductsByYpk(@Path("id")ypkId: String) : Response<ProductsResponse>
    @DELETE("api/Product/{id}")
    suspend fun deleteProductById(@Path("id") productId: String): Response<Unit>
    @Multipart
    @POST("api/Product")
    suspend fun createProduct(
        @Part("ProductName") productName: RequestBody,
        @Part("ProductInfo") productInfo: RequestBody?,
        @Part("ProductCost") productCost: RequestBody,
        @Part("IsProduct") isProduct: RequestBody,
        @Part("Address") address: RequestBody?,
        @Part("YpkId") ypkId: RequestBody,
        @Part("StatusProductId") statusProductId: RequestBody?,
        @Part photo: MultipartBody.Part? = null
    ): Response<String>  // Возвращает ID созданного продукта
    @Multipart
    @PUT("api/Product")
    suspend fun updateProduct(
        @Part("Id") id: RequestBody,                    // ✅ RequestBody
        @Part("ProductName") productName: RequestBody,  // ✅ RequestBody
        @Part("ProductInfo") productInfo: RequestBody,  // ✅ RequestBody
        @Part("ProductCost") productCost: RequestBody,  // ✅ RequestBody
        @Part("IsProduct") isProduct: RequestBody,      // ✅ RequestBody
        @Part("Address") adres: RequestBody,            // ✅ RequestBody
        @Part photo: MultipartBody.Part?,               // ✅ MultipartBody.Part
        @Part("YpkId") ypkId: RequestBody,              // ✅ RequestBody
        @Part("StatusProductId") statusProductId: RequestBody  // ✅ RequestBody
    ): Response<Unit>


    //Role
    @GET("api/Role/All")
    suspend fun getAllRoles() : Response<RolesResponse>
    @GET("api/Role/{id}")
    suspend fun getRoleById(@Path("id") roleId: String) : Response<RoleDto>


    // StatusProduct
    @GET("api/StatusProduct/All")
    suspend fun getAllStatusProduct(): Response<StatusProductResponse>


    //StatusOrder
    @GET("api/StatusOrder/All")
    suspend fun getAllStatusOrder(): Response<StatusOrderResponse>


    //User
    @GET("api/User/All")
    suspend fun getUserAll() : Response<UserResponse>
    @GET("api/User/{id}")
    suspend fun  getUserByID(@Path("id") userId: String) : Response<UserDto>
    @DELETE("api/User/{id}")
    suspend fun deleteUserByID(@Path( "id") userId: String): Response<Unit>
    @POST("api/User")
    suspend fun createUser(@Body request: CreateUserDto) : Response<Unit>

    @Multipart
    @PUT("/api/user")
    suspend fun updateUser(
        @Part("Id") id: RequestBody,
        @Part("OldPassword") oldPassword: RequestBody?,
        @Part("NewPassword") newPassword: RequestBody?,
        @Part("Fullname") fullname: RequestBody,
        @Part("PhoneNumber") phoneNumber: RequestBody,
        @Part("UserInfo") userInfo: RequestBody,
        @Part("IsActive") isActive : RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Response<Unit>

    @Multipart
    @PUT("/api/user/admin")
    suspend fun updateUserForAdmin(
        @Part("Id") id: RequestBody,
        @Part("Fullname") fullname: RequestBody,
        @Part("PhoneNumber") phoneNumber: RequestBody,
        @Part("RoleId") roleId: RequestBody,
        @Part("UserInfo") userInfo: RequestBody,
        @Part("YpkId") ypkId: RequestBody,
        @Part avatar: MultipartBody.Part?
    ): Response<Unit>


    //Ypk
    @GET("api/Ypk/All")
    suspend fun getAllYpk() : Response<YpkResponse>
    @GET("api/Ypk/{id}")
    suspend fun getYpkById(@Path ("id") ypkId: String) : Response<YpksDto>
    @DELETE("api/Ypk/{id}")
    suspend fun  deleteYpkById(@Path("id") ypkId: String) : Response<Unit>
    @POST("api/Ypk")
    suspend fun addNewYpk(@Body request: CreateYpkDto) : Response<String>
    @PUT("api/Ypk")
    suspend fun updateYpk(@Body request: UpdateYpkDto) : Response<Unit>
}
package com.example.upk_btpi.Retrofit

import OrderDto
import ProductDto
import UserDto
import android.R
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
import com.example.upk_btpi.Models.User.UserResponse
import com.example.upk_btpi.Models.User.CreateUserDto
import com.example.upk_btpi.Models.Ypk.CreateYpkDto
import com.example.upk_btpi.Models.Ypk.UpdateYpkDto
import com.example.upk_btpi.Models.Ypk.YpkResponse
import com.example.upk_btpi.Models.Ypk.YpksDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    // ==================== AUTH ====================
    @POST("api/auth/register")
    suspend fun register(@Body request: RegistrationDto): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginDto): Response<AuthResponse>

    @POST("/api/auth/loginViaToken")
    suspend fun loginViaToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @GET("api/auth/me/all")
    suspend fun getFullCurrentUser(): Response<UserDto>

    // ==================== FEEDBACK ====================
    @GET("api/feedback/all")
    suspend fun getAllFeedback(): Response<FeedbackResponse>

    @GET("api/feedback/{id}")
    suspend fun getFeedbackById(@Path("id") feedbackId: String): Response<FeedbackDto>

    @DELETE("api/feedback/{id}")
    suspend fun deleteFeedbackById(@Path("id") feedbackId: String): Response<Unit>

    @POST("/api/feedback")
    suspend fun addNewFeedback(@Body request: NewFeedbackDto): Response<Unit>

    @Multipart
    @PUT("api/feedback")
    suspend fun updateFeedback(
        @Part("Id") id: RequestBody,
        @Part("Comment") comment: RequestBody,
        @Part("Raiting") rating: RequestBody
    ): Response<Unit>

    // ==================== ORDER ====================
    @GET("api/order/manager")
    suspend fun getOrdersForManager(): Response<OrdersResponse>

    @GET("api/order/manager/History")
    suspend fun getOrdersForManagerHistory(): Response<OrdersResponse>

    @GET("api/order/user")
    suspend fun getOrdersForUser(): Response<OrdersResponse>

    @GET("api/order/user/History")
    suspend fun getOrdersForUserHistory(): Response<OrdersResponse>

    @GET("api/order/All")
    suspend fun getAllOrders(): Response<OrdersResponse>

    @GET("api/order/{id}")
    suspend fun getOrderById(@Path("id") orderId: String): Response<OrderDto>

    @DELETE("api/order/{id}")
    suspend fun deleteOrderById(@Path("id") orderId: String): Response<Unit>

    @POST("api/order")
    suspend fun addNewOrder(@Body request: CreateOrderDto): Response<Unit>

    @PUT("api/order")
    suspend fun updateOrder(@Body request: UpdateOrderDto): Response<Unit>

    // ==================== PRODUCT ====================
    @GET("api/product/all")
    suspend fun getAllProducts(): Response<ProductsResponse>

    @GET("api/product/all/created")
    suspend fun getAllEdetingProducts(): Response<ProductsResponse>

    @GET("api/product/{id}")
    suspend fun getProductById(@Path("id") productId: String): Response<ProductDto>

    @GET("api/product/byYpk/{id}")
    suspend fun getProductsByYpk(@Path("id") ypkId: String): Response<ProductsResponse>

    @DELETE("api/product/{id}")
    suspend fun deleteProductById(@Path("id") productId: String): Response<Unit>

    @Multipart
    @POST("api/product")
    suspend fun createProduct(
        @Part("ProductName") productName: RequestBody,
        @Part("ProductInfo") productInfo: RequestBody?,
        @Part("ProductCost") productCost: RequestBody,
        @Part("IsProduct") isProduct: RequestBody,
        @Part("Address") address: RequestBody?,
        @Part("YpkId") ypkId: RequestBody,
        @Part("StatusProductId") statusProductId: RequestBody?,
        @Part photo: MultipartBody.Part? = null
    ): Response<String>

    @Multipart
    @PUT("api/Product")
    suspend fun updateProduct(
        @Part("Id") id: RequestBody,
        @Part("ProductName") productName: RequestBody,
        @Part("ProductInfo") productInfo: RequestBody,
        @Part("ProductCost") productCost: RequestBody,
        @Part("IsProduct") isProduct: RequestBody,
        @Part("Address") address: RequestBody,
        @Part photo: MultipartBody.Part?,
        @Part("YpkId") ypkId: RequestBody,
        @Part("StatusProductId") statusProductId: RequestBody
    ): Response<Unit>

    // ==================== ROLE ====================
    @GET("api/role/All")
    suspend fun getAllRoles(): Response<RolesResponse>

    @GET("api/role/{id}")
    suspend fun getRoleById(@Path("id") roleId: String): Response<RoleDto>

    // ==================== STATUS PRODUCT ====================
    @GET("api/StatusProduct/all")
    suspend fun getAllStatusProduct(): Response<StatusProductResponse>

    // ==================== STATUS ORDER ====================
    @GET("api/statusOrder/all")
    suspend fun getAllStatusOrder(): Response<StatusOrderResponse>

    // ==================== USER ====================
    @GET("api/user/all")
    suspend fun getUserAll(): Response<UserResponse>

    @GET("api/user/{id}")
    suspend fun getUserByID(@Path("id") userId: String): Response<UserDto>

    @DELETE("api/User/{id}")
    suspend fun deleteUserByID(@Path("id") userId: String): Response<Unit>

    @POST("api/user")
    suspend fun createUser(@Body request: CreateUserDto): Response<Unit>

    @Multipart
    @PUT("/api/user")
    suspend fun updateUser(
        @Part("Id") id: RequestBody,
        @Part("OldPassword") oldPassword: RequestBody?,
        @Part("NewPassword") newPassword: RequestBody?,
        @Part("Fullname") fullname: RequestBody,
        @Part("PhoneNumber") phoneNumber: RequestBody,
        @Part("UserInfo") userInfo: RequestBody,
        //@Part("IsActive") isActive: RequestBody,
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

    @Multipart
    @PUT("/api/user/current")
    suspend fun updateCurrentUser(
        @Part("Fullname") fullname: RequestBody,
        @Part("PhoneNumber") phoneNumber: RequestBody,
        @Part("UserInfo") userInfo: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Response<Unit>

    // ==================== YPK ====================
    @GET("api/ypk/all")
    suspend fun getAllYpk(): Response<YpkResponse>

    @GET("api/ypk/{id}")
    suspend fun getYpkById(@Path("id") ypkId: String): Response<YpksDto>

    @DELETE("api/ypk/{id}")
    suspend fun deleteYpkById(@Path("id") ypkId: String): Response<Unit>

    @POST("api/ypk")
    suspend fun addNewYpk(@Body request: CreateYpkDto): Response<String>

    @PUT("api/ypk")
    suspend fun updateYpk(@Body request: UpdateYpkDto): Response<Unit>
}
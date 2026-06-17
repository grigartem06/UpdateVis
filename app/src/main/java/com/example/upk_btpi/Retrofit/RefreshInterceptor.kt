package com.example.upk_btpi.Retrofit

import com.example.upk_btpi.Models.Auth.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit

class RefreshInterceptor(
    private val retrofit: Retrofit,
    private val authInterceptor: AuthInterceptor
) : Interceptor {

    private var isRefreshing = false
    private val pendingRequests: MutableList<() -> Unit> = mutableListOf()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)

        println("📡 Response code: ${response.code} for ${request.url.encodedPath}")

        if (response.code == 401) {
            println("⚠️ 401 Unauthorized detected! Trying to refresh token...")
            response.close()

            synchronized(this) {
                if (!isRefreshing) {
                    isRefreshing = true
                    val newToken = runBlocking { refreshAccessToken() }
                    isRefreshing = false

                    if (newToken != null) {
                        pendingRequests.forEach { it() }
                        pendingRequests.clear()

                        val newRequest = request.newBuilder().addHeader("Authorization", "Bearer $newToken").build()
                        return chain.proceed(newRequest)
                    }
                    else { AuthInterceptor.clearTokens() }
                } else {
                    pendingRequests.add {
                        val newRequest = request.newBuilder()
                            .addHeader("Authorization", "Bearer ${AuthInterceptor.getAccessToken()}")
                            .build()
                        chain.proceed(newRequest)
                    }
                }
            }
        }

        return response
    }

    private suspend fun refreshAccessToken(): String? {
        return try {
            // ✅ Используем authInterceptor вместо TokenManager
            val refreshToken = AuthInterceptor.getRefreshToken()
                ?: return null

            val apiService = retrofit.create(ApiService::class.java)
            val response = apiService.refreshAccessToken(
                RefreshTokenRequest(refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val newAuth = response.body()!!
                AuthInterceptor.saveTokens(
                    newAuth.accessToken!!,
                    newAuth.refreshToken ?: refreshToken
                )
                newAuth.accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
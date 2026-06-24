package com.example.upk_btpi.Retrofit

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val PREF_NAME = "auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        // ✅ ЕДИНСТВЕННЫЙ источник prefs
        private var prefs: SharedPreferences? = null

        // ✅ Инициализация (вызвать в Application или RetrofitClient)
        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }

        // ✅ Сохранение токенов
        fun saveTokens(accessToken: String, refreshToken: String) {
            prefs?.edit()?.apply {
                putString(KEY_ACCESS_TOKEN, accessToken)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                apply()
            }
        }

        // ✅ Получение access токена
        fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

        // ✅ Получение refresh токена
        fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)

        // ✅ Очистка токенов (при выходе)
        fun clearTokens() { prefs?.edit()?.clear()?.apply() }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath
        val method = originalRequest.method

        println("🔍 AuthInterceptor: $method $path")

        // ✅ ГЛАВНОЕ ИСПРАВЛЕНИЕ: Добавляем Accept: application/json
        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Accept", "application/json")  // ← ЭТО КРИТИЧЕСКИ ВАЖНО!

        // Исключаем auth-эндпоинты
        if (isAuthEndpoint(path)) {
            println("✅ Auth endpoint excluded: $path")
            return chain.proceed(requestBuilder.build())
        }

        // Добавляем токен
        val token = getAccessToken()
        if (!token.isNullOrEmpty()) {
            println("🔐 Adding Bearer token to: $path")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }

    /**
     * Проверяет, является ли запрос эндпоинтом аутентификации
     */
    private fun isAuthEndpoint(path: String): Boolean {
        return path.contains("/api/auth/login", ignoreCase = true) ||
                path.contains("/api/auth/register", ignoreCase = true) ||
                path.contains("/api/auth/refresh", ignoreCase = true) ||
                path.contains("/api/auth/forgot-password", ignoreCase = true) ||
                path.contains("/api/auth/reset-password", ignoreCase = true) ||
                path.contains("/api/auth/logout", ignoreCase = true)
    }
}
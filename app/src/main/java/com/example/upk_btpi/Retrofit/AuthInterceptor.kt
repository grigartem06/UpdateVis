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

        // ✅ Инициализация (вызвать в Application)
        fun init(context: Context) { prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

        // ✅ ЕДИНСТВЕННЫЙ метод saveTokens - в companion object
        fun saveTokens(accessToken: String, refreshToken: String) {
            prefs?.edit()?.apply {
                putString(KEY_ACCESS_TOKEN, accessToken)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                apply()
            }
        }

        fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)
        fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)
        fun clearTokens() { prefs?.edit()?.clear()?.apply() }
    }


    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Пропускаем эндпоинты аутентификации
        val path = originalRequest.url.encodedPath
        if (path.contains("/api/Auth/login") ||
            path.contains("/api/Auth/register") ||
            path.contains("/api/Auth/refresh")) {
            return chain.proceed(originalRequest)
        }

        // Добавляем access token в заголовок
        val token = getAccessToken()
        val requestWithAuth = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(requestWithAuth)
    }
}
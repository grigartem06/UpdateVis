package com.example.upk_btpi.Retrofit

import com.example.upk_btpi.Models.Auth.RefreshTokenRequest
import com.example.upk_btpi.Utils.ErrorHandler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RefreshInterceptor(
    private val retrofit: Retrofit,
    private val authInterceptor: AuthInterceptor
) : Interceptor {

    // ✅ Используем ReentrantLock для потокобезопасности
    private val lock = ReentrantLock()

    // ✅ Deferred для ожидания обновления токена
    private var refreshDeferred: CompletableDeferred<String?>? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // 🔹 ИСКЛЮЧАЕМ эндпоинты аутентификации
        if (isAuthEndpoint(path)) {
            return chain.proceed(request)
        }

        // 🔹 Пытаемся выполнить запрос
        var response = chain.proceed(request)

        // 🔹 Если получили 401 — пытаемся обновить токен и повторить запрос
        if (response.code == 401) {
            response.close() // ✅ Обязательно закрываем старый ответ

            // ✅ Ждём обновления токена (синхронно, но безопасно)
            val newToken = waitForTokenRefresh()

            if (newToken != null) {
                // ✅ Повторяем запрос с новым токеном
                val newRequest = request.newBuilder()
                    .addHeader("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(newRequest)
            } else {
                // ✅ Не удалось обновить — очищаем токены и возвращаем ошибку
                AuthInterceptor.clearTokens()  // ✅ ИСПРАВЛЕНО: вызов через companion object
                // ✅ Возвращаем оригинальный ответ с 401 для обработки в UI
                return response
            }
        }

        return response
    }

    /**
     * Ждёт обновления токена (если идёт) или запускает обновление
     */
    private fun waitForTokenRefresh(): String? {
        return lock.withLock {
            // Если обновление уже идёт — ждём его завершения
            val deferred = refreshDeferred
            if (deferred != null && !deferred.isCompleted) {
                runBlocking { deferred.await() }
            } else {
                // Запускаем обновление токена
                refreshDeferred = CompletableDeferred()
                runBlocking {
                    val newToken = refreshAccessToken()
                    refreshDeferred?.complete(newToken)
                }
            }
            refreshDeferred?.getCompleted()
        }
    }

    /**
     * Обновляет access token с помощью refresh token
     */
    private suspend fun refreshAccessToken(): String? {
        return try {
            val refreshToken = AuthInterceptor.getRefreshToken()  // ✅ ИСПРАВЛЕНО: вызов через companion object
                ?: return null.also { println("❌ No refresh token found") }

            println("🔄 Refreshing token...")

            val apiService = retrofit.create(ApiService::class.java)
            val response = apiService.loginViaToken(RefreshTokenRequest(refreshToken))

            if (response.isSuccessful && response.body() != null) {
                val newAuth = response.body()!!
                val newAccessToken = newAuth.accessToken ?: return null
                val newRefreshToken = newAuth.refreshToken ?: refreshToken

                // ✅ Сохраняем новые токены
                AuthInterceptor.saveTokens(newAccessToken, newRefreshToken)  // ✅ ИСПРАВЛЕНО: вызов через companion object
                println("✅ Token refreshed successfully")
                newAccessToken
            } else {
                val error = ErrorHandler.handleApiError(response)
                println("❌ Refresh failed: ${response.code()} - $error")
                null
            }
        } catch (e: Exception) {
            println("❌ Exception during refresh: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Проверяет, является ли путь эндпоинтом аутентификации
     */
    private fun isAuthEndpoint(path: String): Boolean {
        return path.contains("/api/auth/login", ignoreCase = true) ||
                path.contains("/api/auth/register", ignoreCase = true) ||
                path.contains("/api/auth/refresh", ignoreCase = true) ||
                path.contains("/api/auth/forgot-password", ignoreCase = true) ||
                path.contains("/api/auth/reset-password", ignoreCase = true) ||
                path.contains("/api/auth/logout", ignoreCase = true) ||
                path.contains("/api/auth/me", ignoreCase = true)
    }
}
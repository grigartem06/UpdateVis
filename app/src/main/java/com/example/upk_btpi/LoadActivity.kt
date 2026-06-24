package com.example.upk_btpi

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Auth.RefreshTokenRequest
import com.example.upk_btpi.Retrofit.AuthInterceptor
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.Utils.ErrorHandler
import com.example.upk_btpi.Utils.JwtDecoder
import kotlinx.coroutines.launch

class LoadActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_load)

        // ✅ Инициализация (обязательно перед любыми API-запросами)
        AuthInterceptor.init(applicationContext)
        RetrofitClient.init(applicationContext)
        prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        // ✅ Проверка интернета
        if (!ErrorHandler.isInternetAvailable(this)) {
            ErrorHandler.showDialog(
                context = this,
                title = "Нет подключения",
                message = "Проверьте Wi-Fi или мобильные данные",
                onPositive = { finish() }
            )
            return
        }

        // ✅ Настройка отступов для системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✅ Запускаем проверку авторизации
        checkAuthAndNavigate()
    }

    /**
     * Основная логика проверки авторизации и навигации
     */
    private fun checkAuthAndNavigate() {
        val rememberMe = prefs.getBoolean("remember_me", false)

        if (!rememberMe) {
            goToEntry()
            return
        }

        val accessToken = AuthInterceptor.getAccessToken()
        val refreshToken = AuthInterceptor.getRefreshToken()

        when {
            // ✅ Есть refresh token — пробуем обновить
            !refreshToken.isNullOrEmpty() -> {
                lifecycleScope.launch { tryRefreshToken(refreshToken) }
            }
            // ✅ Есть access token — проверяем его валидность
            !accessToken.isNullOrEmpty() -> {
                lifecycleScope.launch { checkTokenValidity(accessToken) }
            }
            // ✅ Нет токенов — пробуем войти по сохранённым данным
            else -> {
                tryLoginWithSavedCredentials()
            }
        }
    }

    /**
     * Попытка обновить токен через refresh token
     */
    private suspend fun tryRefreshToken(refreshToken: String) {
        try {
            println("🔄 Обновление токена...")

            // ✅ Используем новый эндпоинт loginViaToken
            val response = RetrofitClient.apiService.loginViaToken(
                RefreshTokenRequest(refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val newAuth = response.body()!!

                // ✅ Сохраняем новые токены
                AuthInterceptor.saveTokens(
                    accessToken = newAuth.accessToken ?: "",
                    refreshToken = newAuth.refreshToken ?: refreshToken
                )

                println("✅ Токен обновлён")
                saveUserData(newAuth.accessToken ?: "")
                goToMainPage()

            } else {
                println("❌ Ошибка обновления: ${response.code()}")
                // Refresh token истёк — пробуем войти по логину/паролю
                tryLoginWithSavedCredentials()
            }
        } catch (e: Exception) {
            println("❌ Исключение: ${e.message}")
            tryLoginWithSavedCredentials()
        }
    }

    /**
     * Проверка валидности access token через декодирование JWT
     */
    private suspend fun checkTokenValidity(accessToken: String) {
        try {
            val claims = JwtDecoder.decode(accessToken)
            val exp = claims["exp"]?.toString()?.toLongOrNull()
            val currentTime = System.currentTimeMillis() / 1000

            if (exp != null && exp > currentTime) {
                println("✅ Токен действителен")
                saveUserData(accessToken)  // ✅ Сохраняем данные пользователя
                goToMainPage()
            } else {
                println("⚠️ Токен истёк")
                val refreshToken = AuthInterceptor.getRefreshToken()
                if (!refreshToken.isNullOrEmpty()) {
                    tryRefreshToken(refreshToken)
                } else {
                    goToEntry()
                }
            }
        } catch (e: Exception) {
            println("❌ Ошибка проверки токена: ${e.message}")
            goToEntry()
        }
    }

    /**
     * Вход по сохранённым логину и паролю
     */
    private fun tryLoginWithSavedCredentials() {
        val phoneNumber = prefs.getString("user_phone", null)
        val password = prefs.getString("user_password", null)

        if (phoneNumber.isNullOrEmpty() || password.isNullOrEmpty()) {
            println("⚠️ Нет сохранённых учётных данных")
            goToEntry()
            return
        }

        println("🔑 Вход по сохранённым данным...")

        lifecycleScope.launch {
            val result = authRepository.login(phoneNumber, password)

            result.onSuccess { response ->
                if (!response.accessToken.isNullOrEmpty() && !response.refreshToken.isNullOrEmpty()) {
                    AuthInterceptor.saveTokens(response.accessToken!!, response.refreshToken!!)
                    saveUserData(response.accessToken!!)
                    println("✅ Успешный вход")
                    goToMainPage()
                } else {
                    println("⚠️ Токены не получены")
                    goToEntry()
                }
            }

            result.onFailure { error ->
                println("❌ Ошибка входа: ${error.message}")
                goToEntry()
            }
        }
    }

    /**
     * Сохранение данных пользователя после успешной авторизации
     */
    private fun saveUserData(accessToken: String) {
        val claims = JwtDecoder.decode(accessToken)

        prefs.edit().apply {
            putString("user_id", claims["nameid"]?.toString() ?: "")
            putString("user_name", claims["unique_name"]?.toString() ?: "Пользователь")
            putString("user_role", claims["role"]?.toString() ?: "DefaultUser")
            putBoolean("remember_me", true)
            apply()
        }
    }

    // ==================== НАВИГАЦИЯ ====================

    private fun goToMainPage() {
        Intent(this, MainPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }

    private fun goToEntry() {
        // ✅ Очищаем токены при переходе на вход
        AuthInterceptor.clearTokens()

        Intent(this, Entry::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }
}
package com.example.upk_btpi

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Auth.AuthResponse
import com.example.upk_btpi.Models.Auth.RefreshTokenRequest
import com.example.upk_btpi.Retrofit.AuthInterceptor
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Retrofit.RetrofitClient
import com.example.upk_btpi.Utils.JwtDecoder
import kotlinx.coroutines.launch

class LoadActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_load)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация
        AuthInterceptor.init(applicationContext)
        RetrofitClient.init(applicationContext)

        // ✅ Проверяем сохранённый вход
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val rememberMe = prefs.getBoolean("remember_me", false)

        if (rememberMe) {
            // ✅ Сначала пробуем обновить токен через refresh token
            val refreshToken = AuthInterceptor.getRefreshToken()
            val accessToken = AuthInterceptor.getAccessToken()

            if (!refreshToken.isNullOrEmpty()) {
                // Есть refresh token — пробуем обновить
                lifecycleScope.launch {
                    tryRefreshToken(refreshToken)
                }
            } else if (!accessToken.isNullOrEmpty()) {
                // Есть только access token — проверяем его валидность
                lifecycleScope.launch {
                    checkTokenValidityAndProceed()
                }
            } else {
                // Нет токенов — пробуем войти по сохранённым учётным данным
                tryLoginWithSavedCredentials(prefs)
            }
        } else {
            // "Запомнить меня" не включено — идём на экран входа
            goToEntry()
        }
    }

    // ✅ Попытка обновить токен через refresh token
    private suspend fun tryRefreshToken(refreshToken: String) {
        try {
            println("🔄 Попытка обновления токена через refresh token...")

            val response = RetrofitClient.apiService.refreshAccessToken(
                RefreshTokenRequest(refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val newAuth = response.body()!!
                // Сохраняем новые токены
                AuthInterceptor.saveTokens(
                    newAuth.accessToken ?: "",
                    newAuth.refreshToken ?: refreshToken
                )
                println("✅ Токен успешно обновлён")
                goToMainPage()
            } else {
                println("❌ Не удалось обновить токен (код: ${response.code()})")
                // Refresh token истёк — пробуем войти по учётным данным
                val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                tryLoginWithSavedCredentials(prefs)
            }
        } catch (e: Exception) {
            println("❌ Ошибка при обновлении токена: ${e.message}")
            // Ошибка сети или другая — пробуем войти по учётным данным
            val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            tryLoginWithSavedCredentials(prefs)
        }
    }

    // ✅ Проверка валидности текущего access token
    // ✅ Проверка валидности текущего access token
    private suspend fun checkTokenValidityAndProceed() {
        try {
            val accessToken = AuthInterceptor.getAccessToken() ?: run {
                goToEntry()
                return
            }

            // Декодируем токен и проверяем, не истёк ли он
            val claims = JwtDecoder.decode(accessToken)

            // ✅ ИСПРАВЛЕНО: правильно получаем exp (может быть Long или String)
            val exp = when (val expValue = claims["exp"]) {
                is Long -> expValue
                is String -> expValue.toLongOrNull()
                is Number -> expValue.toLong()
                else -> null
            }

            val currentTime = System.currentTimeMillis() / 1000

            if (exp != null && exp > currentTime) {
                // Токен действителен
                println("✅ Access token действителен (истекает: ${exp})")
                goToMainPage()
            } else {
                println("⚠️ Access token истёк или некорректен")
                // Токен истёк — пробуем обновить через refresh
                val refreshToken = AuthInterceptor.getRefreshToken()
                if (!refreshToken.isNullOrEmpty()) {
                    tryRefreshToken(refreshToken)
                } else {
                    // Нет refresh token — идём на вход
                    goToEntry()
                }
            }
        } catch (e: Exception) {
            println("❌ Ошибка проверки токена: ${e.message}")
            goToEntry()
        }
    }

    // ✅ Вход по сохранённым учётным данным
    private fun tryLoginWithSavedCredentials(prefs: SharedPreferences) {
        val phoneNumber = prefs.getString("user_phone", null)
        val password = prefs.getString("user_password", null)

        if (!phoneNumber.isNullOrEmpty() && !password.isNullOrEmpty()) {
            println("🔑 Вход по сохранённым учётным данным...")
            lifecycleScope.launch {
                val result = authRepository.login(phoneNumber, password)

                result.onSuccess { response ->
                    val accessToken = response.accessToken
                    val refreshToken = response.refreshToken

                    if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                        // Сохраняем токены
                        AuthInterceptor.saveTokens(accessToken, refreshToken)
                        // Сохраняем данные пользователя
                        saveUserData(accessToken, phoneNumber, response)
                        println("✅ Успешный вход по учётным данным")
                        goToMainPage()
                    } else {
                        println("⚠️ Токены не получены при входе")
                        goToEntry()
                    }
                }

                result.onFailure { error ->
                    println("❌ Ошибка входа: ${error.message}")
                    Toast.makeText(
                        this@LoadActivity,
                        "❌ Ошибка авто-входа: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    goToEntry()
                }
            }
        } else {
            // Нет сохранённых учётных данных
            println("⚠️ Нет сохранённых учётных данных")
            goToEntry()
        }
    }

    private fun saveUserData(token: String, phoneNumber: String, response: AuthResponse) {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val claims = JwtDecoder.decode(token)

        prefs.edit().apply {
            putString("user_id", claims["nameid"]?.toString() ?: "")
            putString("user_name", claims["unique_name"]?.toString() ?: "Не указано")
            putString("user_phone", phoneNumber)
            putString("user_role", claims["role"]?.toString() ?: "Пользователь")
            putBoolean("remember_me", true)
            apply()
        }
    }

    // Переход на главную
    private fun goToMainPage() {
        val intent = Intent(this@LoadActivity, MainPage::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Переход на экран входа
    private fun goToEntry() {
        val intent = Intent(this@LoadActivity, Entry::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
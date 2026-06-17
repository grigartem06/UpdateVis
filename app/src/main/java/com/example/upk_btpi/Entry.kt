package com.example.upk_btpi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Auth.AuthResponse
import com.example.upk_btpi.Retrofit.AuthInterceptor  // ← Импортируем
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.JwtDecoder
import com.example.upk_btpi.databinding.ActivityEntryBinding
import kotlinx.coroutines.launch

class Entry : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonGoToRegistrtion.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)) // ← Исправьте имя активности
            finish()
        }

        binding.buttonInput.setOnClickListener {
            val phoneNumber = binding.editTextText2.text.toString().trim()
            val password = binding.editTextTextPassword3.text.toString()

            if (phoneNumber.isEmpty()) {
                binding.editTextText2.error = "Введите номер телефона"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.editTextTextPassword3.error = "Введите пароль"
                return@setOnClickListener
            }

            LogIn(phoneNumber, password, binding.switch1.isChecked)
        }
    }

    private fun LogIn(phoneNumber: String, password: String, rememberMe: Boolean) {
        binding.buttonInput.isEnabled = false
        binding.buttonInput.text = "Вход..."

        lifecycleScope.launch {
            val result = authRepository.login(phoneNumber, password)

            result.onSuccess { response ->
                val accessToken = response.accessToken
                val refreshToken = response.refreshToken

                if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {

                    // ✅ Проверка: читаем сохранённые токены
                    val savedAccess = AuthInterceptor.getAccessToken()
                    val savedRefresh = AuthInterceptor.getRefreshToken()
                    println("💾 Сохранено: access=${savedAccess != null}, refresh=${savedRefresh != null}")
                    // ✅ Сохраняем токены ТОЛЬКО если "Запомнить меня" включен
                    //if (rememberMe) { AuthInterceptor.saveTokens(accessToken, refreshToken) }

                    AuthInterceptor.saveTokens(accessToken, refreshToken)

                    // Сохраняем данные пользователя
                    saveUserData(response, phoneNumber, password,rememberMe)

                    // Переход на главную
                    val intent = Intent(this@Entry, MainPage::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@Entry, "⚠️ Токены не получены", Toast.LENGTH_SHORT).show()
                    binding.buttonInput.isEnabled = true
                    binding.buttonInput.text = "Войти"
                }
            }

            result.onFailure { error ->
                val errorMessage = error.message ?: "Неизвестная ошибка"
                Toast.makeText(this@Entry, "❌ ошибка входа", Toast.LENGTH_LONG).show()
                binding.buttonInput.isEnabled = true
                binding.buttonInput.text = "Войти"
            }
        }
    }

    private fun saveUserData(response: AuthResponse, phoneNumber: String, password: String, rememberMe: Boolean) {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val claims = JwtDecoder.decode(response.accessToken!!)

        prefs.edit().apply {
            putString("user_id", claims["nameid"]?.toString() ?: "")
            putString("user_name", claims["unique_name"]?.toString() ?: "Пользователь")
            putString("user_phone", phoneNumber)
            putString("user_role", claims["role"]?.toString() ?: "DefaultUser")

            // ✅ СОХРАНЯЕМ ПАРОЛЬ ТОЛЬКО ЕСЛИ "ЗАПОМНИТЬ МЕНЯ" ВКЛЮЧЁН
            if (rememberMe) {
                putBoolean("remember_me", true)
                putString("user_password", password) // ⚠️ Или храните зашифрованный пароль
            } else {
                putBoolean("remember_me", false)
                remove("user_password")
            }
            apply()
        }
    }
}
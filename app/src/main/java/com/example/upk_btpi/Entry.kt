package com.example.upk_btpi

import UserDto
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Models.Auth.AuthResponse
import com.example.upk_btpi.Retrofit.AuthInterceptor
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.Utils.ErrorHandler
import com.example.upk_btpi.databinding.ActivityEntryBinding
import kotlinx.coroutines.launch

class Entry : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Переход на регистрацию
        binding.buttonGoToRegistrtion.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Кнопка входа
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

            performLogin(phoneNumber, password, binding.switch1.isChecked)
        }
    }

    /**
     * Выполняет вход: login → получение данных пользователя → сохранение → переход
     */
    private fun performLogin(phoneNumber: String, password: String, rememberMe: Boolean) {
        binding.buttonInput.isEnabled = false
        binding.buttonInput.text = "Вход..."

        lifecycleScope.launch {
            // 🔹 Шаг 1: Вход (получаем токены)
            val loginResult = authRepository.login(phoneNumber, password)

            loginResult.onSuccess { authResponse ->
                // ✅ Сохраняем токены
                AuthInterceptor.saveTokens(
                    authResponse.accessToken ?: "",
                    authResponse.refreshToken ?: ""
                )

                // 🔹 Шаг 2: Получаем данные пользователя через API
                val userResult = authRepository.getCurrentUser()

                userResult.onSuccess { user ->
                    // ✅ Сохраняем данные пользователя и учётные данные (если нужно)
                    saveUserData(user, phoneNumber, password, rememberMe)

                    // 🔹 Шаг 3: Переход на главный экран
                    val intent = Intent(this@Entry, MainPage::class.java)
                    startActivity(intent)
                    finish()
                }

                userResult.onFailure { error ->
                    println("❌ Ошибка получения данных пользователя: ${error.message}")
                    ErrorHandler.showDialog(
                        context = this@Entry,
                        title = "Ошибка",
                        message = "Не удалось получить данные профиля: ${error.message}"
                    )
                    resetLoginButton()
                }
            }

            loginResult.onFailure { error ->
                ErrorHandler.showDialog(
                    context = this@Entry,
                    title = "Ошибка входа",
                    message = error.message ?: "Не удалось войти в аккаунт"
                )
                resetLoginButton()
            }
        }
    }

    /**
     * Сохранение данных пользователя в SharedPreferences
     */
    private fun saveUserData(user: UserDto, phoneNumber: String, password: String, rememberMe: Boolean) {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)

        prefs.edit().apply {
            // ✅ ОБЯЗАТЕЛЬНО: сохраняем user_id (этот ключ читает ProfileFragment)
            putString("user_id", user.id)

            println("🔍 Сохранение данных:")
            println("   user.id = ${user.id}")
            println("   user.fullName = ${user.fullName}")

            // ✅ Сохраняем остальные данные
            putString("user_name", user.fullName ?: "Пользователь")
            putString("user_phone", user.phoneNumber ?: phoneNumber)
            putString("user_role", user.role ?: "DefaultUser")
            putString("user_info", user.userInfo ?: "")
            putString("user_avatar", user.avatarUrl ?: "")

            // ✅ Функция rememberMe
            if (rememberMe) {
                putBoolean("remember_me", true)
                putString("user_password", password)
            } else {
                putBoolean("remember_me", false)
                remove("user_password")
            }
            apply()  // ✅ ОБЯЗАТЕЛЬНО: сохраняет изменения!
        }

        println("📦 Данные сохранены: user_id=${user.id}, role=${user.role}")
    }

    /**
     * Сброс кнопки входа в исходное состояние
     */
    private fun resetLoginButton() {
        binding.buttonInput.isEnabled = true
        binding.buttonInput.text = "Войти"
    }
}
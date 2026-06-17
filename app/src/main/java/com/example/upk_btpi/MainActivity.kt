package com.example.upk_btpi


import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.example.upk_btpi.Retrofit.AuthRepository
import com.example.upk_btpi.databinding.ActivityMainBinding
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Кнопка "Зарегистрироваться"
        binding.button.setOnClickListener {
            val fio = binding.editTextText.text.toString().trim()
            val password = binding.editTextTextPassword.text.toString()
            val confirmPassword = binding.editTextTextPassword2.text.toString()
            val phone = binding.editTextPhone.text.toString().trim()

            if (validateFields(fio, password, confirmPassword, phone)) {
                performRegister(fio, phone, password)
            }
        }

        // Кнопка "Войти"
        binding.button2.setOnClickListener {
            startActivity(Intent(this, Entry::class.java))
            finish()
        }
    }

    // Валидация полей
    private fun validateFields(
        fio: String,
        password: String,
        confirmPassword: String,
        phone: String
    ): Boolean {
        if (fio.length < 2) {
            binding.editTextText.error = "Введите ФИО"
            binding.editTextText.requestFocus()
            return false
        }
        if (password.length < 6) {
            binding.editTextTextPassword.error = "Минимум 6 символов"
            binding.editTextTextPassword.requestFocus()
            return false
        }
        if (password != confirmPassword) {
            binding.editTextTextPassword2.error = "Пароли не совпадают"
            binding.editTextTextPassword2.requestFocus()
            return false
        }
        if (!phone.matches(Regex("^(\\+?7|8)\\d{10}$"))) {
            binding.editTextPhone.error = "Формат: +79991234567"
            binding.editTextPhone.requestFocus()
            return false
        }
        return true
    }

    // Запрос регистрации через API
    private fun performRegister(fio: String, phone: String, password: String) {
        // Блокируем кнопку, чтобы избежать повторных нажатий
        binding.button.isEnabled = false
        binding.button.text = "Регистрация..."

        lifecycleScope.launch {
            val result = authRepository.register(fio, phone, password)

            result.onSuccess { response ->
                Toast.makeText(
                    this@MainActivity, "Регистрация успешна!",
                    Toast.LENGTH_LONG
                ).show()

                // Переход на экран SMS с данными пользователя
                val intent = Intent(this@MainActivity, Entry::class.java)
//                intent.putExtra("user_phone", phone)
//                intent.putExtra("user_fio", fio)
                // Можно добавить токен, если он есть: intent.putExtra("token", response.token)

                startActivity(intent)
                finish()
            }

            result.onFailure { error ->
                val errorMessage = error.message ?: "Неизвестная ошибка"

                // Показываем ошибку в поле ФИО
//                binding.editTextText.error = errorMessage
//                binding.editTextText.requestFocus()

                // Дополнительно можно показать Toast (по желанию)
                Toast.makeText(
                    this@MainActivity,
                    "❌ $errorMessage",
                    Toast.LENGTH_LONG
                ).show()

                // Разблокируем кнопку при ошибке, чтобы пользователь мог повторить
                binding.button.isEnabled = true
                binding.button.text = "Зарегистрироваться"
            }

        }
    }
}



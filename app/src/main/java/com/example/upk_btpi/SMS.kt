package com.example.upk_btpi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.upk_btpi.databinding.ActivitySmsBinding



class SMS : AppCompatActivity() {
    private  lateinit var  binding: ActivitySmsBinding

    private var userPhone: String? = null
    private var userFio: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ ПОЛУЧАЕМ ДАННЫЕ ИЗ INTENT
        userPhone = intent.getStringExtra("user_phone")
        userFio = intent.getStringExtra("user_fio")

        // Показываем номер телефона
        binding.textView4.text = "Номер: ${userPhone ?: "не указан"}"

        // Кнопка "ошиблись номером"
        binding.button5.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Кнопка "подтвердить"
        binding.button6.setOnClickListener {
            val code = binding.editTextNumber.text.toString()
            if (checkCode(code)) {
                Toast.makeText(this, "Код подтверждён!", Toast.LENGTH_SHORT).show()
                // Переход на главную страницу
                // val intent = Intent(this, MainPage::class.java)
                // startActivity(intent)
                // finish()
            } else {
                Toast.makeText(this, "Неправильный код", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCode(code: String): Boolean {
        // Здесь должна быть проверка кода с сервером
        return code.isNotEmpty() && code.length >= 4
    }

//    fun AddNewUser(user: User){
//        //отправляем данные нового пользователя в api
//
//    }




}
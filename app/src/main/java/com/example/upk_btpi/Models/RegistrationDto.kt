package com.example.upk_btpi.Models

// Запрос на регистрацию
data class RegistrationDto(
    val fullName: String,
    val phoneNumber: String,
    val password: String
)

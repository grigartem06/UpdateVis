package com.example.upk_btpi.Utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    fun handleApiError(response: Response<*>): String {
        val code = response.code()
        val rawError = try { response.errorBody()?.string()?.trim() } catch (e: Exception) { null }
        val serverMsg = extractServerMessage(rawError)

        return when (code) {
            400 -> serverMsg ?: "Неверные данные. Проверьте заполненные поля."
            401 -> "Сессия истекла. Пожалуйста, войдите заново."
            403 -> "У вас нет прав для выполнения этого действия."
            404 -> serverMsg ?: "Запрошенные данные не найдены."
            409 -> serverMsg ?: "Такая запись или пользователь уже существуют."
            422 -> serverMsg ?: "Не удалось обработать запрос. Проверьте данные."
            in 500..599 -> "Технические работы на сервере. Попробуйте позже."
            else -> serverMsg ?: "Ошибка запроса (код $code). Попробуйте позже."
        }
    }

    fun handleException(e: Throwable): String {
        return when (e) {
            is IOException -> "Нет подключения к интернету. Проверьте Wi-Fi или мобильные данные."
            is SocketTimeoutException -> "Сервер долго не отвечает. Проверьте соединение."
            is UnknownHostException -> "Не удалось подключиться к серверу."
            is java.net.ConnectException -> "Не удалось соединиться с сервером."
            else -> "Произошла ошибка: ${e.message ?: "Неизвестная причина"}"
        }
    }

    private fun extractServerMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val json = Gson().fromJson(raw, Map::class.java)
            (json["message"] as? String) ?: (json["error"] as? String) ?: (json["description"] as? String) ?: raw.take(150)
        } catch (ex: Exception) {
            raw.take(150)
        }
    }

    // ✅ ИСПРАВЛЕНО: onPositive имеет значение по умолчанию
    fun showDialog(
        context: Context,
        title: String,
        message: String,
        onPositive: () -> Unit = {}
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Хорошо") { dialog, which ->
                dialog.dismiss()
                onPositive()
            }
            .setCancelable(false)
            .show()
    }

    fun showErrorWithMessage(context: Context, error: Throwable) {
        showDialog(
            context = context,
            title = "Произошла ошибка",
            message = error.message ?: "Неизвестная причина сбоя"
        )
    }
}
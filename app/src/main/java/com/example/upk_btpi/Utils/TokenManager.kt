package com.example.upk_btpi.Utils

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREF_NAME = "auth_tokens"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) { prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            apply() }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun clearTokens() { prefs.edit().clear().apply() }

    fun isAuthorized(): Boolean = !getAccessToken().isNullOrEmpty()
}
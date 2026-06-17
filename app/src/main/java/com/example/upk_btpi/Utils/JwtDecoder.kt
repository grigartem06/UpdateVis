package com.example.upk_btpi.Utils

import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import android.util.Base64

object JwtDecoder {
    fun decode(token: String): Map<String, Any?> {
        return try {
            val parts = token.split(".")
            if(parts.size !=3 ) return emptyMap()

            var payload = parts[1]
            payload = payload.replace("-","+").replace("_","/")
            when(payload.length % 4) {
                2 -> payload += "=="
                3 -> payload += "="
            }
            val decoded = String(Base64.decode(payload, Base64.DEFAULT), Charsets.UTF_8)
            val json  = JSONObject(decoded)

            json.keys().asSequence().associateWith{
                key -> when(val value = json.get(key))
                {
                    is org.json.JSONObject->value.toString()
                    is org.json.JSONArray -> value.toString()
                else -> value
                }
            }

        }catch (e: Exception) { emptyMap() }
    }

    //конкретное поле
    fun getClaim(token: String , claimName: String) : String?{ return decode(token)[claimName]?.toString() }

    fun getPhoneFromToken(token: String): String? { return getClaim(token, "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone") }

    fun isExpired(token: String): Boolean {
        return try {
            val exp = decode(token)["exp"] as? Long ?: return true
            val now = System.currentTimeMillis() / 1000
            exp < now
        } catch (e: Exception) {
            true
        }
    }


}
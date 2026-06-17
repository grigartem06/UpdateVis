package com.example.upk_btpi.Retrofit


import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://btpit-ypk-api.somee.com/"

    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var refreshInterceptor: RefreshInterceptor
    private lateinit var retrofitClient: RetrofitClient


    lateinit var apiService: ApiService
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return

        // 1️⃣ Инициализируем AuthInterceptor с контекстом
        authInterceptor = AuthInterceptor(context)
        retrofitClient = RetrofitClient

        // 2️⃣ Создаём отдельный Retrofit для внутренних запросов
        val internalRetrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // 3️⃣ Создаём RefreshInterceptor с retrofit и authInterceptor
        refreshInterceptor = RefreshInterceptor(internalRetrofit, authInterceptor)

        // 4️⃣ Логгер
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 5️⃣ OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(refreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .serializeNulls()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(ApiService::class.java)
        isInitialized = true
    }

    fun getInstance(): ApiService {
        if (!::apiService.isInitialized) {
            throw IllegalStateException("RetrofitClient не инициализирован!")
        }
        return apiService
    }

    fun logout() {
        if (::authInterceptor.isInitialized) {
            AuthInterceptor.clearTokens()
        }
    }
}
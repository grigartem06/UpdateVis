package com.example.upk_btpi.Utils

import android.app.Application
import com.example.upk_btpi.Retrofit.AuthInterceptor

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        AuthInterceptor.init(this)
    }
}
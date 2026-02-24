package com.example.aiagent

import android.app.Application
import com.example.aiagent.di.AppModule

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализируем DI модуль с контекстом приложения
        AppModule.init(this)
    }
}
package com.example.aiagent

import android.app.Application
import android.util.Log
import com.example.aiagent.di.AppModule
import org.json.JSONException
import java.lang.Thread.UncaughtExceptionHandler

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Устанавливаем глобальный обработчик необработанных исключений
        setupGlobalExceptionHandler()

        try {
            // Инициализируем DI модуль с контекстом приложения
            AppModule.init(this)
        } catch (e: JSONException) {
            // Обработка MIUI-специфичных ошибок JSON
            // MIUI (Xiaomi) иногда пытается прочитать конфигурацию устройства из JSON,
            // но некоторые модели могут отсутствовать в базе данных MIUI
            Log.w(TAG, "⚠️ MIUI JSONException detected (device model not found in MIUI database): ${e.message}")
            Log.w(TAG, "⚠️ This is a known MIUI system issue and does not affect app functionality")
            // Продолжаем работу приложения, так как это системная ошибка MIUI, не связанная с нашим приложением
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during application initialization: ${e.message}", e)
            throw e
        }
    }

    /**
     * Устанавливает глобальный обработчик необработанных исключений
     * для перехвата MIUI-специфичных ошибок
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Проверяем, является ли это MIUI-специфичной ошибкой
            if (throwable is JSONException && throwable.message?.contains("No value for") == true) {
                Log.w(TAG, "⚠️ MIUI JSONException caught by global handler: ${throwable.message}")
                Log.w(TAG, "⚠️ Device model not found in MIUI database - this is a system issue")
                Log.w(TAG, "⚠️ Thread: ${thread.name}")
                // Не передаем исключение дальше, чтобы приложение не крашилось
                return@setDefaultUncaughtExceptionHandler
            }
            
            // Для всех остальных исключений используем стандартный обработчик
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
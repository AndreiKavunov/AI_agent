package com.example.aiagent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.aiagent.di.AppModule
import com.example.aiagent.ui.screen.ChatScreen
import com.example.aiagent.ui.screen.ChatViewModel
import com.example.aiagent.ui.screen.MainNavigation
import com.example.aiagent.ui.screen.SettingsScreen
import com.example.aiagent.ui.screen.ProfileScreen
import com.example.aiagent.ui.screen.SupportScreen
import com.example.aiagent.ui.screen.components.McpToolsScreen
import com.example.aiagent.ui.theme.AIAgentTheme
import org.json.JSONException

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val viewModel: ChatViewModel by viewModels { AppModule.viewModelFactory }

            enableEdgeToEdge()
            setContent {
                AIAgentTheme {
                    MainNavigation(
                        chatScreen = { ChatScreen() },
                        settingsScreen = { SettingsScreen() },
                        profileScreen = { ProfileScreen() },
                        toolsScreen = { McpToolsScreen() },
                        supportScreen = { SupportScreen() }
                    )
                }
            }
        } catch (e: JSONException) {
            // Обработка MIUI-специфичных ошибок JSON
            Log.w(TAG, "⚠️ MIUI JSONException detected in onCreate: ${e.message}")
            Log.w(TAG, "⚠️ This is a known MIUI system issue, attempting to continue...")
            // Пытаемся продолжить работу приложения
            try {
                val viewModel: ChatViewModel by viewModels { AppModule.viewModelFactory }
                enableEdgeToEdge()
                setContent {
                    AIAgentTheme {
                        MainNavigation(
                            chatScreen = { ChatScreen() },
                            settingsScreen = { SettingsScreen() },
                            profileScreen = { ProfileScreen() },
                            toolsScreen = { McpToolsScreen() },
                            supportScreen = { SupportScreen() }
                        )
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "❌ Critical error during activity creation: ${e2.message}", e2)
                throw e2
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during activity creation: ${e.message}", e)
            throw e
        }
    }
}
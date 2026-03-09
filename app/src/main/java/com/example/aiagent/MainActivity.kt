package com.example.aiagent

import android.os.Bundle
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
import com.example.aiagent.ui.screen.components.McpToolsScreen
import com.example.aiagent.ui.theme.AIAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: ChatViewModel by viewModels { AppModule.viewModelFactory }

        enableEdgeToEdge()
        setContent {
            AIAgentTheme {
                MainNavigation(
                    chatScreen = { ChatScreen() },
                    settingsScreen = { SettingsScreen() },
                    profileScreen = { ProfileScreen() },
                    toolsScreen = { McpToolsScreen() }
                )
            }
        }
    }
}
package com.example.aiagent.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.aiagent.di.AppModule
import com.example.aiagent.ui.theme.AIAgentTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: ChatViewModel by viewModels { AppModule.viewModelFactory }

        enableEdgeToEdge()
        setContent {
            AIAgentTheme {
                ChatScreen(viewModel = viewModel)
            }
        }
    }
}


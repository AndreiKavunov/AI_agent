// ui/screen/components/ChatTopBar.kt
package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.ui.screen.ChatState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    state: ChatState,
    onNewChat: () -> Unit,
    onToggleRepository: () -> Unit,
    onToggleTemperature: () -> Unit,
    onToggleContextSettings: () -> Unit,
    onShowTokenDetails: () -> Unit,
    onToggleProfile: () -> Unit,
    showTemperature: Boolean,
    showContextSettings: Boolean
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            androidx.compose.foundation.layout.Column {
                Text("Чат-агент")
                Text(
                    text = when (state.currentRepositoryType) {
                        RepositoryType.GIGACHAT -> "GigaChat • ${state.currentContextStrategy.name}"
                        RepositoryType.HUGGINGFACE -> "HuggingFace: ${state.huggingFaceModel.displayName} • ${state.currentContextStrategy.name}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            // Информация о токенах
            IconButton(
                onClick = onShowTokenDetails
            ) {
                BadgedIcon(
                    badgeCount = state.tokenStats.totalTokens,
                    icon = Icons.Outlined.Info,
                    contentDescription = "Информация о токенах"
                )
            }

            // Кнопка профиля пользователя
            IconButton(onClick = onToggleProfile) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Профиль пользователя",
                    tint = if (state.showProfileDialog) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Кнопка настроек контекста
            IconButton(onClick = onToggleContextSettings) {
                Icon(
                    imageVector = Icons.Default.Article,
                    contentDescription = "Настройки контекста",
                    tint = if (showContextSettings) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Кнопка нового чата
            IconButton(onClick = onNewChat) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Новый чат",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Кнопка переключения репозитория
            IconButton(onClick = onToggleRepository) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Переключить провайдера",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Кнопка настроек температуры
            IconButton(onClick = onToggleTemperature) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = if (showTemperature) {
                        "Скрыть настройки температуры"
                    } else {
                        "Показать настройки температуры"
                    },
                    tint = if (showTemperature) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    )
}

@Composable
fun BadgedIcon(
    badgeCount: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?
) {
    androidx.compose.foundation.layout.Box {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (badgeCount > 0) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .padding(2.dp)
            ) {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = minOf(badgeCount, 99).toString(),
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(1.dp)
                    )
                }
            }
        }
    }
}

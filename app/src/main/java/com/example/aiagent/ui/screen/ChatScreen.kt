// ui/screen/ChatScreen.kt
package com.example.aiagent.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.di.AppModule
import com.example.aiagent.domain.RepositoryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(factory = AppModule.viewModelFactory)
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showTemperature by remember { mutableStateOf(false) }
    var showRepositorySelector by remember { mutableStateOf(false) }
    var showHuggingFaceModelSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ChatTopBar(
                state = state,
                onNewChat = { viewModel.handleAction(ChatAction.NewChat) },
                onToggleRepository = { showRepositorySelector = !showRepositorySelector },
                onToggleTemperature = { showTemperature = !showTemperature },
                onShowTokenDetails = { viewModel.handleAction(ChatAction.ShowTokenDetails) },
                showTemperature = showTemperature
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Селектор репозитория
            AnimatedVisibility(
                visible = showRepositorySelector,
                enter = fadeIn() + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300)
                ),
                exit = fadeOut() + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300)
                )
            ) {
                RepositorySelector(
                    currentType = state.currentRepositoryType,
                    onTypeSelected = { type ->
                        viewModel.handleAction(ChatAction.SwitchRepository(type))
                        showRepositorySelector = false
                        if (type == RepositoryType.HUGGINGFACE) {
                            showHuggingFaceModelSelector = true
                        }
                    }
                )
            }

            // Селектор модели HuggingFace
            AnimatedVisibility(
                visible = showHuggingFaceModelSelector && state.currentRepositoryType == RepositoryType.HUGGINGFACE,
                enter = fadeIn() + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300)
                ),
                exit = fadeOut() + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300)
                )
            ) {
                HuggingFaceModelSelector(
                    currentModel = state.huggingFaceModel,
                    onModelSelected = { model ->
                        viewModel.handleAction(ChatAction.SelectHuggingFaceModel(model))
                        showHuggingFaceModelSelector = false
                    }
                )
            }

            // Панель с температурой
            AnimatedVisibility(
                visible = showTemperature,
                enter = fadeIn() + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300)
                ),
                exit = fadeOut() + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300)
                )
            ) {
                TemperatureSlider(
                    temperature = state.temperature,
                    onTemperatureChange = { newTemperature ->
                        viewModel.handleAction(ChatAction.UpdateTemperature(newTemperature))
                    }
                )
            }

            // Детальная информация о токенах
            AnimatedVisibility(
                visible = state.showTokenDetails,
                enter = fadeIn() + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300)
                ),
                exit = fadeOut() + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300)
                )
            ) {
                TokenStatsCard(
                    stats = state.tokenStats,
                    onDismiss = { viewModel.handleAction(ChatAction.HideTokenDetails) } // Добавляем вызов
                )
            }

            // Статистика последнего ответа
            state.lastResponse?.let { response ->
                LastResponseStats(
                    responseInfo = response,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Список сообщений
            MessagesList(
                messages = state.messages,
                isLoading = state.isLoading,
                onAgentMessageLongClick = { messageContent ->
                    copyToClipboard(context, messageContent)
                },
                modifier = Modifier.weight(1f)
            )

            // Поле ввода
            MessageInput(
                inputText = state.inputText,
                isLoading = state.isLoading,
                onInputChange = { viewModel.handleAction(ChatAction.UpdateInput(it)) },
                onSendClick = { viewModel.handleAction(ChatAction.SendMessage(state.inputText)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Отображение ошибки
            state.error?.let { error ->
                ErrorMessage(
                    error = error,
                    onDismiss = { viewModel.handleAction(ChatAction.ClearError) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    state: ChatState,
    onNewChat: () -> Unit,
    onToggleRepository: () -> Unit,
    onToggleTemperature: () -> Unit,
    onShowTokenDetails: () -> Unit,
    showTemperature: Boolean
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = {
            Column {
                Text("Чат-агент")
                Text(
                    text = when (state.currentRepositoryType) {
                        RepositoryType.GIGACHAT -> "GigaChat"
                        RepositoryType.HUGGINGFACE -> "HuggingFace: ${state.huggingFaceModel.displayName}"
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
    Box {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .padding(2.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TokenStatsCard(
    stats: TokenStats,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Статистика токенов",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(
                    onClick = onDismiss, // Теперь вызывает hideTokenDetails
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TokenProgressBar(stats)

            Spacer(modifier = Modifier.height(12.dp))

            TokenStatsDetails(stats)
        }
    }
}

@Composable
fun TokenProgressBar(stats: TokenStats) {
    if (stats.totalTokens == 0) {
        Text(
            text = "Нет данных о токенах",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        return
    }

    Column {
        // Системные токены
        TokenProgressSegment(
            label = "Системные",
            value = stats.systemTokens,
            total = stats.totalTokens,
            color = MaterialTheme.colorScheme.tertiary
        )

        // Токены пользователя
        TokenProgressSegment(
            label = "Пользователь",
            value = stats.userTokens,
            total = stats.totalTokens,
            color = MaterialTheme.colorScheme.primary
        )

        // Токены ассистента
        TokenProgressSegment(
            label = "Ассистент",
            value = stats.assistantTokens,
            total = stats.totalTokens,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun TokenProgressSegment(
    label: String,
    value: Int,
    total: Int,
    color: androidx.compose.ui.graphics.Color
) {
    val percentage = if (total > 0) value.toFloat() / total else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = "$value (${(percentage * 100).toInt()}%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold
        )
    }

    // Прогресс бар
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(vertical = 2.dp)
    ) {
        // Фон
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
            )
        ) {}

        // Заполнение
        Card(
            modifier = Modifier
                .fillMaxWidth(percentage)
                .height(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = color
            )
        ) {}
    }

    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun TokenStatsDetails(stats: TokenStats) {
    Column {
        DetailRow("Всего токенов:", stats.totalTokens.toString())
        DetailRow("Последний ответ:", stats.lastResponseTokens?.toString() ?: "нет данных")
        DetailRow("Текущий запрос:", stats.currentQueryTokens.toString())

        if (stats.totalTokens > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Примерная стоимость: ~$${"%.6f".format(stats.totalTokens * 0.000002)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LastResponseStats(
    responseInfo: LastResponseInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Время ответа
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime(responseInfo.timeMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Токены в ответе
            Text(
                text = "${responseInfo.tokenCount} отв.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )

            // Токены в промпте (если есть)
            responseInfo.promptTokens?.let { promptTokens ->
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${promptTokens} пром.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            // Скорость
            responseInfo.tokensPerSecond?.let { tps ->
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${String.format("%.1f", tps)} ток/с",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RepositorySelector(
    currentType: RepositoryType,
    onTypeSelected: (RepositoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Выберите провайдера:",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            RepositoryOption(
                type = RepositoryType.GIGACHAT,
                displayName = "GigaChat",
                description = "Модели от Сбера",
                isSelected = currentType == RepositoryType.GIGACHAT,
                onClick = { onTypeSelected(RepositoryType.GIGACHAT) }
            )

            RepositoryOption(
                type = RepositoryType.HUGGINGFACE,
                displayName = "HuggingFace",
                description = "Открытые модели (Llama, Mistral)",
                isSelected = currentType == RepositoryType.HUGGINGFACE,
                onClick = { onTypeSelected(RepositoryType.HUGGINGFACE) }
            )
        }
    }
}

@Composable
fun RepositoryOption(
    type: RepositoryType,
    displayName: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HuggingFaceModelSelector(
    currentModel: HuggingFaceModel,
    onModelSelected: (HuggingFaceModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Выберите модель HuggingFace:",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            HuggingFaceModel.values().forEach { model ->
                ModelOption(
                    model = model,
                    isSelected = currentModel == model,
                    onClick = { onModelSelected(model) }
                )
            }
        }
    }
}

@Composable
fun ModelOption(
    model: HuggingFaceModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MessagesList(
    messages: List<Message>,
    isLoading: Boolean,
    onAgentMessageLongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty() && !isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👋 Добро пожаловать!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Начните новый диалог",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            item {
                if (isLoading) {
                    LoadingIndicator()
                }
            }

            items(messages.reversed()) { message ->
                when (message) {
                    is Message.UserMessage -> UserMessageItem(message)
                    is Message.AgentMessage -> AgentMessageItem(
                        message = message,
                        onLongClick = { onAgentMessageLongClick(message.content) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserMessageItem(message: Message.UserMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Вы",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AgentMessageItem(
    message: Message.AgentMessage,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick,
                onLongClickLabel = "Копировать сообщение"
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Верхняя строка с метаданными
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Агент",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Статистика ответа
                    if (message.responseTimeMs > 0 || message.tokenCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Время ответа
                        if (message.responseTimeMs > 0) {
                            Text(
                                text = formatTime(message.responseTimeMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Количество токенов
                        if (message.tokenCount > 0) {
                            if (message.responseTimeMs > 0) {
                                Text(
                                    text = " | ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = "${message.tokenCount} ток.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Токены в промпте (если есть)
                        message.promptTokens?.let { promptTokens ->
                            Text(
                                text = " | ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${promptTokens} пром.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row {
                    // Инструмент (если есть)
                    message.toolUsed?.let { tool ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tool,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Время получения ответа
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Текст сообщения
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Подсказка о копировании
            Text(
                text = "Долгое нажатие для копирования",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun MessageInput(
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = { Text("Введите сообщение...") },
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { onSendClick() }
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSendClick,
            enabled = inputText.isNotBlank() && !isLoading
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Отправить"
            )
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

@Composable
fun ErrorMessage(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun TemperatureSlider(
    temperature: Double,
    onTemperatureChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Температура: ${String.format("%.1f", temperature)}",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = temperature.toFloat(),
                onValueChange = { onTemperatureChange(it.toDouble()) },
                valueRange = 0.0f..2.0f,
                steps = 20
            )
            Text(
                text = "Влияет на креативность ответов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Вспомогательные функции
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("agent_message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Сообщение скопировано", Toast.LENGTH_SHORT).show()
}

private fun formatTime(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${String.format("%.1f", ms / 1000.0)}с"
        else -> "${String.format("%.1f", ms / 60000.0)}мин"
    }
}
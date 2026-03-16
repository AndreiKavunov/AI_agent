// ui/screen/components/MessagesList.kt
package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiagent.ui.screen.Message
import com.example.aiagent.ui.screen.components.utils.formatTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesList(
    messages: List<Message>,
    isLoading: Boolean,
    onAgentMessageLongClick: (String) -> Unit,
    onMessageLongClickForBranch: ((String) -> Unit)?,
    ragContext: com.example.aiagent.data.rag.RagContext? = null,
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
                if (onMessageLongClickForBranch != null) {
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "💡 Долгое нажатие на сообщение создаст ветку",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
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
                    is Message.UserMessage -> UserMessageItem(
                        message = message,
                        onLongClick = onMessageLongClickForBranch?.let { { it(message.id) } }
                    )
                    is Message.AgentMessage -> AgentMessageItem(
                        message = message,
                        onLongClick = { onAgentMessageLongClick(message.content) },
                        onBranchClick = onMessageLongClickForBranch?.let { { it(message.id) } },
                        ragContext = ragContext
                    )
                    is Message.SystemMessage -> SystemMessageItem(message)
                }
            }
        }
    }
}

@Composable
fun UserMessageItem(
    message: Message.UserMessage,
    onLongClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { },
                        onLongClick = onLongClick,
                        onLongClickLabel = "Создать ветку от этого сообщения"
                    )
                } else {
                    Modifier
                }
            ),
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

            if (onLongClick != null) {
                Text(
                    text = "Долгое нажатие для создания ветки",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AgentMessageItem(
    message: Message.AgentMessage,
    onLongClick: () -> Unit,
    onBranchClick: (() -> Unit)?,
    ragContext: com.example.aiagent.data.rag.RagContext? = null
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

            // RAG источники (если есть)
            if (ragContext != null && ragContext.documents.isNotEmpty()) {
                RagSourcesDisplay(ragContext = ragContext)
            }

            // Подсказки
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (onBranchClick != null) {
                    Text(
                        text = "Долгое нажатие для создания ветки",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = "Долгое нажатие для копирования",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun SystemMessageItem(
    message: Message.SystemMessage
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
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
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

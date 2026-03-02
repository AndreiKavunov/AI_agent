// ui/screen/components/StatsPanels.kt
package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiagent.ui.screen.LastResponseInfo
import com.example.aiagent.ui.screen.TokenStats
import com.example.aiagent.ui.screen.components.utils.formatTime

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
                    onClick = onDismiss,
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

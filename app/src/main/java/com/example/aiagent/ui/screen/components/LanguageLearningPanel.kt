// ui/screen/components/LanguageLearningPanel.kt
package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Панель настроек для изучения языков
 * Позволяет пользователю настроить параметры обучения
 */
@Composable
fun LanguageLearningPanel(
    languageToLearn: String?,
    learningGoal: String?,
    currentLevel: String?,
    lessonsCompleted: Int,
    onLanguageChange: (String) -> Unit,
    onGoalChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📚 Изучение языка",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { /* TODO: закрыть панель */ }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Выбор языка
            OutlinedTextField(
                value = languageToLearn ?: "",
                onValueChange = onLanguageChange,
                label = { Text("Какой язык изучаете?") },
                placeholder = { Text("Например: English, Spanish, French...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Цель обучения
            OutlinedTextField(
                value = learningGoal ?: "",
                onValueChange = onGoalChange,
                label = { Text("Цель обучения") },
                placeholder = { Text("Например: разговор, перевод, грамматика...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Текущий уровень
            OutlinedTextField(
                value = currentLevel ?: "",
                onValueChange = onLevelChange,
                label = { Text("Ваш текущий уровень") },
                placeholder = { Text("Например: A1, A2, B1, B2, C1, C2") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Статистика прогресса
            ProgressStats(
                lessonsCompleted = lessonsCompleted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Компонент для отображения статистики прогресса
 */
@Composable
fun ProgressStats(
    lessonsCompleted: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "$lessonsCompleted",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "уроков пройдено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Уровни владения языком
            LanguageLevelIndicator(
                level = "A1",
                isCompleted = true
            )
            LanguageLevelIndicator(
                level = "A2",
                isCompleted = true
            )
            LanguageLevelIndicator(
                level = "B1",
                isCompleted = lessonsCompleted >= 10
            )
            LanguageLevelIndicator(
                level = "B2",
                isCompleted = false
            )
            LanguageLevelIndicator(
                level = "C1",
                isCompleted = false
            )
            LanguageLevelIndicator(
                level = "C2",
                isCompleted = false
            )
        }
    }
}

/**
 * Индикатор уровня владения языком
 */
@Composable
fun LanguageLevelIndicator(
    level: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = level,
            style = MaterialTheme.typography.labelMedium,
            color = if (isCompleted) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
            },
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
        )
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Панель с достижениями
 */
@Composable
fun AchievementsPanel(
    achievements: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "🏆 Достижения",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (achievements.isEmpty()) {
                Text(
                    text = "Пока нет достижений. Продолжайте учиться!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            } else {
                achievements.forEach { achievement ->
                    Text(
                        text = "• $achievement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

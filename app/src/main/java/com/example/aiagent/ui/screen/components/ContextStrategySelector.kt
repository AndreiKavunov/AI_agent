// ui/screen/components/ContextStrategySelector.kt
package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiagent.domain.contextStrategy.ContextStrategy

@Composable
fun ContextStrategySelector(
    currentStrategy: ContextStrategy,
    slidingWindowSize: Int,
    onStrategySelected: (ContextStrategy) -> Unit,
    onSlidingWindowSizeChange: (Int) -> Unit,
    onDismiss: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Стратегия управления контекстом",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Закрыть"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sliding Window стратегия
            StrategyOption(
                strategy = ContextStrategy.SlidingWindow(slidingWindowSize),
                isSelected = currentStrategy is ContextStrategy.SlidingWindow,
                onClick = { onStrategySelected(ContextStrategy.SlidingWindow(slidingWindowSize)) }
            )

            if (currentStrategy is ContextStrategy.SlidingWindow) {
                SlidingWindowSizeControl(
                    size = slidingWindowSize,
                    onSizeChange = onSlidingWindowSizeChange
                )
            }

            // Sticky Facts стратегия
            StrategyOption(
                strategy = ContextStrategy.StickyFacts(slidingWindowSize),
                isSelected = currentStrategy is ContextStrategy.StickyFacts,
                onClick = { onStrategySelected(ContextStrategy.StickyFacts(slidingWindowSize)) }
            )

            // Branching стратегия
            StrategyOption(
                strategy = ContextStrategy.Branching(),
                isSelected = currentStrategy is ContextStrategy.Branching,
                onClick = { onStrategySelected(ContextStrategy.Branching()) }
            )

            // Language Learning стратегия
            StrategyOption(
                strategy = ContextStrategy.LanguageLearning(),
                isSelected = currentStrategy is ContextStrategy.LanguageLearning,
                onClick = { onStrategySelected(ContextStrategy.LanguageLearning()) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Текущая стратегия: ${currentStrategy.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = currentStrategy.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StrategyOption(
    strategy: ContextStrategy,
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
                text = strategy.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = strategy.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SlidingWindowSizeControl(
    size: Int,
    onSizeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 4.dp, bottom = 8.dp)
    ) {
        Text(
            text = "Размер окна: $size сообщений",
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = size.toFloat(),
            onValueChange = { onSizeChange(it.toInt()) },
            valueRange = 1f..30f,
            steps = 29
        )
    }
}

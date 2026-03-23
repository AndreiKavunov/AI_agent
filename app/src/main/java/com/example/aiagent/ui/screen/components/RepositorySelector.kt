// ui/screen/components/RepositorySelector.kt
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.domain.RepositoryType

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

            RepositoryOption(
                type = RepositoryType.LOCAL,
                displayName = "Local (Ollama)",
                description = "Локальная модель phi3",
                isSelected = currentType == RepositoryType.LOCAL,
                onClick = { onTypeSelected(RepositoryType.LOCAL) }
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
                    text = "Выберите модель HuggingFace:",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HuggingFaceModel.values().forEach { model ->
                ModelOption(
                    model = model,
                    isSelected = currentModel == model,
                    onClick = {
                        onModelSelected(model)
                        onDismiss()
                    }
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

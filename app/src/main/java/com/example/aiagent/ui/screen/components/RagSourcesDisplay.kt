package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiagent.data.rag.RagContext
import com.example.aiagent.data.rag.RagDocumentSource

/**
 * Компонент для отображения источников RAG в сообщении ассистента
 */
@Composable
fun RagSourcesDisplay(
    ragContext: RagContext?,
    modifier: Modifier = Modifier
) {
    if (ragContext == null || ragContext.documents.isEmpty()) {
        return
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Заголовок с информацией о RAG
            RagContextHeader(
                ragContext = ragContext,
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded }
            )

            // Список источников
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                RagSourcesList(
                    sources = ragContext.documents,
                    strategy = ragContext.strategy
                )
            }
        }
    }
}

/**
 * Заголовок контекста RAG
 */
@Composable
fun RagContextHeader(
    ragContext: RagContext,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            
            Text(
                text = "Использовано ${ragContext.chunksUsed} фрагментов из ${ragContext.documents.size} источников",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Свернуть" else "Развернуть",
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Список источников RAG
 */
@Composable
fun RagSourcesList(
    sources: List<RagDocumentSource>,
    strategy: com.example.aiagent.data.rag.RagStrategy
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(sources, key = { it.filename + it.chunkId }) { source ->
            RagSourceItem(source = source)
        }
    }
}

/**
 * Элемент источника RAG
 */
@Composable
fun RagSourceItem(
    source: RagDocumentSource
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Название файла
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = source.filename,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Метаданные источника
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ID чанка
                SourceMetadataItem(
                    icon = Icons.Default.Tag,
                    label = "Чанк",
                    value = "#${source.chunkId}"
                )

                // Страница (если есть)
                source.page?.let { page ->
                    SourceMetadataItem(
                        icon = Icons.Default.MenuBook,
                        label = "Стр.",
                        value = "$page"
                    )
                }

                // Раздел (если есть)
                source.section?.let { section ->
                    SourceMetadataItem(
                        icon = Icons.Default.Category,
                        label = "Раздел",
                        value = section.take(15),
                        maxLines = 1
                    )
                }
            }

            // Релевантность (если есть)
            source.relevanceScore?.let { score ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (score > 0.7) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Релевантность: ${(score * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (score > 0.7) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Элемент метаданных источника
 */
@Composable
fun SourceMetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    maxLines: Int = Int.MAX_VALUE
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Индикатор использования RAG в сообщении
 */
@Composable
fun RagIndicator(
    ragContext: RagContext?,
    modifier: Modifier = Modifier
) {
    if (ragContext == null || ragContext.documents.isEmpty()) {
        return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${ragContext.documents.size} источников",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Бейдж стратегии RAG
 */
@Composable
fun RagStrategyBadge(
    strategy: com.example.aiagent.data.rag.RagStrategy,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when (strategy) {
            com.example.aiagent.data.rag.RagStrategy.FIXED -> 
                Color(0xFF2196F3).copy(alpha = 0.2f)
            com.example.aiagent.data.rag.RagStrategy.STRUCTURAL -> 
                Color(0xFF9C27B0).copy(alpha = 0.2f)
        }
    ) {
        Text(
            text = when (strategy) {
                com.example.aiagent.data.rag.RagStrategy.FIXED -> "Fixed"
                com.example.aiagent.data.rag.RagStrategy.STRUCTURAL -> "Structural"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when (strategy) {
                com.example.aiagent.data.rag.RagStrategy.FIXED -> Color(0xFF1976D2)
                com.example.aiagent.data.rag.RagStrategy.STRUCTURAL -> Color(0xFF7B1FA2)
            },
            fontWeight = FontWeight.Bold
        )
    }
}

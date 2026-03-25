package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    modifier: Modifier = Modifier,
    onSourceClick: ((RagDocumentSource) -> Unit)? = null
) {
    if (ragContext == null || ragContext.documents.isEmpty()) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var showQuotes by remember { mutableStateOf(false) }

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
                    strategy = ragContext.strategy,
                    onSourceClick = onSourceClick
                )

                // Кнопка для отображения цитат
                if (ragContext.quotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RagQuotesToggle(
                        quotesCount = ragContext.quotes.size,
                        showQuotes = showQuotes,
                        onToggleQuotes = { showQuotes = !showQuotes }
                    )

                    // Список цитат
                    if (showQuotes) {
                        Spacer(modifier = Modifier.height(8.dp))
                        RagQuotesList(quotes = ragContext.quotes)
                    }
                }
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
    strategy: com.example.aiagent.data.rag.RagStrategy,
    onSourceClick: ((RagDocumentSource) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sources.forEach { source ->
            RagSourceItem(source = source, onSourceClick = onSourceClick)
        }
    }
}

/**
 * Элемент источника RAG
 */
@Composable
fun RagSourceItem(
    source: RagDocumentSource,
    onSourceClick: ((RagDocumentSource) -> Unit)? = null
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
            // Название файла (кликабельное)
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
                    text = source.file,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = if (onSourceClick != null) TextDecoration.Underline else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .then(
                            if (onSourceClick != null) {
                                Modifier.clickable { onSourceClick(source) }
                            } else {
                                Modifier
                            }
                        )
                )
                if (onSourceClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Открыть источник",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
                    value = "#${source.chunk_id}"
                )

                // Раздел
                SourceMetadataItem(
                    icon = Icons.Default.Category,
                    label = "Раздел",
                    value = source.section.take(20),
                    maxLines = 1
                )
            }

            // Релевантность
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (source.relevance > 0.7) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Релевантность: ${(source.relevance * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (source.relevance > 0.7) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
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

/**
 * Кнопка для отображения/скрытия цитат
 */
@Composable
fun RagQuotesToggle(
    quotesCount: Int,
    showQuotes: Boolean,
    onToggleQuotes: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleQuotes),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Цитаты из документов ($quotesCount)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Icon(
                imageVector = if (showQuotes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showQuotes) "Свернуть цитаты" else "Развернуть цитаты",
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * Список цитат
 */
@Composable
fun RagQuotesList(
    quotes: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quotes.forEach { quote ->
            RagQuoteItem(quote = quote)
        }
    }
}

/**
 * Элемент цитаты
 */
@Composable
fun RagQuoteItem(
    quote: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = quote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

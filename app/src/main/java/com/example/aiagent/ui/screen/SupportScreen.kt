package com.example.aiagent.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiagent.data.support.FAQItem
import com.example.aiagent.data.support.Ticket


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: SupportViewModel = viewModel()
) {
    val supportUiState by viewModel.supportUiState.collectAsState()
    val ticketsUiState by viewModel.ticketsUiState.collectAsState()
    val faqUiState by viewModel.faqUiState.collectAsState()
    val selectedTicket by viewModel.selectedTicket.collectAsState()
    val userId by viewModel.userId.collectAsState()

    var questionText by remember { mutableStateOf("") }
    var userIdText by remember { mutableStateOf(userId ?: "") }
    var showFAQ by remember { mutableStateOf(false) }
    var showTickets by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поддержка") },
                actions = {
                    IconButton(onClick = { showFAQ = true }) {
                        Icon(Icons.Default.Help, contentDescription = "FAQ")
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        
        // Auto-scroll to response when it's displayed
        LaunchedEffect(supportUiState) {
            if (supportUiState is SupportUiState.Success) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User ID Input
            OutlinedTextField(
                value = userIdText,
                onValueChange = { 
                    userIdText = it
                    if (it.isNotEmpty()) {
                        viewModel.setUserId(it)
                    }
                },
                label = { Text("ID пользователя (опционально)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (userIdText.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.loadUserTickets(userIdText)
                            showTickets = true
                        }) {
                            Icon(Icons.Default.List, contentDescription = "Показать тикеты")
                        }
                    }
                }
            )

            // Selected Ticket Display
            selectedTicket?.let { ticket ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ticket.subject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Статус: ${ticket.status} | Приоритет: ${ticket.priority}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { viewModel.selectTicket(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить")
                        }
                    }
                }
            }

            // Question Input
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Ваш вопрос") },
                placeholder = { Text("Введите ваш вопрос...") },
                leadingIcon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // Send Button
            Button(
                onClick = {
                    if (questionText.isNotBlank()) {
                        viewModel.askQuestion(questionText)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = questionText.isNotBlank() && supportUiState !is SupportUiState.Loading
            ) {
                if (supportUiState is SupportUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Отправить")
                }
            }

            // Response Display
            when (supportUiState) {
                is SupportUiState.Success -> {
                    val response = (supportUiState as SupportUiState.Success).response
                    ResponseCard(response = response)
                }
                is SupportUiState.Error -> {
                    ErrorCard(message = (supportUiState as SupportUiState.Error).message)
                }
                else -> {}
            }
        }
    }

    // FAQ Dialog
    if (showFAQ) {
        FAQDialog(
            faqState = faqUiState,
            onDismiss = { showFAQ = false },
            onRetry = { viewModel.loadFAQ() }
        )
    }

    // Tickets Dialog
    if (showTickets) {
        TicketsDialog(
            ticketsState = ticketsUiState,
            onDismiss = { showTickets = false },
            onRetry = { viewModel.loadUserTickets(userIdText) },
            onTicketSelected = { ticket ->
                viewModel.selectTicket(ticket)
                showTickets = false
            }
        )
    }
}

@Composable
fun ResponseCard(response: com.example.aiagent.data.support.AskQuestionResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ответ поддержки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Context
            response.user_context?.let { context ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = context,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Ticket Context
            response.ticket_context?.let { context ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = context,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Main Answer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = response.answer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // RAG Context (Sources)
            if (response.rag_context.isNotEmpty()) {
                Text(
                    text = "Источники:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                response.rag_context.forEach { context ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "📄 ${context.file}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Раздел: ${context.section}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Релевантность: ${(context.relevance * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun FAQDialog(
    faqState: FAQUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Часто задаваемые вопросы") },
        text = {
            when (faqState) {
                is FAQUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is FAQUiState.Success -> {
                    val faq = (faqState as FAQUiState.Success).faq
                    if (faq.isEmpty()) {
                        Text("FAQ пуст")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(faq) { item ->
                                FAQItemCard(item = item)
                            }
                        }
                    }
                }
                is FAQUiState.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ошибка загрузки FAQ",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetry) {
                            Text("Повторить")
                        }
                    }
                }
                else -> {}
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun FAQItemCard(item: FAQItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.answer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TicketsDialog(
    ticketsState: TicketsUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onTicketSelected: (Ticket) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ваши тикеты") },
        text = {
            when (ticketsState) {
                is TicketsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is TicketsUiState.Success -> {
                    val tickets = (ticketsState as TicketsUiState.Success).tickets
                    if (tickets.isEmpty()) {
                        Text("У вас нет тикетов")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tickets) { ticket ->
                                TicketCard(
                                    ticket = ticket,
                                    onClick = { onTicketSelected(ticket) }
                                )
                            }
                        }
                    }
                }
                is TicketsUiState.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ошибка загрузки тикетов",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetry) {
                            Text("Повторить")
                        }
                    }
                }
                else -> {}
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun TicketCard(ticket: Ticket, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = ticket.status)
            }
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ID: ${ticket.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Приоритет: ${ticket.priority}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status.lowercase()) {
        "open" -> MaterialTheme.colorScheme.primary to "Открыт"
        "in_progress" -> MaterialTheme.colorScheme.tertiary to "В работе"
        "closed" -> MaterialTheme.colorScheme.secondary to "Закрыт"
        "resolved" -> Color(0xFF4CAF50) to "Решен"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 11.sp
        )
    }
}

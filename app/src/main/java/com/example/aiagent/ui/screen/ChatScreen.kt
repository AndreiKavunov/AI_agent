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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiagent.data.huggingFace.HuggingFaceModel
import com.example.aiagent.di.AppModule
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import com.example.aiagent.domain.contextStrategy.DialogBranch
import com.example.aiagent.domain.contextStrategy.DialogFact
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
    var showContextSettings by remember { mutableStateOf(false) }
    var showHuggingFaceSelector by remember { mutableStateOf(false) }
    var branchNameInput by remember { mutableStateOf("") }
    var selectedMessageForBranch by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            ChatTopBar(
                state = state,
                onNewChat = { viewModel.handleAction(ChatAction.NewChat) },
                onToggleRepository = { showRepositorySelector = !showRepositorySelector },
                onToggleTemperature = { showTemperature = !showTemperature },
                onToggleContextSettings = { showContextSettings = !showContextSettings },
                onShowTokenDetails = { viewModel.handleAction(ChatAction.ShowTokenDetails) },
                showTemperature = showTemperature,
                showContextSettings = showContextSettings
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
                            showHuggingFaceSelector = true
                        }
                    }
                )
            }

            // Селектор модели HuggingFace
            AnimatedVisibility(
                visible = showHuggingFaceSelector && state.currentRepositoryType == RepositoryType.HUGGINGFACE,
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
                        showHuggingFaceSelector = false
                    },
                    onDismiss = { showHuggingFaceSelector = false }
                )
            }

            // Панель настроек контекста
            AnimatedVisibility(
                visible = showContextSettings,
                enter = fadeIn() + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300)
                ),
                exit = fadeOut() + shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(300)
                )
            ) {
                ContextStrategySelector(
                    currentStrategy = state.currentContextStrategy,
                    slidingWindowSize = state.slidingWindowSize,
                    onStrategySelected = { strategy ->
                        viewModel.handleAction(ChatAction.SelectContextStrategy(strategy))
                    },
                    onSlidingWindowSizeChange = { size ->
                        viewModel.handleAction(ChatAction.UpdateSlidingWindowSize(size))
                    },
                    onDismiss = { showContextSettings = false }
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

            // Панель с фактами (для стратегии Sticky Facts)
            if (state.currentContextStrategy is ContextStrategy.StickyFacts && state.facts.isNotEmpty()) {
                FactsPanel(
                    facts = state.facts,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Панель с ветками (для стратегии Branching)
            if (state.currentContextStrategy is ContextStrategy.Branching && state.branches.isNotEmpty()) {
                BranchesPanel(
                    branches = state.branches,
                    currentBranchId = state.currentBranchId,
                    onSwitchBranch = { branchId ->
                        viewModel.handleAction(ChatAction.SwitchBranch(branchId))
                    },
                    onDeleteBranch = { branchId ->
                        viewModel.handleAction(ChatAction.DeleteBranch(branchId))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                    onDismiss = { viewModel.handleAction(ChatAction.HideTokenDetails) }
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
                onMessageLongClickForBranch = if (state.currentContextStrategy is ContextStrategy.Branching) { messageId ->
                    selectedMessageForBranch = messageId
                    branchNameInput = ""
                } else null,
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

    // Диалог создания ветки
    if (selectedMessageForBranch != null) {
        CreateBranchDialog(
            messageContent = state.messages.find { it.id == selectedMessageForBranch }?.content ?: "",
            branchName = branchNameInput,
            onBranchNameChange = { branchNameInput = it },
            onConfirm = {
                selectedMessageForBranch?.let { messageId ->
                    viewModel.handleAction(ChatAction.CreateBranch(messageId, branchNameInput))
                    selectedMessageForBranch = null
                    branchNameInput = ""
                }
            },
            onDismiss = {
                selectedMessageForBranch = null
                branchNameInput = ""
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    state: ChatState,
    onNewChat: () -> Unit,
    onToggleRepository: () -> Unit,
    onToggleTemperature: () -> Unit,
    onToggleContextSettings: () -> Unit,
    onShowTokenDetails: () -> Unit,
    showTemperature: Boolean,
    showContextSettings: Boolean
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
                        imageVector = Icons.Default.Close,
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

@Composable
fun FactsPanel(
    facts: Map<String, DialogFact>,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "📌 Важные факты",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            facts.values.take(5).forEach { fact ->
                FactItem(fact = fact)
            }

            if (facts.size > 5) {
                Text(
                    text = "... и еще ${facts.size - 5} фактов",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FactItem(fact: DialogFact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = fact.key.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } + ":",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = fact.value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
fun BranchesPanel(
    branches: List<DialogBranch>,
    currentBranchId: String?,
    onSwitchBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🌿 Ветки диалога",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Долгое нажатие на сообщение для создания ветки",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            branches.forEach { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = branch.id == currentBranchId,
                    onSwitch = { onSwitchBranch(branch.id) },
                    onDelete = { onDeleteBranch(branch.id) }
                )
            }
        }
    }
}

@Composable
fun BranchItem(
    branch: DialogBranch,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSwitch() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isCurrent,
                onClick = onSwitch
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = branch.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Сообщений: ${branch.messages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isCurrent) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить ветку",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CreateBranchDialog(
    messageContent: String,
    branchName: String,
    onBranchNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать ветку") },
        text = {
            Column {
                Text(
                    text = "Создать ветку от сообщения:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = messageContent.take(100) + if (messageContent.length > 100) "..." else "",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = branchName,
                    onValueChange = onBranchNameChange,
                    label = { Text("Название ветки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Text(
                text = "Создать",
                modifier = Modifier
                    .clickable(enabled = branchName.isNotBlank()) { onConfirm() }
                    .padding(8.dp),
                color = if (branchName.isNotBlank())
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        },
        dismissButton = {
            Text(
                text = "Отмена",
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
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

@Composable
fun MessagesList(
    messages: List<Message>,
    isLoading: Boolean,
    onAgentMessageLongClick: (String) -> Unit,
    onMessageLongClickForBranch: ((String) -> Unit)?,
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
                        onBranchClick = onMessageLongClickForBranch?.let { { it(message.id) } }
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
    onBranchClick: (() -> Unit)?
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
// ui/screen/ChatScreen.kt
package com.example.aiagent.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiagent.di.AppModule
import com.example.aiagent.domain.RepositoryType
import com.example.aiagent.domain.contextStrategy.ContextStrategy
import com.example.aiagent.ui.screen.components.*

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
    var showRagSettings by remember { mutableStateOf(false) }
    var showPrReviewDialog by remember { mutableStateOf(false) }
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
                onToggleProfile = { viewModel.handleAction(ChatAction.ShowProfileDialog) },
                onToggleRagSettings = { showRagSettings = !showRagSettings },
                onTogglePrReview = { showPrReviewDialog = !showPrReviewDialog },
                showTemperature = showTemperature,
                showContextSettings = showContextSettings
            )
        }
    ) { paddingValues ->
        // Показываем тост при изменении toastMessage
        LaunchedEffect(state.toastMessage) {
            state.toastMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                // Очищаем тост после показа
                viewModel.handleAction(ChatAction.ClearToast)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    },
                    maxTokens = state.maxTokens,
                    onMaxTokensChange = { newMaxTokens ->
                        viewModel.handleAction(ChatAction.UpdateMaxTokens(newMaxTokens))
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

            // Элементы управления workflow (показываем только если workflow активен или выбрана стратегия Workflow)
            if (state.workflowState.isActive || state.currentContextStrategy is com.example.aiagent.domain.contextStrategy.ContextStrategy.Workflow) {
                WorkflowControls(
                    workflowState = state.workflowState,
                    onAction = { viewModel.handleAction(it) }
                )
            }

            // Список сообщений
            MessagesList(
                messages = state.messages,
                isLoading = state.isLoading,
                onAgentMessageLongClick = { messageContent ->
                    com.example.aiagent.ui.screen.components.utils.copyToClipboard(context, messageContent)
                },
                onMessageLongClickForBranch = if (state.currentContextStrategy is ContextStrategy.Branching) { messageId ->
                    selectedMessageForBranch = messageId
                    branchNameInput = ""
                } else null,
                onRagSourceClick = { source ->
                    // Показываем информацию о кликнутом источнике
                    val sourceInfo = "📄 Файл: ${source.file}\n" +
                            "📍 Раздел: ${source.section}\n" +
                            "🏷️ Чанк ID: ${source.chunk_id}\n" +
                            "⭐ Релевантность: ${(source.relevance * 100).toInt()}%"
                    Toast.makeText(context, sourceInfo, Toast.LENGTH_LONG).show()
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

    // Диалог настроек пользователя
    if (state.showProfileDialog) {
        UserProfileDialog(
            profile = state.userSettings,
            onDismiss = { viewModel.handleAction(ChatAction.HideProfileDialog) },
            onSave = { settings ->
                viewModel.handleAction(ChatAction.UpdateUserSettings(settings))
            }
        )
    }

    // Диалог настроек RAG
    if (showRagSettings) {
        RagSettingsPanel(
            state = state,
            onToggleRag = { viewModel.toggleRag() },
            onSetStrategy = { viewModel.setRagStrategy(it) },
            onBuildIndex = { viewModel.buildRagIndex(it) },
            onDismiss = { showRagSettings = false }
        )
    }

    // Диалог PR Review
    if (showPrReviewDialog) {
        PrReviewDialog(
            onDismiss = { showPrReviewDialog = false },
            onReviewRequest = { prNumber, repo, diff ->
                // Send the review request as a message to the AI agent
                val reviewMessage = buildString {
                    appendLine("🔍 PR Review Request")
                    appendLine("PR #$prNumber in $repo")
                    appendLine()
                    appendLine("Please review this Pull Request diff:")
                    appendLine()
                    appendLine(diff)
                }
                viewModel.handleAction(ChatAction.SendMessage(reviewMessage))
            }
        )
    }
}

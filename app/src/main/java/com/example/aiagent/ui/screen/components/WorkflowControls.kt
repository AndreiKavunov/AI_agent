// ui/screen/components/WorkflowControls.kt
package com.example.aiagent.ui.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiagent.domain.workflow.WorkflowStage
import com.example.aiagent.domain.workflow.WorkflowState
import com.example.aiagent.ui.screen.ChatAction

/**
 * Компонент для отображения состояния workflow и кнопок управления
 * Показывается только когда workflow активен
 */
@Composable
fun WorkflowControls(
    workflowState: WorkflowState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (workflowState.isActive) {
        // Если workflow активен, показываем этап и кнопки управления
        ActiveWorkflowControls(
            workflowState = workflowState,
            onAction = onAction,
            modifier = modifier
        )
    }
}

/**
 * Элементы управления для активного workflow
 */
@Composable
fun ActiveWorkflowControls(
    workflowState: WorkflowState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с текущим этапом
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Название этапа
                Text(
                    text = "Этап: ${workflowState.currentStage.displayName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Кнопка сброса
                IconButton(
                    onClick = { onAction(ChatAction.ResetWorkflow) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть workflow",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Индикатор прогресса этапов
            WorkflowProgressIndicator(
                currentStage = workflowState.currentStage
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка возврата (если можно)
                if (workflowState.canRetreat()) {
                    OutlinedButton(
                        onClick = { onAction(ChatAction.RetreatWorkflow) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = workflowState.currentStage.getPreviousButtonLabel(),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Кнопка перехода вперед (если можно)
                if (workflowState.canAdvance()) {
                    Button(
                        onClick = { onAction(ChatAction.AdvanceWorkflow) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (workflowState.currentStage) {
                                WorkflowStage.PLANNING -> Color(0xFF4CAF50)
                                WorkflowStage.EXECUTION -> Color(0xFF2196F3)
                                WorkflowStage.VALIDATION -> Color(0xFFFF9800)
                                WorkflowStage.DONE -> Color(0xFF9C27B0)
                            },
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = workflowState.currentStage.getNextButtonLabel(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (workflowState.currentStage == WorkflowStage.DONE) {
                    // Этап DONE - показываем кнопку завершения
                    Button(
                        onClick = { onAction(ChatAction.ResetWorkflow) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Начать заново",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Нет сохраненного ответа для текущего этапа
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "Ожидание ответа...",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Индикатор прогресса этапов workflow
 */
@Composable
fun WorkflowProgressIndicator(
    currentStage: WorkflowStage,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Прогресс бар этапов
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WorkflowStage.entries.forEach { stage ->
                val isCurrentStage = stage == currentStage
                val isPastStage = stage.ordinal < currentStage.ordinal
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            color = when {
                                isCurrentStage || isPastStage -> when (stage) {
                                    WorkflowStage.PLANNING -> Color(0xFF4CAF50)
                                    WorkflowStage.EXECUTION -> Color(0xFF2196F3)
                                    WorkflowStage.VALIDATION -> Color(0xFFFF9800)
                                    WorkflowStage.DONE -> Color(0xFF9C27B0)
                                }
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Подписи этапов
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WorkflowStage.entries.forEach { stage ->
                Text(
                    text = stage.displayName,
                    fontSize = 10.sp,
                    color = if (stage == currentStage) {
                        MaterialTheme.colorScheme.primary
                    } else if (stage.ordinal < currentStage.ordinal) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

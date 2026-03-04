// domain/workflow/WorkflowState.kt
package com.example.aiagent.domain.workflow

/**
 * Состояние рабочего процесса (workflow)
 * Хранит текущий этап и накопленные данные для каждого этапа
 */
data class WorkflowState(
    val isActive: Boolean = false,
    val currentStage: WorkflowStage = WorkflowStage.PLANNING,
    val originalRequest: String = "",
    val stageResponses: Map<WorkflowStage, StageResponse> = emptyMap()
) {
    /**
     * Получить ответ для текущего этапа
     */
    fun getCurrentStageResponse(): StageResponse? {
        return stageResponses[currentStage]
    }

    /**
     * Получить накопленный контекст для системного промпта
     */
    fun getAccumulatedContext(): String {
        return buildString {
            appendLine("=== КОНТЕКСТ РАБОЧЕГО ПРОЦЕССА ===")
            appendLine("Этап: ${currentStage.displayName}")
            appendLine()
            
            // Добавляем предыдущие этапы
            WorkflowStage.entries
                .filter { it.ordinal < currentStage.ordinal }
                .forEach { stage ->
                    stageResponses[stage]?.let { response ->
                        appendLine("--- ${stage.displayName} ---")
                        appendLine(response.content)
                        appendLine()
                    }
                }
            
            appendLine("===================================")
        }
    }

    /**
     * Создать копию с добавленным ответом для этапа
     */
    fun withStageResponse(stage: WorkflowStage, response: String): WorkflowState {
        return copy(
            stageResponses = stageResponses.toMutableMap().apply {
                put(stage, StageResponse(
                    stage = stage,
                    content = response,
                    timestamp = System.currentTimeMillis()
                ))
            }
        )
    }

    /**
     * Перейти к следующему этапу
     */
    fun advanceToNextStage(): WorkflowState? {
        val nextStage = currentStage.next() ?: return null
        return copy(currentStage = nextStage)
    }

    /**
     * Вернуться к предыдущему этапу
     */
    fun retreatToPreviousStage(): WorkflowState? {
        val previousStage = currentStage.previous() ?: return null
        return copy(currentStage = previousStage)
    }

    /**
     * Проверить, можно ли перейти к следующему этапу
     */
    fun canAdvance(): Boolean {
        return currentStage.next() != null && getCurrentStageResponse() != null
    }

    /**
     * Проверить, можно ли вернуться к предыдущему этапу
     */
    fun canRetreat(): Boolean {
        return currentStage.previous() != null
    }
}

/**
 * Ответ для этапа рабочего процесса
 */
data class StageResponse(
    val stage: WorkflowStage,
    val content: String,
    val timestamp: Long
)

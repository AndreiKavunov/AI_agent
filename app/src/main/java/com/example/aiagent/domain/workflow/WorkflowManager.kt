// domain/workflow/WorkflowManager.kt
package com.example.aiagent.domain.workflow

import android.util.Log

private const val TAG = "WorkflowManager"

/**
 * Менеджер рабочего процесса (workflow)
 * Управляет состоянием и переходами между этапами
 */
class WorkflowManager {
    private var currentWorkflow: WorkflowState = WorkflowState()

    /**
     * Запустить новый рабочий процесс
     */
    fun startWorkflow(request: String): WorkflowState {
        currentWorkflow = WorkflowState(
            isActive = true,
            currentStage = WorkflowStage.PLANNING,
            originalRequest = request
        )
        Log.d(TAG, "🚀 Запущен новый workflow: ${currentWorkflow.currentStage.displayName}")
        return currentWorkflow
    }
    
    /**
     * Обновить оригинальный запрос workflow
     */
    fun updateWorkflowRequest(request: String): WorkflowState {
        currentWorkflow = currentWorkflow.copy(originalRequest = request)
        Log.d(TAG, "📝 Обновлен запрос workflow: $request")
        return currentWorkflow
    }

    /**
     * Получить текущее состояние рабочего процесса
     */
    fun getCurrentWorkflow(): WorkflowState = currentWorkflow

    /**
     * Сохранить ответ для текущего этапа
     */
    fun saveStageResponse(response: String): WorkflowState {
        if (!currentWorkflow.isActive) {
            Log.w(TAG, "⚠️ Попытка сохранить ответ при неактивном workflow")
            return currentWorkflow
        }

        currentWorkflow = currentWorkflow.withStageResponse(currentWorkflow.currentStage, response)
        Log.d(TAG, "💾 Сохранен ответ для этапа: ${currentWorkflow.currentStage.displayName}")
        return currentWorkflow
    }

    /**
     * Перейти к следующему этапу
     */
    fun advanceToNextStage(): WorkflowState? {
        if (!currentWorkflow.isActive) {
            Log.w(TAG, "⚠️ Попытка перейти к следующему этапу при неактивном workflow")
            return null
        }

        if (!currentWorkflow.canAdvance()) {
            Log.w(TAG, "⚠️ Нельзя перейти к следующему этапу - нет ответа для текущего этапа")
            return null
        }

        val nextWorkflow = currentWorkflow.advanceToNextStage()
        if (nextWorkflow != null) {
            currentWorkflow = nextWorkflow
            Log.d(TAG, "➡️ Переход к этапу: ${currentWorkflow.currentStage.displayName}")
        }
        return currentWorkflow
    }

    /**
     * Вернуться к предыдущему этапу
     */
    fun retreatToPreviousStage(): WorkflowState? {
        if (!currentWorkflow.isActive) {
            Log.w(TAG, "⚠️ Попытка вернуться к предыдущему этапу при неактивном workflow")
            return null
        }

        if (!currentWorkflow.canRetreat()) {
            Log.w(TAG, "⚠️ Нельзя вернуться к предыдущему этапу - это первый этап")
            return null
        }

        val previousWorkflow = currentWorkflow.retreatToPreviousStage()
        if (previousWorkflow != null) {
            currentWorkflow = previousWorkflow
            Log.d(TAG, "⬅️ Возврат к этапу: ${currentWorkflow.currentStage.displayName}")
        }
        return currentWorkflow
    }

    /**
     * Сбросить рабочий процесс
     */
    fun resetWorkflow(): WorkflowState {
        currentWorkflow = WorkflowState()
        Log.d(TAG, "🔄 Workflow сброшен")
        return currentWorkflow
    }

    /**
     * Получить системный промпт для текущего этапа
     */
    fun getSystemPromptForCurrentStage(): String? {
        if (!currentWorkflow.isActive) {
            return null
        }
        
        return buildString {
            // Добавляем накопленный контекст
            appendLine(currentWorkflow.getAccumulatedContext())
            appendLine()
            
            // Добавляем промпт текущего этапа
            appendLine(currentWorkflow.currentStage.systemPrompt)
            appendLine()
            
            // Добавляем оригинальный запрос пользователя
            appendLine("Оригинальный запрос пользователя:")
            appendLine(currentWorkflow.originalRequest)
        }
    }

    /**
     * Проверить, активен ли рабочий процесс
     */
    fun isWorkflowActive(): Boolean = currentWorkflow.isActive

    /**
     * Получить текущий этап
     */
    fun getCurrentStage(): WorkflowStage? {
        return if (currentWorkflow.isActive) currentWorkflow.currentStage else null
    }
}

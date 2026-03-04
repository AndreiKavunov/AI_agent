// domain/workflow/WorkflowStage.kt
package com.example.aiagent.domain.workflow

/**
 * Этапы рабочего процесса (workflow) для пошагового выполнения задач
 */
enum class WorkflowStage(val displayName: String, val systemPrompt: String) {
    PLANNING(
        displayName = "Планирование",
        systemPrompt = "Ты на этапе планирования. Создай подробный план для выполнения задачи пользователя. План должен быть структурированным и включать конкретные шаги."
    ),
    EXECUTION(
        displayName = "Выполнение",
        systemPrompt = "Ты на этапе выполнения. Реализуй план, который был утвержден на этапе планирования. Следуй шагам плана и предоставляй результаты."
    ),
    VALIDATION(
        displayName = "Валидация",
        systemPrompt = "Ты на этапе валидации. Проверь результаты выполнения плана и убедись, что все требования пользователя выполнены корректно."
    ),
    DONE(
        displayName = "Готово",
        systemPrompt = "Задача выполнена. Подведи итоги и предоставь финальный результат."
    );

    /**
     * Получить следующий этап
     */
    fun next(): WorkflowStage? {
        return when (this) {
            PLANNING -> EXECUTION
            EXECUTION -> VALIDATION
            VALIDATION -> DONE
            DONE -> null
        }
    }

    /**
     * Получить предыдущий этап
     */
    fun previous(): WorkflowStage? {
        return when (this) {
            PLANNING -> null
            EXECUTION -> PLANNING
            VALIDATION -> EXECUTION
            DONE -> VALIDATION
        }
    }

    /**
     * Получить текст кнопки для перехода к следующему этапу
     */
    fun getNextButtonLabel(): String {
        return when (this) {
            PLANNING -> "Перейти к выполнению →"
            EXECUTION -> "Перейти к валидации →"
            VALIDATION -> "Завершить ✓"
            DONE -> "Готово"
        }
    }

    /**
     * Получить текст кнопки для возвращения к предыдущему этапу
     */
    fun getPreviousButtonLabel(): String {
        return when (this) {
            PLANNING -> ""
            EXECUTION -> "← Вернуться к плану"
            VALIDATION -> "← Вернуться к выполнению"
            DONE -> "← Вернуться к валидации"
        }
    }
}

package com.example.aiagent.data.huggingFace

enum class HuggingFaceModel(
    val modelId: String,
    val displayName: String,
    val description: String,
    val taskType: TaskType
) {
    // СЛАБАЯ - модель для анализа тональности (классификация)
    WEAK_ANALYSIS_1(
        modelId = "cardiffnlp/twitter-roberta-base-sentiment-latest",
        displayName = "RoBERTa",
        description = "Анализ тональности текста, 125M параметров",
        taskType = TaskType.TEXT_CLASSIFICATION
    ),
    WEAK_ANALYSIS_2(
        modelId = "cardiffnlp/twitter-roberta-base-sentiment-latest",
        displayName = "Анализ тональности",
        description = "Определяет позитив/негатив текста",
        taskType = TaskType.TEXT_CLASSIFICATION
    ),

    WEAK(
        modelId = "meta-llama/Llama-3.2-3B-Instruct",
        displayName = "Llama-3.2-3B (Слабая)",
        description = "3B, компактная",
        taskType = TaskType.TEXT_GENERATION
    ),

    MEDIUM(
        modelId = "mistralai/Mistral-7B-Instruct-v0.2", // v0.2 вместо v0.3
        displayName = "Mistral-7B v0.2 (Средняя)",
        description = "7B, предыдущая версия",
        taskType = TaskType.TEXT_GENERATION
    ),
    // СИЛЬНАЯ - Мощная чат-модель от Meta
    STRONG(
        modelId = "meta-llama/Meta-Llama-3-8B-Instruct",
        displayName = "Llama-3-8B (Сильная)",
        description = "8B параметров, одна из лучших open-source моделей",
        taskType = TaskType.TEXT_GENERATION
    ),
}

enum class TaskType {
    TEXT_CLASSIFICATION,
    TEXT_GENERATION,
    QUESTION_ANSWERING
}
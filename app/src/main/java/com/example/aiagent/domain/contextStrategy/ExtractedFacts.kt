package com.example.aiagent.domain.contextStrategy

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedFacts(
    val user_name: String? = null,
    val user_full_name: String? = null,
    val age: Int? = null,
    val likes: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val occupation: String? = null,
    val profession: String? = null,
    val agreements: List<String> = emptyList(),
    val decisions: List<String> = emptyList()
) {
    fun toDialogFacts(): Map<String, DialogFact> {
        val facts = mutableMapOf<String, DialogFact>()
        val timestamp = System.currentTimeMillis()

        user_name?.let {
            facts["user_name"] = DialogFact("user_name", it, 1.0f, timestamp)
        }
        user_full_name?.let {
            facts["user_full_name"] = DialogFact("user_full_name", it, 1.0f, timestamp)
        }
        age?.let {
            facts["age"] = DialogFact("age", it.toString(), 1.0f, timestamp)
        }
        occupation?.let {
            facts["occupation"] = DialogFact("occupation", it, 0.9f, timestamp)
        }
        profession?.let {
            facts["profession"] = DialogFact("profession", it, 0.9f, timestamp)
        }

        likes.forEachIndexed { index, item ->
            facts["likes_$index"] = DialogFact("likes", item, 0.9f, timestamp)
        }
        dislikes.forEachIndexed { index, item ->
            facts["dislikes_$index"] = DialogFact("dislikes", item, 0.9f, timestamp)
        }
        goals.forEachIndexed { index, item ->
            facts["goals_$index"] = DialogFact("goals", item, 0.8f, timestamp)
        }
        limitations.forEachIndexed { index, item ->
            facts["limitations_$index"] = DialogFact("limitations", item, 0.8f, timestamp)
        }
        agreements.forEachIndexed { index, item ->
            facts["agreements_$index"] = DialogFact("agreements", item, 0.9f, timestamp)
        }
        decisions.forEachIndexed { index, item ->
            facts["decisions_$index"] = DialogFact("decisions", item, 0.9f, timestamp)
        }

        return facts
    }

    fun formatForSystemPrompt(): String {
        if (isEmpty()) return ""

        val builder = StringBuilder("\n=== ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ ===\n")

        user_name?.let { builder.append("Имя: $it\n") }
        user_full_name?.let { builder.append("Полное имя: $it\n") }
        age?.let { builder.append("Возраст: $it\n") }
        occupation?.let { builder.append("Деятельность: $it\n") }
        profession?.let { builder.append("Профессия: $it\n") }

        if (likes.isNotEmpty()) {
            builder.append("Нравится: ${likes.joinToString(", ")}\n")
        }
        if (dislikes.isNotEmpty()) {
            builder.append("Не нравится: ${dislikes.joinToString(", ")}\n")
        }
        if (goals.isNotEmpty()) {
            builder.append("Цели: ${goals.joinToString(", ")}\n")
        }
        if (limitations.isNotEmpty()) {
            builder.append("Ограничения: ${limitations.joinToString(", ")}\n")
        }
        if (agreements.isNotEmpty()) {
            builder.append("Договоренности: ${agreements.joinToString(", ")}\n")
        }
        if (decisions.isNotEmpty()) {
            builder.append("Решения: ${decisions.joinToString(", ")}\n")
        }

        return builder.toString()
    }

    fun isEmpty(): Boolean {
        return user_name == null &&
                user_full_name == null &&
                age == null &&
                likes.isEmpty() &&
                dislikes.isEmpty() &&
                goals.isEmpty() &&
                limitations.isEmpty() &&
                occupation == null &&
                profession == null &&
                agreements.isEmpty() &&
                decisions.isEmpty()
    }
}
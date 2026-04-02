package com.example.aiagent.domain.fileops

import android.util.Log
import com.example.aiagent.data.mcp.FileAnalyzeResponse
import com.example.aiagent.data.mcp.FileListResponse
import com.example.aiagent.data.mcp.FileReadResponse
import com.example.aiagent.data.mcp.FileSearchResponse
import com.example.aiagent.data.mcp.FileWriteResponse
import com.example.aiagent.data.mcp.GenerateResponse
import com.example.aiagent.data.mcp.McpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "FileCommandProcessor"

data class FileCommandResult(
    val handled: Boolean,
    val response: String? = null,
    val error: String? = null
)

class FileCommandProcessor(
    private val mcpRepository: McpRepository
) {
    
    suspend fun processCommand(message: String): FileCommandResult = withContext(Dispatchers.IO) {
        val trimmedMessage = message.trim()
        
        Log.d(TAG, "🔍 Checking if message is a file command: $trimmedMessage")
        
        when {
            trimmedMessage.contains("найди", ignoreCase = true) && 
            (trimmedMessage.contains("где используется", ignoreCase = true) || 
             trimmedMessage.contains("место", ignoreCase = true) ||
             trimmedMessage.contains("search", ignoreCase = true)) -> {
                handleFileSearch(trimmedMessage)
            }
            
            trimmedMessage.contains("структура проекта", ignoreCase = true) ||
            trimmedMessage.contains("покажи структуру", ignoreCase = true) ||
            trimmedMessage.contains("список файлов", ignoreCase = true) -> {
                handleGenerateReadme()
            }
            
            trimmedMessage.contains("что изменилось", ignoreCase = true) ||
            trimmedMessage.contains("изменения", ignoreCase = true) ||
            trimmedMessage.contains("changelog", ignoreCase = true) ||
            trimmedMessage.contains("история", ignoreCase = true) -> {
                handleGenerateChangelog()
            }
            
            trimmedMessage.contains("покажи код", ignoreCase = true) ||
            trimmedMessage.contains("прочитай файл", ignoreCase = true) ||
            trimmedMessage.contains("read file", ignoreCase = true) -> {
                handleFileRead(trimmedMessage)
            }
            
            trimmedMessage.contains("создай файл", ignoreCase = true) ||
            trimmedMessage.contains("запиши в файл", ignoreCase = true) ||
            trimmedMessage.contains("write file", ignoreCase = true) -> {
                handleFileWrite(trimmedMessage)
            }
            
            trimmedMessage.contains("проанализируй", ignoreCase = true) ||
            trimmedMessage.contains("анализ", ignoreCase = true) ||
            trimmedMessage.contains("analyze", ignoreCase = true) -> {
                handleFileAnalyze(trimmedMessage)
            }
            
            trimmedMessage.contains("список файлов", ignoreCase = true) ||
            trimmedMessage.contains("все файлы", ignoreCase = true) ||
            trimmedMessage.contains("list files", ignoreCase = true) -> {
                handleFileList(trimmedMessage)
            }
            
            else -> {
                Log.d(TAG, "❌ Message is not a file command")
                FileCommandResult(handled = false)
            }
        }
    }
    
    private suspend fun handleFileSearch(message: String): FileCommandResult {
        Log.d(TAG, "🔍 Handling file search")
        
        val searchPattern = extractSearchPattern(message) ?: return FileCommandResult(
            handled = true,
            error = "Не удалось определить что искать. Укажите текст для поиска."
        )
        
        val filePattern = extractFilePattern(message) ?: "*.kt"
        
        return try {
            val response = mcpRepository.fileSearch(searchPattern, filePattern)
            
            if (response.success && response.results.isNotEmpty()) {
                val formattedResults = response.results.joinToString("\n\n") { result ->
                    "📄 **${result.file}** (строка ${result.line})\n```\n${result.content.trim()}\n```"
                }
                
                val answer = buildString {
                    append("🔍 **Результаты поиска '${response.pattern}' в файлах ${response.file_pattern}:**\n\n")
                    append("Найдено ${response.count} совпадений:\n\n")
                    append(formattedResults)
                }
                
                FileCommandResult(handled = true, response = answer)
            } else if (response.success) {
                FileCommandResult(handled = true, response = "Поиск '${searchPattern}' не дал результатов.")
            } else {
                FileCommandResult(handled = true, error = "Ошибка при поиске: ${response.pattern}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in file search: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при поиске: ${e.message}")
        }
    }
    
    private suspend fun handleGenerateReadme(): FileCommandResult {
        Log.d(TAG, "📄 Generating README")
        
        return try {
            val response = mcpRepository.generateReadme()
            
            if (response.success) {
                FileCommandResult(handled = true, response = "## Структура проекта\n\n${response.content}")
            } else {
                FileCommandResult(handled = true, error = "Ошибка при генерации: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating README: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при генерации: ${e.message}")
        }
    }
    
    private suspend fun handleGenerateChangelog(): FileCommandResult {
        Log.d(TAG, "📝 Generating CHANGELOG")
        
        return try {
            val response = mcpRepository.generateChangelog()
            
            if (response.success) {
                val answer = buildString {
                    append("## История изменений\n\n")
                    append(response.message)
                    if (response.commits_count != null) {
                        append("\n\nВсего коммитов: ${response.commits_count}")
                    }
                }
                FileCommandResult(handled = true, response = answer)
            } else {
                FileCommandResult(handled = true, error = "Ошибка при генерации: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CHANGELOG: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при генерации: ${e.message}")
        }
    }
    
    private suspend fun handleFileRead(message: String): FileCommandResult {
        Log.d(TAG, "📖 Reading file")
        
        val filePath = extractFilePath(message) ?: return FileCommandResult(
            handled = true,
            error = "Не удалось определить путь к файлу. Укажите путь к файлу."
        )
        
        return try {
            val response = mcpRepository.fileRead(filePath)
            
            if (response.success) {
                val answer = buildString {
                    append("📄 **Файл:** ${response.path}\n")
                    append("📏 **Размер:** ${response.size} байт\n\n")
                    append("```\n${response.content}\n```")
                }
                FileCommandResult(handled = true, response = answer)
            } else {
                FileCommandResult(handled = true, error = "Ошибка при чтении файла: ${filePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при чтении файла: ${e.message}")
        }
    }
    
    private suspend fun handleFileWrite(message: String): FileCommandResult {
        Log.d(TAG, "✏️ Writing file")
        
        val (path, content) = extractFilePathAndContent(message) ?: return FileCommandResult(
            handled = true,
            error = "Не удалось определить путь к файлу. Укажите путь к файлу."
        )
        
        // Если содержимое не указано, просим пользователя его ввести
        if (content.isBlank()) {
            return FileCommandResult(
                handled = true,
                error = "Укажите содержимое для файла '$path'. Например: 'создай файл $path с текстом: Задачи на сегодня'"
            )
        }
        
        return try {
            val response = mcpRepository.fileWrite(path, content)
            
            if (response.success) {
                val answer = buildString {
                    append("✅ Файл успешно ${if (response.was_new) "создан" else "обновлен"}: ${response.message}\n")
                    if (response.diff.isNotEmpty()) {
                        append("\n**Изменения:**\n```\n${response.diff}\n```")
                    }
                }
                FileCommandResult(handled = true, response = answer)
            } else {
                FileCommandResult(handled = true, error = "Ошибка при записи файла: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при записи файла: ${e.message}")
        }
    }
    
    private suspend fun handleFileAnalyze(message: String): FileCommandResult {
        Log.d(TAG, "🔬 Analyzing files")
        
        val query = extractAnalyzeQuery(message) ?: return FileCommandResult(
            handled = true,
            error = "Не удалось определить вопрос для анализа. Укажите что именно нужно проанализировать."
        )
        
        val filePattern = extractFilePattern(message) ?: "*.kt"
        
        return try {
            val response = mcpRepository.fileAnalyze(query, filePattern)
            
            if (response.success) {
                val answer = buildString {
                    append("🔬 **Анализ:** ${response.query}\n")
                    append("📁 **Файлы:** ${response.file_pattern}\n\n")
                    append(response.answer)
                    
                    if (response.sources.isNotEmpty()) {
                        append("\n\n**Источники:**\n")
                        response.sources.forEach { source ->
                            append("- ${source.file} (релевантность: ${(source.relevance * 100).toInt()}%)\n")
                        }
                    }
                }
                FileCommandResult(handled = true, response = answer)
            } else {
                FileCommandResult(handled = true, error = "Ошибка при анализе")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing files: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при анализе: ${e.message}")
        }
    }
    
    private suspend fun handleFileList(message: String): FileCommandResult {
        Log.d(TAG, "📂 Listing files")
        
        val pattern = extractFilePattern(message) ?: "*"
        
        return try {
            val response = mcpRepository.fileList(pattern)
            
            if (response.success && response.files.isNotEmpty()) {
                val answer = buildString {
                    append("📂 **Файлы по маске '${response.pattern}':**\n\n")
                    append("Всего найдено: ${response.count} файлов\n\n")
                    response.files.forEach { file ->
                        append("📄 $file\n")
                    }
                }
                FileCommandResult(handled = true, response = answer)
            } else if (response.success) {
                FileCommandResult(handled = true, response = "Файлы по маске '$pattern' не найдены.")
            } else {
                FileCommandResult(handled = true, error = "Ошибка при получении списка файлов")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing files: ${e.message}")
            FileCommandResult(handled = true, error = "Ошибка при получении списка файлов: ${e.message}")
        }
    }
    
    private fun extractSearchPattern(message: String): String? {
        // Паттерны для естественного языка на русском
        val naturalPatterns = listOf(
            // "найди все места где используется X"
            Regex("где\\s+используется\\s+(.+)", RegexOption.IGNORE_CASE),
            // "найди X"
            Regex("найди\\s+(.+?)(?:\\s+в|$)", RegexOption.IGNORE_CASE),
        )
        
        // Проверяем естественные паттерны
        for (pattern in naturalPatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val result = match.groupValues[1].trim()
                // Удаляем лишние слова
                return result.replace(Regex("^(все|места)\\s+"), "").trim()
            }
        }
        
        // Старые паттерны для совместимости
        val keywords = listOf(
            "найди", "где используется", "поиска", "search for", "find"
        )
        
        for (keyword in keywords) {
            if (message.lowercase().contains(keyword)) {
                val startIndex = message.lowercase().indexOf(keyword) + keyword.length
                val remaining = message.substring(startIndex).trim()
                
                val patterns = listOf(
                    Regex("\"([^\"]+)\""),
                    Regex("'([^']+)'"),
                    Regex("в файле\\s+([\\w./]+)"),
                    Regex("in\\s+([\\w./]+)")
                )
                
                for (pattern in patterns) {
                    val match = pattern.find(remaining)
                    if (match != null) {
                        return match.groupValues[1]
                    }
                }
                
                if (remaining.isNotEmpty()) {
                    val parts = remaining.split(Regex("\\s+"))
                    // Пропускаем слова "все", "места" и т.д.
                    val skipWords = setOf("все", "места", "где", "в", "files", "in")
                    for (part in parts) {
                        if (part.lowercase() !in skipWords) {
                            return part
                        }
                    }
                }
            }
        }
        return null
    }
    
    private fun extractFilePattern(message: String): String? {
        val patterns = listOf(
            Regex("\\*\\.\\w+"),
            Regex("файлах?\\s+([*\\w.]+)"),
            Regex("files?\\s+([*\\w.]+)")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    private fun extractFilePath(message: String): String? {
        val patterns = listOf(
            Regex("\"([^\"]+\\.[\\w]+)\""),
            Regex("'([^']+\\.[\\w]+)'"),
            Regex("путь\\s+[\"']?([\\w./]+\\.[\\w]+)[\"']?"),
            Regex("path\\s+[\"']?([\\w./]+\\.[\\w]+)[\"']?"),
            Regex("[\\w./]+\\.[\\w]{2,4}")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    private fun extractFilePathAndContent(message: String): Pair<String, String>? {
        // Паттерны для естественного языка на русском
        val naturalLanguagePatterns = listOf(
            // "создай файл X с Y"
            Regex("создай\\s+файл\\s+(\\S+)\\s+с\\s+(.+)", RegexOption.IGNORE_CASE),
            // "создай файл X с текстом Y"
            Regex("создай\\s+файл\\s+(\\S+)\\s+с\\s+текстом\\s+(.+)", RegexOption.IGNORE_CASE),
            // "запиши в X: Y"
            Regex("запиши\\s+в\\s+(\\S+)\\s*:\\s*(.+)", RegexOption.IGNORE_CASE),
            // "создай файл X"
            Regex("создай\\s+файл\\s+(\\S+)", RegexOption.IGNORE_CASE),
        )
        
        // Проверяем естественные паттерны
        for (pattern in naturalLanguagePatterns) {
            val match = pattern.find(message)
            if (match != null) {
                val path = match.groupValues[1]
                val content = if (match.groupValues.size > 2) match.groupValues[2].trim() else ""
                return Pair(path, content)
            }
        }
        
        // Паттерны с кавычками
        val pathMatch = Regex("\"([^\"]+)\"").find(message)
        val pathMatch2 = Regex("'([^']+)'").find(message)
        
        val path = pathMatch?.groupValues?.get(1) ?: pathMatch2?.groupValues?.get(1)
        
        if (path != null) {
            val contentPatterns = listOf(
                Regex("с содержимым:\\s*\"([^\"]+)\""),
                Regex("с текстом:\\s*\"([^\"]+)\""),
                Regex("with content:\\s*\"([^\"]+)\""),
                Regex("создай файл\\s+[\"'][^\"']+[\"']\\s+(.+)"),
            )
            
            for (pattern in contentPatterns) {
                val contentMatch = pattern.find(message)
                if (contentMatch != null) {
                    return Pair(path, contentMatch.groupValues[1])
                }
            }
            
            return Pair(path, "")
        }
        
        return null
    }
    
    private fun extractAnalyzeQuery(message: String): String? {
        val patterns = listOf(
            Regex("проанализируй\\s+(.+?)(?:\\s+в файл|$)"),
            Regex("анализ\\s+(.+?)(?:\\s+в файл|$)"),
            Regex("что\\s+(.+?)(?:\\s+в файл|$)"),
            Regex("как\\s+(.+?)(?:\\s+в файл|$)"),
        )
        
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return null
    }
}

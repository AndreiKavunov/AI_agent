# File Operations Integration

## Overview

This document describes the integration of file operation tools into the Android application. The assistant can now interact with the project's file system through the MCP server.

## What Was Added

### 1. Data Models (`FileOperationModels.kt`)

Created new data models for file operation responses:

- `FileSearchResponse` - Results from searching text in files
- `FileReadResponse` - Content of a read file
- `GenerateResponse` - Generated README or CHANGELOG
- `FileWriteResponse` - Result of writing/creating a file
- `FileListResponse` - List of files matching a pattern
- `FileAnalyzeResponse` - RAG analysis results

### 2. Enhanced MCP Repository (`McpRepository.kt`)

Added new methods to `McpRepository` for file operations:

- `fileSearch(pattern, filePattern)` - Search for text in files
- `fileRead(path)` - Read file contents
- `fileWrite(path, content)` - Create or update a file
- `fileList(pattern)` - List files matching a pattern
- `fileAnalyze(query, filePattern)` - Analyze files using RAG
- `generateReadme()` - Generate README from project structure
- `generateChangelog()` - Generate CHANGELOG from git history

### 3. File Command Processor (`FileCommandProcessor.kt`)

Created a command processor that interprets high-level natural language commands and converts them to appropriate file operations:

- Detects file operation commands in user messages
- Extracts parameters (paths, patterns, content) from natural language
- Executes the appropriate MCP tool
- Formats responses for the user

### 4. Integration with Universal Agent (`UniversalAgentImpl.kt`)

- Added `McpRepository` as a dependency
- Added `FileCommandProcessor` to intercept messages
- File commands are processed before sending messages to the LLM
- File commands don't get stored in the chat history

### 5. Dependency Injection (`AppModule.kt`)

- Added `McpRepository` instance
- Injected into `UniversalAgentImpl`

## Available Commands

### Search in Files

**Russian:**
- "Найди все места, где используется GigaChat"
- "Где используется функция login?"
- "Поиск текста 'TODO' в файлах"

**English:**
- "Find all places where GigaChat is used"
- "Where is the login function used?"
- "Search for 'TODO' in files"

### Show Project Structure

**Russian:**
- "Покажи мне структуру проекта"
- "Сгенерируй README"

**English:**
- "Show me the project structure"
- "Generate README"

### View Git History

**Russian:**
- "Что изменилось за последнее время?"
- "Покажи историю изменений"
- "Сгенерируй CHANGELOG"

**English:**
- "What changed recently?"
- "Show the change history"
- "Generate CHANGELOG"

### Read File

**Russian:**
- "Покажи код MainActivity"
- "Прочитай файл app/src/main/AndroidManifest.xml"
- "Read file 'MainActivity.kt'"

**English:**
- "Show me the MainActivity code"
- "Read the file app/src/main/AndroidManifest.xml"

### Create/Write File

**Russian:**
- "Создай файл todo.txt со списком задач"
- "Запиши в файл notes.txt текст: 'Заметка'"

**English:**
- "Create a file todo.txt with a task list"
- "Write to file notes.txt the text: 'Note'"

### Analyze Files

**Russian:**
- "Проанализируй архитектуру приложения"
- "Что такое ViewModel?"
- "Как работает RAG?"

**English:**
- "Analyze the application architecture"
- "What is a ViewModel?"
- "How does RAG work?"

### List Files

**Russian:**
- "Покажи все Kotlin файлы"
- "Список файлов *.xml"
- "List all *.kt files"

**English:**
- "Show all Kotlin files"
- "List files *.xml"

## How It Works

1. **User sends a message** (e.g., "Найди все места, где используется GigaChat")

2. **FileCommandProcessor intercepts the message** in `UniversalAgentImpl.processMessage()`

3. **Command detection**: The processor checks if the message contains file operation keywords

4. **Parameter extraction**: Extracts search pattern, file paths, content, etc. from natural language

5. **API call**: Calls the appropriate method in `McpRepository` which communicates with the MCP server

6. **Response formatting**: Formats the server response in a user-friendly way

7. **Return response**: Returns the formatted response without sending the message to the LLM

## Example Response

For the command "Найди все места, где используется GigaChat":

```
🔍 **Результаты поиска 'GigaChat' в файлах *.kt:**

Найдено 56 совпадений:

📄 **app/src/main/java/com/example/aiagent/data/giga/GigaChatRepository.kt** (строка 42)
```
class GigaChatRepository {
```

📄 **app/src/main/java/com/example/aiagent/domain/agent/UniversalAgentImpl.kt** (строка 115)
```
gigaChatRepository = gigaChatRepository,
```
...
```

## Server Endpoints

The following MCP server endpoints are used:

- `/call_tool` - Generic endpoint to call any tool
  - `file_search` - Search text in files
  - `file_read` - Read file contents
  - `file_write` - Create/update file
  - `file_list` - List files
  - `file_analyze` - Analyze files via RAG
  - `generate_readme` - Generate README
  - `generate_changelog` - Generate CHANGELOG

## Configuration

The MCP server URL is configured in `AppModule.kt`:

```kotlin
private val mcpRepository: McpRepository by lazy {
    McpRepository("http://192.168.0.82:8000")
}
```

Update this URL if your server is running on a different address.

## Error Handling

If a file operation fails, the error is returned in a user-friendly format:

```
❌ Ошибка при поиске: Connection refused
```

File operation errors don't crash the app; they return an error message that the user sees.

## Notes

- File commands are not stored in the chat history (database)
- File commands are processed synchronously before LLM processing
- The assistant can handle multiple file operations in a single message
- Regular chat messages that don't match file command patterns are sent to the LLM as usual

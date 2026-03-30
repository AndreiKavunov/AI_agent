# Структура проекта AI_agent

## 📁 Корневая структура
AI_agent/
├── app/ # Android приложение
│ ├── src/main/java/com/example/aiagent/
│ │ ├── MainActivity.kt # Главная активность
│ │ ├── MyApplication.kt # Класс приложения
│ │ ├── data/ # Слой данных
│ │ │ ├── database/ # База данных Room
│ │ │ │ ├── AppDatabase.kt
│ │ │ │ ├── MessageDao.kt
│ │ │ │ └── MessageEntity.kt
│ │ │ ├── mcp/ # MCP интеграция
│ │ │ └── rag/ # RAG клиент
│ │ ├── ui/ # UI слой (Compose)
│ │ └── domain/ # Бизнес-логика
│ └── build.gradle.kts # Сборка модуля
├── MCP_INTEGRATION.md # Документация MCP
├── MCP_QUICKSTART.md # Быстрый старт MCP
└── MEMORY_IMPLEMENTATION_SUMMARY.md

text

## 🏗️ Архитектура приложения

### Data Layer (слой данных)
- **Room Database**: Локальное хранилище сообщений
- **MessageDao**: Доступ к данным сообщений
- **MessageEntity**: Модель сообщения

### MCP Integration
- **McpTool.kt**: Модели для MCP
- **McpClient.kt**: HTTP клиент для MCP сервера
- **McpExample.kt**: Примеры использования

### RAG Integration
- **RagClient.kt**: Клиент для RAG сервера
- **RagModels.kt**: Модели данных RAG

### UI Layer
- **ChatScreen.kt**: Экран чата
- **McpToolsScreen.kt**: Экран инструментов MCP

## 🔄 Основные компоненты

1. **UniversalAgent** - основной обработчик сообщений
2. **GigaChat** - интеграция с GigaChat API
3. **LanguageMemoryManager** - управление памятью и языковыми фактами
4. **RAGSystem** - поиск по документации
5. **McpTools** - инструменты MCP сервера

## 📋 ПРАВИЛА РАБОТЫ С КОНТЕКСТОМ

- Если в контексте есть конкретная информация о проекте - используй её
- Не придумывай то, чего нет в контексте
- Указывай, из какого документа взята информация, если это уместно

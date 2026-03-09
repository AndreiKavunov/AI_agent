# MCP Server Integration

Этот документ описывает интеграцию с MCP (Model Context Protocol) HTTP сервером в Android приложении.

## Структура файлов

### Data Layer
- **`app/src/main/java/com/example/aiagent/data/mcp/McpTool.kt`** - Модели данных для ответов от MCP сервера
- **`app/src/main/java/com/example/aiagent/data/mcp/McpRepository.kt`** - Репозиторий для HTTP запросов к MCP серверу
- **`app/src/main/java/com/example/aiagent/data/mcp/McpExample.kt`** - Примеры использования

### UI Layer
- **`app/src/main/java/com/example/aiagent/ui/screen/components/McpToolsScreen.kt`** - Composable экран для отображения инструментов

### Configuration
- **`app/src/main/res/xml/network_security_config.xml`** - Конфигурация безопасности сети для HTTP запросов

## API MCP сервера

### Базовый URL
```
http://192.168.0.82:8000
```

### Эндпоинты

#### 1. GET /tools
Получить список доступных инструментов.

**Ответ:**
```json
{
  "success": true,
  "tools": [
    {
      "name": "Hello Local server",
      "description": "Простой инструмент локального сервера",
      "inputSchema": {
        "type": "object",
        "properties": {}
      }
    }
  ],
  "count": 1
}
```

#### 2. POST /call_tool
Вызвать инструмент (пока не реализовано).

## Использование

### Вариант 1: Использование через UI экран

Добавьте `McpToolsScreen` в ваше приложение:

```kotlin
// В вашем Activity или Composable
McpToolsScreen()
```

### Вариант 2: Программное использование

#### В ViewModel или Activity:

```kotlin
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.aiagent.data.mcp.McpRepository
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {
    private val repository = McpRepository()
    
    fun loadTools() {
        viewModelScope.launch {
            try {
                val response = repository.getTools()
                
                if (response.success) {
                    println("Получено ${response.count} инструментов")
                    response.tools.forEach { tool ->
                        println("Инструмент: ${tool.name}")
                        println("Описание: ${tool.description}")
                    }
                }
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }
}
```

#### В Activity с lifecycleScope:

```kotlin
import androidx.lifecycle.lifecycleScope
import com.example.aiagent.data.mcp.McpRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = McpRepository()
        
        lifecycleScope.launch {
            try {
                val response = repository.getTools()
                // Обработка ответа
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }
}
```

#### Использование примера:

```kotlin
import com.example.aiagent.data.mcp.McpExample

// Простой пример с блокирующим вызовом (для тестирования)
McpExample.fetchToolsExample()
```

## Конфигурация

### Разрешения в AndroidManifest.xml

Убедитесь, что в вашем `AndroidManifest.xml` есть разрешение на интернет:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Конфигурация сети

Конфигурация безопасности сети уже добавлена в `AndroidManifest.xml`:

```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config">
```

Это позволяет HTTP запросы к вашему локальному серверу `192.168.0.82`.

## Тестирование

### Требования:
1. Убедитесь, что MCP сервер запущен и доступен по адресу `http://192.168.0.82:8000`
2. Устройство должно быть в той же сети, что и сервер
3. Проверьте, что сервер принимает HTTP запросы (не HTTPS)

### Проверка через Logcat:

При успешном подключении вы увидите в Logcat:

```
D/McpRepository: Fetching tools from http://192.168.0.82:8000/tools
D/McpRepository: Successfully fetched 1 tools
D/McpRepository: Tool: Hello Local server - Простой инструмент локального сервера
```

### Возможные проблемы:

1. **Connection refused**: Проверьте, что сервер запущен
2. **Timeout**: Проверьте сетевое подключение и доступность IP адреса
3. **Cleartext traffic not permitted**: Убедитесь, что `network_security_config.xml` настроен правильно
4. **Permission denied**: Проверьте наличие `INTERNET` разрешения

## Структура данных

### McpTool
```kotlin
data class McpTool(
    val name: String,           // Название инструмента
    val description: String,    // Описание
    val inputSchema: InputSchema // Схема входных данных
)
```

### McpToolsResponse
```kotlin
data class McpToolsResponse(
    val success: Boolean,       // Успешность запроса
    val tools: List<McpTool>,   // Список инструментов
    val count: Int              // Количество инструментов
)
```

## Дальнейшее развитие

Для расширения функциональности можно добавить:

1. **Вызов инструментов**: Реализовать метод `callTool()` для POST запросов
2. **Кэширование**: Добавить кэширование списка инструментов
3. **Обработка ошибок**: Улучшить обработку сетевых ошибок
4. **WebSocket**: Добавить поддержку WebSocket для реального времени
5. **Аутентификация**: Добавить поддержку токенов аутентификации

## Зависимости

Все необходимые зависимости уже добавлены в `app/build.gradle.kts`:

```kotlin
// Ktor HTTP клиент
implementation("io.ktor:ktor-client-core:2.3.9")
implementation("io.ktor:ktor-client-cio:2.3.9")
implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

// Kotlinx Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

// Логирование
implementation("io.ktor:ktor-client-logging:2.3.9")
implementation("org.slf4j:slf4j-android:1.7.36")
```

## Полезные ссылки

- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Android Network Security Configuration](https://developer.android.com/training/articles/security-config)

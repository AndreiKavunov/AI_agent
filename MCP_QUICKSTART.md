# MCP Integration - Быстрый старт

## Что было создано

### 1. Модели данных
- [`McpTool.kt`](app/src/main/java/com/example/aiagent/data/mcp/McpTool.kt) - Структуры данных для MCP ответов

### 2. Репозиторий
- [`McpRepository.kt`](app/src/main/java/com/example/aiagent/data/mcp/McpRepository.kt) - HTTP клиент для работы с MCP сервером

### 3. UI компоненты
- [`McpToolsScreen.kt`](app/src/main/java/com/example/aiagent/ui/screen/components/McpToolsScreen.kt) - Composable экран для отображения инструментов

### 4. Примеры и тесты
- [`McpExample.kt`](app/src/main/java/com/example/aiagent/data/mcp/McpExample.kt) - Примеры использования
- [`McpTestActivity.kt`](app/src/main/java/com/example/aiagent/McpTestActivity.kt) - Activity для тестирования
- [`McpRepositoryTest.kt`](app/src/test/java/com/example/aiagent/McpRepositoryTest.kt) - Unit тест

### 5. Конфигурация
- [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml) - Разрешение HTTP трафика
- Обновлен [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) с сетевой конфигурацией

## Как использовать

### Способ 1: Через UI экран (рекомендуется для проверки)

Добавьте в ваш Composable:

```kotlin
import com.example.aiagent.ui.screen.components.McpToolsScreen

@Composable
fun MyScreen() {
    McpToolsScreen()
}
```

Или создайте отдельный экран в навигации.

### Способ 2: Программно в ViewModel

```kotlin
class MyViewModel : ViewModel() {
    private val repository = McpRepository()
    
    fun loadMcpTools() {
        viewModelScope.launch {
            try {
                val response = repository.getTools()
                println("Получено ${response.count} инструментов")
                response.tools.forEach { tool ->
                    println("${tool.name}: ${tool.description}")
                }
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }
}
```

### Способ 3: Через тестовую Activity

1. Добавьте в [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml):

```xml
<activity
    android:name=".McpTestActivity"
    android:exported="true"
    android:label="MCP Test" />
```

2. Запустите приложение и откройте McpTestActivity
3. Нажмите кнопку "Test Connection"

### Способ 4: Через пример

```kotlin
import com.example.aiagent.data.mcp.McpExample

// Запуск примера (для быстрого тестирования)
McpExample.fetchToolsExample()
```

## Проверка подключения

### Требования:
1. ✅ MCP сервер должен быть запущен по адресу `http://192.168.0.82:8000`
2. ✅ Устройство должно быть в той же сети (192.168.0.x)
3. ✅ Сервер должен принимать HTTP запросы

### Проверка через Logcat:

При успешном подключении вы увидите:

```
D/McpRepository: Fetching tools from http://192.168.0.82:8000/tools
D/McpRepository: Successfully fetched 1 tools
D/McpRepository: Tool: Hello Local server - Простой инструмент локального сервера
```

### Возможные ошибки:

| Ошибка | Причина | Решение |
|--------|---------|---------|
| Connection refused | Сервер не запущен | Запустите MCP сервер |
| Timeout | Нет доступа к сети | Проверьте IP адрес и подключение |
| Cleartext traffic not permitted | Нет конфигурации сети | Проверьте `network_security_config.xml` |
| Permission denied | Нет INTERNET разрешения | Проверьте `AndroidManifest.xml` |

## Структура ответа сервера

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

## Документация

Подробная документация доступна в [`MCP_INTEGRATION.md`](MCP_INTEGRATION.md)

## Следующие шаги

1. **Протестируйте подключение** - Используйте любой из способов выше
2. **Интегрируйте в приложение** - Добавьте вызовы в нужные места
3. **Расширьте функциональность** - Добавьте вызов инструментов через POST /call_tool
4. **Обработка ошибок** - Улучшите обработку сетевых ошибок
5. **Кэширование** - Добавьте кэширование инструментов

## Быстрая проверка (одна строка)

Добавьте в [`MainActivity.onCreate()`](app/src/main/java/com/example/aiagent/MainActivity.kt:14):

```kotlin
// В onCreate() после super.onCreate(savedInstanceState)
lifecycleScope.launch {
    try {
        val repo = McpRepository()
        val response = repo.getTools()
        Log.d("MCP", "✅ Успех! Получено ${response.count} инструментов")
        repo.close()
    } catch (e: Exception) {
        Log.e("MCP", "❌ Ошибка: ${e.message}")
    }
}
```

Импорты:
```kotlin
import androidx.lifecycle.lifecycleScope
import com.example.aiagent.data.mcp.McpRepository
import kotlinx.coroutines.launch
import android.util.Log
```

## Полезные команды

### Сборка проекта:
```bash
./gradlew build
```

### Запуск тестов:
```bash
./gradlew test
```

### Установка на устройство:
```bash
./gradlew installDebug
```

### Просмотр логов:
```bash
adb logcat | grep McpRepository
```

## Поддержка

Если возникли проблемы:
1. Проверьте логи в Logcat
2. Убедитесь, что сервер запущен
3. Проверьте сетевое подключение
4. Посмотрите документацию в [`MCP_INTEGRATION.md`](MCP_INTEGRATION.md)

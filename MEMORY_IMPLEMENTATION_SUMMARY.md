# Реализация трёхуровневой памяти для изучения языка

## Обзор реализации

Данная реализация создаёт систему трёхуровневой памяти для модели изучения языка, как было описано в требованиях.

## Созданные файлы

### 1. Data Classes (domain/agent/memory/)

#### [`RecentMistake.kt`](app/src/main/java/com/example/aiagent/domain/agent/memory/RecentMistake.kt)
- Представляет ошибку, сделанную пользователем при изучении языка
- Поля: error, correction, rule, explanation, timestamp, occurrences
- Методы: create(), incrementOccurrences(), formatForPrompt()

#### [`UserProfile.kt`](app/src/main/java/com/example/aiagent/domain/agent/memory/UserProfile.kt)
- Полный профиль ученика для изучения языка
- Поля: name, nativeLanguage, targetLanguage, overallLevel, learningGoals, preferredLessonDuration, weeklyGoal, interests, age, profession, learningStyle, timezone
- Методы: update(), formatForPrompt(), isValid()

#### [`VocabularyItem.kt`](app/src/main/java/com/example/aiagent/domain/agent/memory/VocabularyItem.kt)
- Представляет слово или фразу для изучения
- Поля: word, translation, partOfSpeech, pronunciation, example, level, topic, masteryLevel, reviewCount, lastReviewed, nextReview, isFavorite, tags
- Методы: create(), updateMastery(), formatForPrompt(), isDueForReview()
- Включает простую реализацию Spaced Repetition

#### [`LessonGoal.kt`](app/src/main/java/com/example/aiagent/domain/agent/memory/LessonGoal.kt)
- Представляет цель урока
- Поля: title, description, skillType (grammar, vocabulary, listening, etc.), targetLevel, isCompleted, progress, exercisesCompleted, exercisesTotal
- Методы: create(), updateProgress(), formatForPrompt()

### 2. Extractor & Manager (domain/agent/memory/)

#### [`LanguageFactExtractor.kt`](app/src/main/java/com/example/aiagent/domain/agent/memory/LanguageFactExtractor.kt)
- Извлекает языковые факты из сообщений пользователя
- Использует LLM для извлечения:
  - Vocabulary words (слова с переводом, частью речи, примерами)
  - Grammar rules (грамматические правила с описанием и примерами)
  - Mistakes (ошибки с исправлением и объяснением)
  - Lesson topics (темы уроков с уровнем сложности)
  - Learning goals (цели обучения)
  - Exercise context (контекст упражнения)
- Определяет тип памяти для каждого факта (SHORT_TERM, WORKING, LONG_TERM)

#### [`LanguageMemoryManager.kt`](app/src/main/java/com/example/aiagent/domain/agent/memory/LanguageMemoryManager.kt)
- Оркестрирует трёхуровневую память
- Методы:
  - `processMessage()` - обрабатывает сообщение и сохраняет в соответствующий уровень памяти
  - `updateShortTermMemory()` - обновляет краткосрочную память (последние сообщения)
  - `updateWorkingMemory()` - обновляет рабочую память (текущий урок, ошибки, цели)
  - `updateLongTermMemory()` - обновляет долговременную память (профиль, словарь, правила)
  - `buildPrompt()` - строит промпт с контекстом из всех уровней памяти
  - `getUserProfile()` / `saveUserProfile()` - работа с профилем
  - `getLearningStats()` - получение статистики обучения
  - `clearShortTermMemory()` / `clearWorkingMemory()` - очистка памяти

## Изменённые файлы

### 1. Database Layer

#### [`MemoryDao.kt`](app/src/main/java/com/example/aiagent/data/database/memory/MemoryDao.kt)
**Добавленные методы:**
- `updateConversationContext()` - обновление контекста разговора в краткосрочной памяти
- `updateCurrentLevel()` - обновление уровня в рабочей памяти
- `updateLanguageToLearn()` - обновление изучаемого языка
- `updateLearningGoal()` - обновление цели обучения
- `updateLastPracticeDate()` - обновление даты последней практики
- `updateStreakDays()` - обновление количества дней подряд

#### [`MemoryRepository.kt`](app/src/main/java/com/example/aiagent/data/database/memory/MemoryRepository.kt)
**Добавленные методы:**
- `updateConversationContext()` - обёртка над DAO методом
- `updateCurrentLevel()` - обёртка над DAO методом
- `updateLanguageToLearn()` - обёртка над DAO методом
- `updateLearningGoal()` - обёртка над DAO методом
- `updateLastPracticeDate()` - обёртка над DAO методом
- `updateStreakDays()` - обёртка над DAO методом

### 2. Domain Layer

#### [`UniversalAgent.kt`](app/src/main/java/com/example/aiagent/domain/agent/UniversalAgent.kt)
**Добавленные методы:**
- `getUserProfile(): UserProfile?` - получение профиля пользователя
- `saveUserProfile(profile: UserProfile)` - сохранение профиля
- `getLearningStats(): LearningStats?` - получение статистики обучения
- `buildMemoryPrompt(basePrompt: String): String` - построение промпта с контекстом памяти
- `clearShortTermMemory()` - очистка краткосрочной памяти
- `clearWorkingMemory()` - очистка рабочей памяти

#### [`UniversalAgentImpl.kt`](app/src/main/java/com/example/aiagent/domain/agent/UniversalAgentImpl.kt)
**Изменения:**
- Добавлен `LanguageMemoryManager` в конструктор
- Интеграция `processMessage()` в метод `processMessage()` для обработки сообщений пользователя
- Интеграция `processMessage()` для обработки ответов ассистента
- Реализация всех новых методов интерфейса

### 3. Dependency Injection

#### [`AppModule.kt`](app/src/main/java/com/example/aiagent/di/AppModule.kt)
**Добавленные зависимости:**
- `LanguageFactExtractor` - экстрактор языковых фактов
- `LanguageMemoryManager` - менеджер языковой памяти
- Обновлён `UniversalAgent` с передачей `languageMemoryManager`

## Архитектура трёхуровневой памяти

### УРОВЕНЬ 1: SHORT_TERM_MEMORY (Краткосрочная память)
**Хранит:**
- Последние 10-20 сообщений текущего диалога
- Текущий контекст разговора ("о чём только что говорили")
- Недавние исправления ошибок

**Срок жизни:** Пока идёт диалог (очищается при смене темы)

**Используемые сущности:**
- [`ShortTermMemoryEntity`](app/src/main/java/com/example/aiagent/data/database/memory/ShortTermMemoryEntity.kt)
- `lastUserMessage`
- `lastAgentResponse`
- `currentTopic`
- `conversationContext`

### УРОВЕНЬ 2: WORKING_MEMORY (Рабочая память)
**Хранит:**
1. **Текущая тема урока:**
   - `currentTopic` (например: "restaurant")
   - `currentLevel` (например: "A2")

2. **Цели урока:**
   - `learningGoal` (например: "practice_past_tense")
   - `targetVocabulary` (TODO: нужно добавить поле)

3. **Временные ошибки:**
   - `recentMistakes` (TODO: нужно добавить поле)

4. **Контекст задания:**
   - `currentLessonId` (например: "ex_123")
   - `attemptsLeft` (TODO: нужно добавить поле)

**Срок жизни:** В течение одного урока/сессии

**Используемые сущности:**
- [`WorkingMemoryEntity`](app/src/main/java/com/example/aiagent/data/database/memory/WorkingMemoryEntity.kt)
- `languageToLearn`
- `learningGoal`
- `currentLevel`
- `lessonsCompleted`
- `exercisesCompleted`
- `correctAnswers`
- `totalAnswers`
- `achievements`
- `streakDays`
- `lastPracticeDate`
- `currentTopic`
- `currentLessonId`

### УРОВЕНЬ 3: LONG_TERM_MEMORY (Долговременная память)
**Хранит:**
1. **Профиль ученика:**
   - `name`
   - `nativeLanguage`
   - `targetLanguage`
   - `overallLevel`
   - `learningGoals`
   - `preferredLessonDuration`
   - `weeklyGoal`
   - `interests`
   - `age`
   - `profession`
   - `learningStyle`

2. **Словарный запас:**
   - `VocabularyItem` с уровнем освоения
   - Spaced Repetition для повторения

3. **Грамматические правила:**
   - Правила с описанием и примерами

**Срок жизни:** Сохраняется между уроками

**Используемые сущности:**
- [`LongTermMemoryEntity`](app/src/main/java/com/example/aiagent/data/database/memory/LongTermMemoryEntity.kt)
- [`UserProfile`](app/src/main/java/com/example/aiagent/domain/agent/memory/UserProfile.kt)
- [`VocabularyItem`](app/src/main/java/com/example/aiagent/domain/agent/memory/VocabularyItem.kt)
- `MemoryCategory`: PROFILE, PREFERENCES, KNOWLEDGE, DECISIONS, LEARNING_GOALS, ACHIEVEMENTS, CUSTOM

## Поток обработки сообщений

1. **Пользователь отправляет сообщение**
   - Сообщение сохраняется в [`MessageLocalRepository`](app/src/main/java/com/example/aiagent/data/database/MessageLocalRepository.kt)
   - Запускается `LanguageMemoryManager.processMessage()` в фоне

2. **LanguageFactExtractor извлекает факты**
   - Отправляет промпт в LLM
   - Получает JSON с языковыми фактами
   - Определяет тип памяти для каждого факта

3. **LanguageMemoryManager сохраняет факты**
   - **SHORT_TERM_MEMORY**: обновляет lastUserMessage, conversationContext
   - **WORKING_MEMORY**: обновляет текущую тему, цели, сохраняет ошибки
   - **LONG_TERM_MEMORY**: сохраняет словарь, грамматические правила, профиль

4. **Ассистент генерирует ответ**
   - Использует `buildPrompt()` для получения контекста из всех уровней памяти
   - Контекст включает:
     - Последние сообщения (SHORT_TERM)
     - Текущий урок и прогресс (WORKING)
     - Профиль пользователя (LONG_TERM)

5. **Ответ сохраняется**
   - Запускается `LanguageMemoryManager.processMessage()` для ответа ассистента

## TODO: Дополнительные улучшения

### Необходимые изменения в сущностях базы данных:

#### ShortTermMemoryEntity
```kotlin
val recentMessages: String = ""  // JSON список последних 10-20 сообщений
val recentCorrections: String = ""  // JSON список последних исправлений
```

#### WorkingMemoryEntity
```kotlin
val currentSubtopic: String? = null  // Подтема урока
val difficultyLevel: String? = null  // Уровень сложности (A1-C2)
val lessonGoals: String = ""  // JSON список целей урока
val targetVocabulary: String = ""  // JSON список целевого словаря
val recentMistakes: String = ""  // JSON список последних ошибок
val currentExerciseType: String? = null  // Тип упражнения
val attemptsLeft: Int = 3  // Оставшиеся попытки
```

#### LongTermMemoryEntity
```kotlin
// Добавить новые категории в MemoryCategory:
VOCABULARY,           // Словарный запас
GRAMMAR_RULES,        // Грамматические правила
LEARNING_PROGRESS,     // Прогресс обучения
```

### Дополнительные DAO методы:
- Сохранение/получение `recentMessages` в ShortTermMemoryEntity
- Сохранение/получение `recentMistakes` в WorkingMemoryEntity
- Получение словаря по уровню или теме
- Получение слов для повторения (due for review)

### Дополнительные методы в MemoryRepository:
- `addRecentMessage(sessionId, message)` - добавление сообщения в recentMessages
- `addRecentMistake(sessionId, mistake)` - добавление ошибки в recentMistakes
- `getVocabularyForReview()` - получение слов для повторения
- `updateVocabularyMastery(wordId, mastery)` - обновление уровня освоения слова

## Использование

### Пример использования в коде:

```kotlin
// Получение профиля пользователя
val profile = universalAgent.getUserProfile()
if (profile == null) {
    // Создаём новый профиль
    val newProfile = UserProfile(
        name = "Анна",
        nativeLanguage = "ru",
        targetLanguage = "en",
        overallLevel = "A2",
        learningGoals = listOf("work_abroad", "watch_movies")
    )
    universalAgent.saveUserProfile(newProfile)
}

// Получение статистики обучения
val stats = universalAgent.getLearningStats()
if (stats != null) {
    println("Пройдено уроков: ${stats.lessonsCompleted}")
    println("Точность: ${(stats.accuracy * 100).toInt()}%")
}

// Построение промпта с контекстом памяти
val prompt = universalAgent.buildMemoryPrompt("Ты учитель английского языка")
// Теперь prompt включает контекст из всех уровней памяти

// Очистка памяти при смене темы
universalAgent.clearShortTermMemory()  // Очищает краткосрочную память
universalAgent.clearWorkingMemory()    // Очищает рабочую память
```

## Преимущества реализации

1. **Модульность** - Каждый компонент имеет чёткую ответственность
2. **Расширяемость** - Легко добавлять новые типы фактов и уровни памяти
3. **Производительность** - Фоновая обработка не блокирует основной поток
4. **Persistence** - Все данные сохраняются в Room базе данных
5. **Интеграция** - Полностью интегрирована с существующей системой

## Заключение

Реализация создаёт полноценную трёхуровневую систему памяти для изучения языка, соответствующую требованиям. Все основные компоненты созданы и интегрированы. Для полной функциональности необходимо добавить недостающие поля в сущности базы данных, как указано в разделе TODO.

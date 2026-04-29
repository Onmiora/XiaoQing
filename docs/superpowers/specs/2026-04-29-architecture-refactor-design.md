# Qing 架构重构设计文档

**日期:** 2026-04-29
**范围:** 架构优先 + 大文件拆分
**策略:** 自底向上 — Room → Hilt → ViewModel 拆分 → UI 拆分

---

## 1. 背景与目标

Qing 是一款约 19,000 行 Kotlin 代码的心理健康 Android 应用。当前架构存在以下核心问题：

- **无 DI 框架** — 8 个 ViewModel 在 MainActivity 中手动实例化，Navigation 函数有 15 个参数
- **DataStore 滥用** — 结构化数据（聊天记录、心情日记、成就）以 JSON 字符串存在 Preferences DataStore 中，每次操作全量序列化/反序列化
- **ViewModel 职责不清** — StateViewModel (598 行) 承担 4+ 种职责，ViewModel 间有直接依赖
- **大文件** — DiscoverScreen.kt (1403 行) 包含 15+ 个 Composable，大量重复代码

### 目标

1. 引入 Hilt 管理依赖注入
2. 用 Room 替代 DataStore 存储结构化集合
3. 拆分 StateViewModel 为职责单一的小 ViewModel
4. 拆分 DiscoverScreen 等大文件，提取共享组件
5. 清理死代码和已知 bug

### 非目标

- 不做 UI 视觉改版（硬编码颜色/字符串替换留到下一批）
- 不添加新功能
- 不改后端 API
- 不引入图片加载库或崩溃上报

---

## 2. 数据层：Room + DataStore

### 2.1 Room 实体

```kotlin
@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val analysisCount: Int = 0
)

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ChatSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,        // "user" / "assistant"
    val content: String,
    val timestamp: Long,
    val hasRecommendation: Boolean = false,
    val recommendationType: String? = null
)

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey val id: String,
    val moodType: String,
    val reason: String,
    val timestamp: Long
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val category: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
```

### 2.2 DAO 接口

```kotlin
@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: String)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)
}

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<MoodEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoodEntryEntity)

    @Update
    suspend fun update(entry: MoodEntryEntity)

    @Delete
    suspend fun delete(entry: MoodEntryEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY category, name")
    fun getAll(): Flow<List<AchievementEntity>>

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)
}
```

### 2.3 AppDatabase

```kotlin
@Database(
    entities = [
        ChatSessionEntity::class,
        MessageEntity::class,
        MoodEntryEntity::class,
        AchievementEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun moodDao(): MoodDao
    abstract fun achievementDao(): AchievementDao
}
```

### 2.4 Repository 层

```kotlin
@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()
        .map { list -> list.map { it.toDomain() } }

    fun getMessages(sessionId: String): Flow<List<Message>> = chatDao.getMessages(sessionId)
        .map { list -> list.map { it.toDomain() } }

    suspend fun createSession(session: ChatSession) = chatDao.insertSession(session.toEntity())
    suspend fun addMessage(message: Message) = chatDao.insertMessage(message.toEntity())
    suspend fun updateMessageContent(messageId: String, newContent: String) {
        val message = chatDao.getMessageById(messageId) ?: return
        chatDao.updateMessage(message.copy(content = newContent))
    }
    suspend fun deleteMessage(message: Message) = chatDao.deleteMessage(message.toEntity())
    suspend fun deleteSession(session: ChatSession) = chatDao.deleteSession(session.toEntity())
}

// MoodRepository、AchievementRepository 结构类似，各自注入对应的 DAO
// MoodRepository: getAllEntries(), insert(), update(), delete()
// AchievementRepository: getAll(), unlock(id), insertAll() (初始化默认成就)
```

### 2.5 DataStore 瘦身

QingDataStore 重命名为 `PreferencesManager`，只保留：

- `apiUrl: Flow<String>` — API 地址
- `userName: Flow<String>` / `userProfile: Flow<UserProfile>` — 用户信息
- `psychologyDimensions: Flow<PsychologyDimensions>` — 6 个浮点值
- `usageStats: Flow<UsageStats>` — 计数器
- `dailyActivity: Flow<DailyActivity>` — 每日活动追踪

删除所有 `getCurrentSessions()`、`getCurrentMessages()`、`getCurrentAchievements()`、`getCurrentMoodEntries()` 方法及其 JSON 序列化逻辑。

### 2.6 数据模型变更

- `PsychologyDimension` 中的 `Color` 字段改为 `Long`（hex 值），UI 层通过扩展函数转为 `Color`
- `Message.content` 从 `var` 改为 `val`，流式更新通过 `copy()` 生成新实例
- 删除 `MoodEntry.kt` 中重复的 `MoodEntryEntity` 定义和 `toEntity()`/`toMoodEntry()` 扩展

### 2.7 数据迁移

首次启动时检查 Room 数据库是否为空，若为空且 DataStore 中有旧数据，则执行一次性迁移：
1. 从 DataStore 读取 JSON 字符串
2. 解析为实体列表
3. 批量插入 Room
4. 清除 DataStore 中的旧数据

---

## 3. Hilt 集成

### 3.1 依赖配置

在 `build.gradle.kts` 中添加：
- `com.google.dagger:hilt-android`
- `com.google.dagger:hilt-android-compiler` (KSP)
- `androidx.hilt:hilt-navigation-compose`
- `androidx.hilt:hilt-work` (如需后台任务)

### 3.2 Application 类

```kotlin
@HiltAndroidApp
class QingApplication : Application()
```

### 3.3 DI 模块

```
di/
├── AppModule.kt          // @Singleton: PreferencesManager, DataStore 实例
├── DatabaseModule.kt     // @Singleton: AppDatabase, ChatDao, MoodDao, AchievementDao
├── RepositoryModule.kt   // @Singleton: ChatRepository, MoodRepository, AchievementRepository
└── NetworkModule.kt      // @Singleton: OkHttpClient, ApiServiceFactory
```

### 3.4 ApiServiceFactory

```kotlin
@Singleton
class ApiServiceFactory @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val okHttpClient: OkHttpClient
) {
    private var currentBaseUrl: String? = null
    private var cachedRetrofit: Retrofit? = null

    fun <T> create(serviceClass: Class<T>): T {
        val baseUrl = runBlocking { preferencesManager.apiUrl.first() }
        if (baseUrl != currentBaseUrl || cachedRetrofit == null) {
            currentBaseUrl = baseUrl
            cachedRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return cachedRetrofit!!.create(serviceClass)
    }
}
```

### 3.5 ViewModel 注入

所有 ViewModel 从手动 Factory 迁移到 `@HiltViewModel`：

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val apiServiceFactory: ApiServiceFactory,
    private val preferencesManager: PreferencesManager,
    private val usageStatsManager: UsageStatsManager
) : ViewModel()
```

### 3.6 Navigation 简化

ViewModel 不再从顶层传入，各 composable 内部通过 `hiltViewModel()` 获取：

```kotlin
@Composable
fun QingNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    NavHost(navController, ...) {
        composable(Screen.Home.route) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            HomeScreen(homeViewModel, ...)
        }
        // ...
    }
}
```

### 3.7 迁移顺序

1. 添加 Hilt 依赖和插件
2. Application 加 `@HiltAndroidApp`
3. 创建 di/ 模块
4. 逐个 ViewModel 迁移（顺序：SettingsViewModel → MoodViewModel → HomeViewModel → MbtiViewModel → SbtiViewModel → StressDetectionViewModel → ChatViewModel → StateViewModel 拆分后迁移）
5. 清理 MainActivity 中的手动 Factory

---

## 4. ViewModel 拆分

### 4.1 StateViewModel (598 行) 拆分为：

**PsychologyViewModel** (~120 行)
- 职责：6 个心理维度的状态管理
- StateFlow: `psychologyDimensions`, `overallScore` (derivedStateOf)
- 方法：`updateDimensionByKey()`, `resetDimensions()`
- 依赖：PreferencesManager

**AchievementViewModel** (~150 行)
- 职责：成就系统的状态和解锁逻辑
- StateFlow: `achievements`
- 方法：`checkAndUnlockAchievements()`, `unlockAchievement()`
- 依赖：AchievementRepository, PreferencesManager (使用统计)

**AnalysisViewModel** (~180 行)
- 职责：AI 分析 API 调用
- StateFlow: `analysisState` (Idle/Loading/Result/Error)
- 方法：`analyzeSession()`
- 依赖：ApiServiceFactory, PreferencesManager

**UsageStatsManager** (~60 行)
- 职责：使用统计计数器（不是 ViewModel，是 @Singleton）
- Flow: `chatCount`, `analysisCount`, `moodCount`
- 方法：`incrementChatCount()`, `incrementAnalysisCount()`, `incrementMoodCount()`
- 依赖：PreferencesManager

### 4.2 ChatViewModel (523 行) 内部重构

不拆分为多个 ViewModel，但做以下清理：

1. **提取 `buildMessageContext()`** — 消除 `nonStreamingChat` 和 `streamChat` 中的重复代码
2. **修复竞态条件** — `streamingMessageId` 用 `Mutex` 或限制在单一协程上下文
3. **移除 mock fallback** — API 失败时返回明确的错误状态，不再用 `AIResponses.getResponse()` 假装成功
4. **模型名称可配置** — 从 Settings 读取，不再硬编码 `"glm-4.5-air"`
5. **修复嵌套 launch** — catch 块中的 `viewModelScope.launch` 改用 `SupervisorJob` 或确保外层不取消

### 4.3 ViewModel 间通信

消除所有 ViewModel-to-ViewModel 依赖：

| 旧模式 | 新模式 |
|---|---|
| ChatViewModel → StateViewModel.incrementChatCount() | ChatViewModel → UsageStatsManager.incrementChatCount() |
| HomeViewModel → StateViewModel.psychologyDimensions | HomeViewModel → PreferencesManager.psychologyDimensions Flow |
| DiscoverScreen → StateViewModel.achievements | DiscoverScreen → AchievementViewModel (hiltViewModel) |
| Navigation.kt 传 8 个 VM | 各 composable 内部 hiltViewModel() |

---

## 5. UI 拆分

### 5.1 DiscoverScreen.kt (1403 行) 拆分为：

```
ui/screens/discover/
├── DiscoverScreen.kt              // 主编排（~150 行）
├── MoodDashboardCard.kt           // 心情仪表盘（~400 行）
│   ├── MoodDistributionRing
│   ├── MoodMiniTrendChart
│   ├── MoodLegendItem
│   └── MoodCountSection / MoodTrendSection
├── FeatureCards.kt                // 功能卡片（~250 行）
│   ├── BreathingExerciseCard
│   ├── FunTestCard
│   ├── StressDetectionCard
│   ├── AchievementStatCard
│   └── HistoryStatCard
└── InfoCards.kt                   // 信息卡片（~150 行）
    ├── HotlineCard
    └── TipCard
```

### 5.2 提取共享组件

```
ui/components/
├── AnimatedCard.kt         // 从 DiscoverScreen/HomeScreen 提取
├── MoodHelpers.kt          // getMoodColor/getMoodIcon/getMoodText → MoodType 扩展函数
├── DimensionHelpers.kt     // getDimensionColor/getDimensionIcon → String 扩展函数
└── BottomSheetDefaults.kt  // 共享拖拽手柄样式
```

### 5.3 AnimatedCard 提取

```kotlin
@Composable
fun AnimatedCard(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

### 5.4 Navigation.kt 简化

Hilt 后 `QingNavHost` 参数从 15 个缩减为约 5 个。ViewModel 通过 `hiltViewModel()` 在各 composable 内部获取。

---

## 6. 死代码清理与 Bug 修复

### 死代码删除

| 代码 | 位置 | 原因 |
|---|---|---|
| `Screen.SbtiResult` | Screen.kt | 无对应导航注册 |
| `StateViewModel.updateDimension()` | StateViewModel.kt:428 | 从未被调用 |
| `AIResponses.greeting` 列表 | Message.kt | 未使用 |
| `AIResponses.getResponse()` 中的 `lowerMessage` | Message.kt:53 | 计算后未使用 |
| `parseMbtiQuestions()` | MbtiQuestion.kt:26 | 从未被调用 |
| `MoodEntryEntity` 公开定义 | MoodEntry.kt:21-26 | 与 QingDataStore 中的私有定义重复 |
| `MockAnalysisGenerator` | AnalysisResult.kt:56-109 | 生产代码中的 mock，移入 demo 包 |
| DeveloperScreen 空 onClick 测试项 | DeveloperScreen.kt:513-546 | 死 UI |

### Bug 修复

| Bug | 位置 | 修复 |
|---|---|---|
| `iconName = " tranquility"` 前导空格 | Achievement.kt:107 | 删除空格 |
| `DailyGoalsCard` 进度条 `3f` 应为 `goals.size.toFloat()` | HomeScreen.kt:529 | 修正除数 |
| `SimpleDateFormat` 非线程安全 | QingDataStore.kt:36 | 改用 `DateTimeFormatter` |
| MBTI 题目 50 映射到两个维度 | MbtiAlgorithm.kt:60 | 确认正确映射 |
| `AnimatedVisibility(visible = true)` 无意义 | ChatScreen.kt:152,177 | 移除包装或绑定状态 |
| `if (true)` 死条件 | PermissionBottomSheet.kt:51 | 移除无用条件 |

---

## 7. 实施顺序

### Phase 1: Room 数据层
1. 添加 Room 依赖
2. 定义实体、DAO、Database
3. 创建 Repository 层
4. 实现数据迁移（DataStore → Room）
5. 瘦身 PreferencesManager

### Phase 2: Hilt 集成
1. 添加 Hilt 依赖和插件
2. Application 加 `@HiltAndroidApp`
3. 创建 di/ 模块
4. 迁移最简单的 ViewModel（SettingsViewModel）
5. 验证模式后批量迁移其余 ViewModel
6. 清理 MainActivity

### Phase 3: ViewModel 拆分
1. 创建 UsageStatsManager
2. 拆分 StateViewModel → PsychologyViewModel + AchievementViewModel + AnalysisViewModel
3. 重构 ChatViewModel（提取公共方法、修复竞态）
4. 更新所有 ViewModel 的依赖关系
5. 简化 Navigation.kt

### Phase 4: UI 拆分
1. 提取 AnimatedCard 到 ui/components/
2. 提取 MoodHelpers.kt / DimensionHelpers.kt
3. 拆分 DiscoverScreen.kt
4. 清理死代码和修复 bug

---

## 8. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| Room 数据迁移丢失用户数据 | 严重 | 迁移前备份 DataStore 文件，迁移后校验记录数 |
| Hilt 注入导致循环依赖 | 中等 | Repository 层无状态，ViewModel 间通过 Flow 通信而非直接引用 |
| 拆分后功能回归 | 中等 | 每个 Phase 完成后运行完整功能测试 |
| Room schema 升级复杂度 | 低 | 初期版本为 1，后续用 Migration 管理 |

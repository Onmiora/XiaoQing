# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 项目概述

**小晴 (Qing)** 是一款面向大学生的心理健康助手 Android 应用，使用 Jetpack Compose 构建。单 Activity 架构 (`MainActivity`)，包名 `com.onmi.qing`。

这是一个前后端分离项目，后端服务位于 `/Users/onmiora/Documents/QingAPI`。

## 构建命令

```bash
./gradlew assembleDebug          # 构建 debug APK
./gradlew assembleRelease        # 构建 release APK
./gradlew installDebug           # 安装到已连接的设备/模拟器
./gradlew test                   # 运行单元测试
./gradlew connectedAndroidTest   # 运行仪器化测试
```

需要 JDK 21（通过 `gradle-daemon-jvm.properties` 配置）。

## 架构

**MVVM + Hilt DI + Repository 模式。** 使用 Hilt 进行依赖注入，ViewModel 通过 `@HiltViewModel` 注解由 Hilt 自动注入。`QingApplication` 使用 `@HiltAndroidApp`，`MainActivity` 使用 `@AndroidEntryPoint`。

### 分层结构 (`com.onmi.qing/`)

| 层级 | 位置 | 说明 |
|---|---|---|
| UI | `ui/screens/`、`ui/components/` | Composable 页面、可复用组件 |
| 导航 | `ui/navigation/` | `Screen.kt`（路由密封类）、`Navigation.kt`（NavHost） |
| 主题 | `ui/theme/` | Material 3，动态取色（Android 12+），支持亮/暗主题 |
| ViewModel | `viewmodel/` | 11 个 ViewModel，全部使用 `@HiltViewModel`，`ChatViewModel` 额外使用 `@AssistedInject` |
| 数据模型 | `data/` | 数据类、问卷题目、算法逻辑（MBTI、SBTI、压力检测） |
| 数据库 | `data/local/` | Room 数据库（`AppDatabase` v2），4 个 Entity，3 个 DAO，`DataMigration` 从旧版 DataStore 迁移 |
| Repository | `data/repository/` | `ChatRepository`、`MoodRepository`、`AchievementRepository` |
| 持久化 | `data/datastore/` | DataStore Preferences — 用户偏好、心理维度、使用统计 |
| 远程 API | `data/remote/` | Retrofit + OkHttp，兼容 Anthropic 格式的 `/v1/messages` 端点，SSE 流式传输 |
| DI 模块 | `di/` | Hilt 模块：`AppModule`、`DatabaseModule`、`NetworkModule`、`RepositoryModule` |
| BLE | `ble/` | `HeartRateManager` — 心率监测设备的 GATT 客户端 |
| 演示模式 | `data/demo/` | 通过 `DemoModeManager` 切换的内存演示数据 |

### 关键架构要点

- **依赖注入**：Hilt 2.59.2，4 个模块分别提供 App 单例、数据库、网络服务和 Repository。
- **导航**：路由在 `Screen.kt` 中定义为密封类，4 个底部导航栏 Tab（Home、Discover、MoodDiary、Profile）+ 10 个子页面。`Navigation.kt` 中注册所有 Composable 目的地，使用 `hiltViewModel<>()` 获取 ViewModel。
- **数据持久化**：Room 数据库存储聊天记录（`ChatSessionEntity` + `MessageEntity`）、心情日记（`MoodEntryEntity`）、成就（`AchievementEntity`）。DataStore Preferences 存储用户偏好、心理维度和使用统计。`DataMigration` 负责从旧版 DataStore JSON 迁移到 Room。
- **Repository 模式**：`ChatRepository`（会话/消息 CRUD）、`MoodRepository`（心情 CRUD）、`AchievementRepository`（成就 CRUD + 默认初始化）。
- **AI 聊天**：`ChatViewModel` 连接兼容 Anthropic 格式的 API，支持 SSE 流式传输（通过 `SseEventParser`）和非流式回退。支持多会话管理（`chat/{sessionId}`），消息编辑、重新生成、删除。API 地址和模型可在设置中由用户配置。
- **心理维度**：`StateViewModel` 中跟踪 6 个浮点数（0–1）— 情绪稳定性、自我认知、压力管理、社交信心、睡眠质量、自我关怀。通过 `/v1/analyze` 端点由 AI 分析更新。
- **自适应布局**：使用 `WindowWidthSizeClass` — 平板/折叠屏使用 NavigationRail，手机使用自定义 `FloatingPillNavBar`。
- **无图片加载库、无崩溃上报。**

### 新增功能模块

| 功能 | ViewModel | 主要文件 |
|---|---|---|
| SBTI 性格测试 | `SbtiViewModel` | `data/SbtiQuestion.kt`、`data/SbtiAlgorithm.kt`、`ui/screens/sbti/` |
| MBTI 性格测试 | `MbtiViewModel` | `data/MbtiQuestion.kt`、`data/MbtiAlgorithm.kt`、`ui/screens/mbti/` |
| 压力检测 | `StressDetectionViewModel` | `ble/HeartRateManager.kt`、`data/StressQuestion.kt`、`data/StressResult.kt`、`ui/screens/stressdetection/`（6 个页面） |
| 使用统计 | `UsageStatsViewModel` | `data/UsageStatsManager.kt`（追踪聊天/呼吸/签到次数，每日重置） |
| 聊天历史 | — | `ui/screens/history/HistoryScreen.kt` |
| 开发者选项 | — | `ui/screens/developer/DeveloperScreen.kt` |

## 后端服务 (QingAPI)

后端位于 `/Users/onmiora/Documents/QingAPI`，使用 Python + FastAPI 构建，提供 AI 聊天和心理分析 API。

### 技术栈
- **语言**：Python 3.14.3
- **框架**：FastAPI (async) + Uvicorn
- **LLM**：Anthropic SDK 连接智谱 AI 的 Anthropic 兼容端点 (`open.bigmodel.cn/api/anthropic`)，模型 `glm-4.5-air`
- **向量数据库**：ChromaDB（本地持久化，10 篇心理学论文已索引）
- **嵌入模型**：通过 LM Studio 运行`text-embedding-qwen3-embedding-0.6b`，或是指定云端模型服务。

### 启动命令
```bash
cd /Users/onmiora/Documents/QingAPI/QingBackend
python main.py  # 启动在 0.0.0.0:8000
```

### API 端点
| 端点 | 方法 | 说明 |
|---|---|---|
| `/` | GET | 健康检查，返回服务名 + 版本 |
| `/health` | GET | 简单健康检查 |
| `/v1/messages` | POST | AI 聊天（支持 SSE 流式传输），自动注入 RAG 知识上下文 |
| `/v1/models` | GET | 列出可用模型 |
| `/v1/analyze` | POST | 心理维度分析（返回 6 个维度的变化量，temperature=0.5） |

### 后端特性
- **RAG 管道**：用户查询 → LM Studio 生成嵌入 → ChromaDB 向量搜索 → 相关心理学论文片段注入系统提示词 → LLM 生成带引用的回复。
- **工具调用**：LLM 可调用 `recommend_feature`（推荐功能）和 `crisis_intervention`（危机干预，热线 12356）。工具调用在响应流中编码为文本标记 `[RECOMMENDATION:...]` / `[CRISIS:...]`。
- **思维链流式传输**：支持 `thinking_delta` 事件缓冲和回放。
- **多模态**：支持 base64 图片内容块（`image` 类型）。
- **中间件**：CORS 全开放、自定义 HTTP 日志（掩码敏感头）。
- **知识库初始化**：懒加载，ChromaDB 不可用时优雅降级。

### 依赖
```
fastapi>=0.109.0
uvicorn[standard]>=0.27.0
anthropic>=0.25.0
pydantic>=2.5.0
python-dotenv>=1.0.0
httpx>=0.25.0
```
注意：`chromadb` 和 `pdfplumber` 是实际运行时依赖，但未列入 `requirements.txt`。

## 版本目录

依赖通过 `gradle/libs.versions.toml` 管理，在 `build.gradle.kts` 中通过 `libs.*` 访问器引用。关键版本：

| 依赖 | 版本 |
|---|---|
| AGP | 9.1.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2025.12.00 |
| KSP | 2.2.10-2.0.2 |
| Room | 2.7.1 |
| Hilt | 2.59.2 |
| Navigation | 2.7.7 |
| compileSdk | 36 (minorApiLevel=1) |
| minSdk | 31 |
| targetSdk | 36 |

## 添加新页面

1. 在 `ui/screens/<名称>/` 下创建 Composable
2. 在 `ui/navigation/Screen.kt` 的密封类中添加路由对象
3. 在 `ui/navigation/Navigation.kt` 中注册 Composable 目的地
4. 创建 `@HiltViewModel` 的 ViewModel（如需参数化注入，参考 `ChatViewModel` 的 `@AssistedInject` 模式）
5. ViewModel 由 Hilt 自动注入，无需手动 Factory

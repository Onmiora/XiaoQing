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

**MVVM，无 DI 框架。** ViewModel 使用 `ViewModelProvider.Factory` 模式，在 `MainActivity` 中手动实例化。`QingApplication` 持有 `QingDataStore` 和 `DemoModeManager` 的单例。

### 分层结构 (`com.onmi.qing/`)

| 层级 | 位置 | 说明 |
|---|---|---|
| UI | `ui/screens/`、`ui/components/` | Composable 页面、可复用组件 |
| 导航 | `ui/navigation/` | `Screen.kt`（路由密封类）、`Navigation.kt`（NavHost） |
| 主题 | `ui/theme/` | Material 3，动态取色（Android 12+），支持亮/暗主题 |
| ViewModel | `viewmodel/` | 8 个 ViewModel，各自带有 companion `Factory` |
| 数据模型 | `data/` | 数据类、问卷题目、算法逻辑 |
| 持久化 | `data/datastore/` | 仅使用 DataStore Preferences（无 Room），所有数据通过 Gson 序列化为 JSON |
| 远程 API | `data/remote/` | Retrofit + OkHttp，兼容 Anthropic 格式的 `/v1/messages` 端点，SSE 流式传输 |
| BLE | `ble/` | `HeartRateManager` — 心率监测设备的 GATT 客户端 |
| 演示模式 | `data/demo/` | 通过 `DemoModeManager` 切换的内存演示数据 |

### 关键架构要点

- **导航**：路由在 `Screen.kt` 中定义为密封类，底部导航栏项目为其子集。`Navigation.kt` 中注册所有 Composable 目的地。
- **数据持久化**：全部使用 DataStore Preferences + Gson JSON 序列化，无 SQLite/Room。包括聊天记录、心情日记、成就、心理维度和用户偏好。
- **AI 聊天**：`ChatViewModel` 连接兼容 Anthropic 格式的 API，支持 SSE 流式传输（通过 `SseEventParser`）和非流式回退。API 地址和模型可在设置中由用户配置。
- **心理维度**：`StateViewModel` 中跟踪 6 个浮点数（0–1）— 情绪稳定性、自我认知、压力管理、社交信心、睡眠质量、自我关怀。通过 `/v1/analyze` 端点由 AI 分析更新。
- **自适应布局**：使用 `WindowWidthSizeClass` — 平板/折叠屏使用 NavigationRail，手机使用自定义 `FloatingPillNavBar`。
- **无依赖注入、无图片加载库、无崩溃上报。**

## 后端服务 (QingAPI)

后端位于 `/Users/onmiora/Documents/QingAPI`，使用 Python + FastAPI 构建，提供 AI 聊天和心理分析 API。

### 技术栈
- **语言**：Python 3.14.3
- **框架**：FastAPI (async) + Uvicorn
- **LLM**：Anthropic SDK 连接智谱 AI 的 Anthropic 兼容端点 (`open.bigmodel.cn/api/anthropic`)，模型 `glm-4.5-air`
- **向量数据库**：ChromaDB（本地持久化）
- **嵌入模型**：通过 LM Studio 运行 `text-embedding-qwen3-embedding-0.6b`

### 启动命令
```bash
cd /Users/onmiora/Documents/QingAPI/QingBackend
python main.py  # 启动在 0.0.0.0:8000
```

### API 端点
| 端点 | 方法 | 说明 |
|---|---|---|
| `/v1/messages` | POST | AI 聊天（支持 SSE 流式传输） |
| `/v1/models` | GET | 列出可用模型 |
| `/v1/analyze` | POST | 心理维度分析（返回 6 个维度的变化量） |
| `/health` | GET | 健康检查 |

### RAG 管道
用户查询 → LM Studio 生成嵌入 → ChromaDB 向量搜索 → 相关心理学论文片段注入系统提示词 → LLM 生成带引用的回复。

### 工具调用
LLM 可调用两个工具：
- `recommend_feature`：推荐应用功能（呼吸练习、压力检测、心情日记、性格测试）
- `crisis_intervention`：检测到自杀/自残语言时触发危机干预卡片（热线 12356）

工具调用在响应流中转换为文本标记（`[RECOMMENDATION:...]` / `[CRISIS:...]`），供前端解析。

## 版本目录

依赖通过 `gradle/libs.versions.toml` 管理，在 `build.gradle.kts` 中通过 `libs.*` 访问器引用。关键版本：AGP 9.1.1、Kotlin 2.2.10、Compose BOM 2025.12.00、compileSdk 36、minSdk 31。

## 添加新页面

1. 在 `ui/screens/<名称>/` 下创建 Composable
2. 在 `ui/navigation/Screen.kt` 的密封类中添加路由对象
3. 在 `ui/navigation/Navigation.kt` 中注册 Composable 目的地
4. 如需独立状态管理，在 `viewmodel/` 中创建 ViewModel 并附带 `Factory` companion object，然后在 `MainActivity` 中实例化并传递

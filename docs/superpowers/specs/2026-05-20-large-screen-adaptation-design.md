# 大屏适配设计文档

日期：2026-05-20

## 概述

为 Qing 应用实现完整的大屏适配方案，包含三个核心部分：
1. 统一内容最大宽度为 720dp，居中显示
2. 将水平胶囊导航栏改造为竖向胶囊，放置在大屏左侧
3. 转为多 Activity 架构，支持 Activity Embedding 分屏

## 1. 内容宽度约束

### 设计

新增 `ConstrainedWidthContainer` 组件，所有屏幕内容包裹在此容器中。

```kotlin
@Composable
fun ConstrainedWidthContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
```

### 应用范围

所有 14 个屏幕的内容区域都需要包裹在 `ConstrainedWidthContainer` 中：
- HomeScreen、DiscoverScreen、MoodDiaryScreen、ProfileScreen
- ChatScreen、HistoryScreen、BreathingScreen
- SettingsScreen、DeveloperScreen
- AchievementScreen
- SbtiTestScreen、MbtiTestScreen、TestSelectionScreen
- StressDetectionScreen（6 个子页面）

### 与现有自适应布局的关系

- `adaptiveHorizontalPadding()` 继续作为内边距使用
- `ConstrainedWidthContainer` 在 padding 之外限制最大宽度
- `isCompactWidth()` 保留但不强制使用

## 2. 竖向胶囊导航栏

### 设计

新建 `VerticalPillNavBar` 组件，替代现有的 `NavigationRail`。

#### 组件规格

| 属性 | 值 |
|---|---|
| 宽度 | 约 120dp |
| 高度 | 撑满屏幕（`fillMaxHeight`） |
| 导航项 | 4 个（首页、发现、心情、我的） |
| 项布局 | 图标（左）+ 文字（右），横向排列 |
| 选中指示器 | 发光胶囊背景，弹性动画 |
| 主题 | 深色/浅色自适应，磨砂玻璃风格 |

#### 布局结构

```
┌──────────────────────────────────────────┐
│ ┌──────────┬────────────────────────────┐│
│ │ 🏠 首页  │                            ││
│ │ 🔍 发现  │     内容区域               ││
│ │ 📔 心情  │     (ConstrainedWidth      ││
│ │ 👤 我的  │      Container)            ││
│ │          │                            ││
│ └──────────┴────────────────────────────┘│
└──────────────────────────────────────────┘
```

#### 显示逻辑

- `WindowWidthSizeClass != Compact` 时显示竖向胶囊
- `WindowWidthSizeClass == Compact` 时显示水平胶囊（现有行为不变）
- 当前路由不属于底部导航 Tab 时，竖向胶囊隐藏，内容区占满宽度

#### 与现有 FloatingPillNavBar 的关系

- `FloatingPillNavBar` 保持不变，继续用于 Compact 模式
- `VerticalPillNavBar` 是新组件，复用相同的磨砂玻璃视觉风格
- 共享 `FloatingNavItem` 数据类和 `toFloatingNavItems()` 扩展函数

#### 动画

- 选中胶囊指示器：`animateFloatAsState` + `spring(DampingRatioMediumBouncy, StiffnessLow)`
- 图标缩放：选中时 1.1x，弹性动画
- 按压缩放：0.95x，弹性动画
- 进入/退出：`slideInHorizontally` / `slideOutHorizontally`

## 3. Activity 拆分与 Activity Embedding

### Activity 拆分方案

| Activity | 包含页面 | 路由 |
|---|---|---|
| MainActivity | 首页、发现、心情日记、我的、呼吸练习、成就 | NavHost 内部路由 |
| ChatActivity | 聊天、对话历史 | NavHost 内部路由 |
| SettingsActivity | 设置、开发者选项 | NavHost 内部路由 |
| TestActivity | SBTI测试、MBTI测试、选择测试 | NavHost 内部路由 |
| StressActivity | 压力检测 | NavHost 内部路由 |

### 各 Activity 结构

#### MainActivity

- 保留现有的 `QingApp` 组件作为入口
- 左侧竖向胶囊导航栏 + 右侧 NavHost
- 路由：home、discover、mood_diary、profile、breathing、achievement
- 通过 Intent 启动其他 Activity

#### ChatActivity

- 新建 `@AndroidEntryPoint` Activity
- 内部 NavHost 管理聊天和历史页面
- 路由：chat/{sessionId}、history
- 接收 Intent 参数：sessionId（可选）

#### SettingsActivity

- 新建 `@AndroidEntryPoint` Activity
- 内部 NavHost 管理设置和开发者页面
- 路由：settings、developer

#### TestActivity

- 新建 `@AndroidEntryPoint` Activity
- 内部 NavHost 管理测试相关页面
- 路由：test_selection、sbti_test、mbti_test

#### StressActivity

- 新建 `@AndroidEntryPoint` Activity
- 内部 NavHost 管理压力检测流程
- 路由：stress_detection 及其子页面

### Activity Embedding 配置

#### 依赖

在 `build.gradle.kts` 中添加：

```kotlin
implementation("androidx.window:window:1.3.0")
```

#### 分屏规则

在 `res/xml/main_split_config.xml` 中配置：

```xml
<SplitPairRuleFilter
    windowClass="EXPANDED"
    splitRatio="0.4"
    splitMinWidthDp="840">
    <SplitPair
        primaryActivity="com.onmi.qing.MainActivity"
        secondaryActivity="com.onmi.qing.ChatActivity" />
    <SplitPair
        primaryActivity="com.onmi.qing.MainActivity"
        secondaryActivity="com.onmi.qing.SettingsActivity" />
    <SplitPair
        primaryActivity="com.onmi.qing.MainActivity"
        secondaryActivity="com.onmi.qing.TestActivity" />
    <SplitPair
        primaryActivity="com.onmi.qing.MainActivity"
        secondaryActivity="com.onmi.qing.StressActivity" />
</SplitPairRuleFilter>
```

#### AndroidManifest 配置

在 `AndroidManifest.xml` 的 `<application>` 标签内添加：

```xml
<meta-data
    android:name="android.window.SplitPairRule"
    android:resource="@xml/main_split_config" />
```

每个新 Activity 都需要注册：

```xml
<activity android:name=".ChatActivity" android:exported="false" />
<activity android:name=".SettingsActivity" android:exported="false" />
<activity android:name=".TestActivity" android:exported="false" />
<activity android:name=".StressActivity" android:exported="false" />
```

### 跨 Activity 导航

#### 从 MainActivity 启动其他 Activity

```kotlin
// 启动聊天
val intent = Intent(context, ChatActivity::class.java).apply {
    putExtra("sessionId", sessionId)
}
context.startActivity(intent)

// 启动设置
context.startActivity(Intent(context, SettingsActivity::class.java))
```

#### 子 Activity 内部导航

每个子 Activity 内部使用自己的 `NavController`，与 MainActivity 互不影响。

#### 返回行为

- 子 Activity 内部：标准返回按钮，内部 NavHost 处理返回栈
- 子 Activity 返回 MainActivity：`finish()` 或系统返回键

## 4. 屏幕尺寸行为总结

| 屏幕尺寸 | 内容宽度 | 导航栏 | Activity Embedding |
|---|---|---|---|
| Compact（<600dp） | 全宽 | 底部水平胶囊（FloatingPillNavBar） | 不分屏 |
| Medium（600-840dp） | 最大 720dp 居中 | 左侧竖向胶囊（VerticalPillNavBar） | 不分屏 |
| Expanded（≥840dp） | 最大 720dp 居中 | 左侧竖向胶囊（VerticalPillNavBar） | 双 Activity 并排 |

## 5. 文件变更清单

### 新增文件

| 文件 | 说明 |
|---|---|
| `ui/components/VerticalPillNavBar.kt` | 竖向胶囊导航栏组件 |
| `ui/components/ConstrainedWidthContainer.kt` | 内容宽度约束容器 |
| `ChatActivity.kt` | 聊天 Activity |
| `SettingsActivity.kt` | 设置 Activity |
| `TestActivity.kt` | 测试 Activity |
| `StressActivity.kt` | 压力检测 Activity |
| `res/xml/main_split_config.xml` | Activity Embedding 分屏规则 |
| `ui/navigation/ChatNavHost.kt` | ChatActivity 内部导航 |
| `ui/navigation/SettingsNavHost.kt` | SettingsActivity 内部导航 |
| `ui/navigation/TestNavHost.kt` | TestActivity 内部导航 |
| `ui/navigation/StressNavHost.kt` | StressActivity 内部导航 |

### 修改文件

| 文件 | 变更 |
|---|---|
| `MainActivity.kt` | 替换 NavigationRail 为 VerticalPillNavBar，移除子页面路由 |
| `AdaptiveLayout.kt` | 新增 ConstrainedWidthContainer |
| `Navigation.kt` | 仅保留 MainActivity 的路由 |
| `Screen.kt` | 按 Activity 拆分路由常量 |
| `AndroidManifest.xml` | 注册新 Activity + Activity Embedding 配置 |
| `build.gradle.kts` | 添加 window 库依赖 |
| 所有 Screen 文件 | 包裹 ConstrainedWidthContainer |

## 6. 实施顺序

1. **Phase 1：内容宽度约束** — 新增 ConstrainedWidthContainer，应用到所有屏幕
2. **Phase 2：竖向胶囊导航栏** — 新建 VerticalPillNavBar，替换 NavigationRail
3. **Phase 3：Activity 拆分** — 创建 4 个新 Activity，迁移路由
4. **Phase 4：Activity Embedding** — 配置分屏规则，测试大屏行为

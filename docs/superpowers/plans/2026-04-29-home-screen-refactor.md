# Home Screen Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the home screen from 5 equal-weight cards to a Hero + auxiliary cards layout with clear visual hierarchy, using MD3 dynamic color.

**Architecture:** Single file rewrite of `HomeScreen.kt`. The Hero card merges greeting, psychology dimensions, and inline mood recording. QuickAccessCard stays as-is. Removed cards (DailyGoals, AchievementOverview) and floating mood button are deleted. Navigation.kt call site updated to drop unused ViewModel params and callbacks.

**Tech Stack:** Jetpack Compose, Material 3, MD3 dynamic color (Android 12+), existing ViewModels (HomeViewModel, MoodViewModel)

---

### Task 1: Rewrite HomeScreen.kt

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt`

This is the main task. The entire file is rewritten. The new structure has 3 composables: `HomeScreen` (top-level), `HeroCard`, and `InlineMoodInput`. The existing `QuickAccessCard` and `QuickAccessButton` are kept with minimal changes. All other composables (`GreetingBanner`, `DailyCheckInCard`, `DailyGoalsCard`, `AchievementOverviewCard`, `GoalItemRow`, `GoalItem`) and the floating button / BottomSheet code are removed.

- [ ] **Step 1: Replace the entire HomeScreen.kt file**

Write the following complete file:

```kotlin
package com.onmi.qing.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.PsychologyDimension
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.ui.components.GlowProgressRing
import com.onmi.qing.ui.components.MoodOption
import com.onmi.qing.ui.components.moodOptions
import com.onmi.qing.ui.components.toDimensionIcon
import com.onmi.qing.ui.components.adaptiveHorizontalPadding
import com.onmi.qing.viewmodel.HomeViewModel
import com.onmi.qing.viewmodel.MoodViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    moodViewModel: MoodViewModel,
    demoModeManager: DemoModeManager,
    onStartChatClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onStressDetectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val psychologyDimensions by viewModel.psychologyDimensions.collectAsState()
    val dimensions by viewModel.allDimensions.collectAsState()
    val overallScore = ((psychologyDimensions.moodStability + psychologyDimensions.selfAwareness +
            psychologyDimensions.stressManagement + psychologyDimensions.socialConfidence +
            psychologyDimensions.sleepQuality + psychologyDimensions.selfCare) / 6 * 100).toInt()

    val hour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toInt()
    val greeting = when {
        hour < 6 -> "夜深了，注意休息哦"
        hour < 9 -> "早上好！今天也要加油呀"
        hour < 12 -> "上午好！保持好心情"
        hour < 14 -> "中午好！记得吃午饭"
        hour < 18 -> "下午好！来杯水休息一下吧"
        hour < 22 -> "晚上好！今天辛苦了"
        else -> "夜深了，早点休息吧"
    }

    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var moodReason by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            HeroCard(
                greeting = greeting,
                overallScore = overallScore,
                dimensions = dimensions,
                selectedMood = selectedMood,
                onMoodSelect = { mood ->
                    selectedMood = if (selectedMood == mood) null else mood
                    moodReason = ""
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = selectedMood != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                InlineMoodInput(
                    selectedMood = selectedMood,
                    reason = moodReason,
                    onReasonChange = { moodReason = it },
                    onSave = {
                        selectedMood?.let { mood ->
                            moodViewModel.addMoodEntry(mood, moodReason)
                            selectedMood = null
                            moodReason = ""
                        }
                    }
                )
            }
        }

        item {
            QuickAccessCard(
                onStartChatClick = onStartChatClick,
                onBreathingClick = onBreathingClick,
                onStressDetectionClick = onStressDetectionClick
            )
        }

        item { Spacer(modifier = Modifier.height(130.dp)) }
    }
}

@Composable
private fun HeroCard(
    greeting: String,
    overallScore: Int,
    dimensions: List<PsychologyDimension>,
    selectedMood: MoodType?,
    onMoodSelect: (MoodType) -> Unit
) {
    val currentDate = SimpleDateFormat("MM月dd日 E", Locale.getDefault()).format(Date())

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 顶部：日期 + 问候 + 心理分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$overallScore",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "心理健康分",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 心理维度环
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "← 拖动查看全部 →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(dimensions) { dimension ->
                    DimensionRingItem(dimension = dimension)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 心情选择 chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodOptions.forEach { option ->
                    MoodFilterChip(
                        option = option,
                        selected = selectedMood == option.type,
                        onClick = { onMoodSelect(option.type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodFilterChip(
    option: MoodOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        leadingIcon = {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = option.color.copy(alpha = 0.2f),
            selectedLabelColor = option.color,
            selectedLeadingIconColor = option.color,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) option.color else Color.Transparent,
            selectedBorderColor = option.color,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
}

@Composable
private fun InlineMoodInput(
    selectedMood: MoodType?,
    reason: String,
    onReasonChange: (String) -> Unit,
    onSave: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "发生了什么？",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "记录下此刻的心情来源...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                enabled = selectedMood != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "保存",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DimensionRingItem(dimension: PsychologyDimension) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        GlowProgressRing(
            progress = dimension.progress,
            size = 70.dp,
            strokeWidth = 6.dp,
            glowColor = Color(dimension.colorHex)
        ) {
            Icon(
                imageVector = dimension.name.toDimensionIcon(),
                contentDescription = dimension.name,
                tint = Color(dimension.colorHex),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dimension.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "${(dimension.progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(dimension.colorHex)
        )
    }
}

@Composable
private fun QuickAccessCard(
    onStartChatClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onStressDetectionClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "小晴建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessButton(
                    icon = Icons.Default.Psychology,
                    text = "聊聊",
                    onClick = onStartChatClick,
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
                QuickAccessButton(
                    icon = Icons.Default.Air,
                    text = "呼吸",
                    onClick = onBreathingClick,
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
                QuickAccessButton(
                    icon = Icons.Default.Favorite,
                    text = "压力检测",
                    onClick = onStressDetectionClick,
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun QuickAccessButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

- [ ] **Step 2: Verify the build compiles**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL. The new HomeScreen has fewer parameters so Navigation.kt will fail — that's expected and fixed in Task 2.

---

### Task 2: Update Navigation.kt call site

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt:92-108`

Remove unused ViewModel injections and the `onAchievementClick` callback from the HomeScreen composable call.

- [ ] **Step 1: Update the HomeScreen composable block in Navigation.kt**

Replace lines 92-108:

```kotlin
        composable(Screen.Home.route) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val usageStatsViewModel = hiltViewModel<UsageStatsViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                achievementViewModel = achievementViewModel,
                usageStatsViewModel = usageStatsViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onStartChatClick = { navController.navigate(Screen.Chat.route) },
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onAchievementClick = { navController.navigate(Screen.Achievement.route) },
                onStressDetectionClick = { navController.navigate(Screen.StressDetection.route) }
            )
        }
```

With:

```kotlin
        composable(Screen.Home.route) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onStartChatClick = { navController.navigate(Screen.Chat.route) },
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onStressDetectionClick = { navController.navigate(Screen.StressDetection.route) }
            )
        }
```

- [ ] **Step 2: Verify the build compiles**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt
git commit -m "refactor: simplify home screen to Hero + QuickAccess layout"
```

---

### Task 3: Build verification and cleanup check

**Files:**
- None modified (verification only)

- [ ] **Step 1: Full build verification**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL with no warnings about unused imports or unreachable code in the modified files.

- [ ] **Step 2: Verify MoodBottomSheet is still used elsewhere**

Run: `grep -r "MoodBottomSheet" app/src/main/java/ --include="*.kt" | grep -v "MoodBottomSheet.kt"`

If the output is empty, `MoodBottomSheet.kt` is unused. It can be kept (the spec says to keep it) but note it for future cleanup. If used elsewhere, no action needed.

- [ ] **Step 3: Verify no broken references**

Run: `grep -r "DailyGoalsCard\|AchievementOverviewCard\|GreetingBanner\|DailyCheckInCard" app/src/main/java/ --include="*.kt"`

Expected: No results — these composables were private to HomeScreen.kt and are now removed.

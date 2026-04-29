# Phase 4: UI Decomposition & Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decompose DiscoverScreen.kt (1403 lines) into focused files, extract shared components (AnimatedCard, MoodHelpers, DimensionHelpers), and clean up dead code and known bugs.

**Architecture:** DiscoverScreen becomes a thin orchestrator (~150 lines). Sub-composables live in sibling files. Shared utilities go in `ui/components/`. Dead code is deleted.

**Tech Stack:** Jetpack Compose, Material 3

**Prerequisite:** Phases 1-3 must be complete.

---

### Task 4.1: Extract AnimatedCard to Shared Component

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ui/components/AnimatedCard.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt`

- [ ] **Step 1: Create AnimatedCard.kt**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun AnimatedCard(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 100L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) +
                scaleIn(initialScale = 0.95f, animationSpec = tween(500)),
        modifier = modifier
    ) {
        Box(content = content)
    }
}
```

- [ ] **Step 2: Remove AnimatedCard from DiscoverScreen.kt**

Delete the `AnimatedCard` composable function (lines 1362-1403 in the original file) and add import:
```kotlin
import com.onmi.qing.ui.components.AnimatedCard
```

- [ ] **Step 3: Remove AnimatedCard from HomeScreen.kt**

Delete the `AnimatedCard` composable function (lines 668-709 in the original file) and add import:
```kotlin
import com.onmi.qing.ui.components.AnimatedCard
```

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/components/AnimatedCard.kt \
       app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt \
       app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt
git commit -m "refactor: extract AnimatedCard to shared component"
```

---

### Task 4.2: Extract MoodHelpers

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ui/components/MoodHelpers.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/mooddiary/MoodDiaryScreen.kt`

- [ ] **Step 1: Create MoodHelpers.kt**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.onmi.qing.data.MoodType

fun MoodType.toColor(): Color = when (this) {
    MoodType.HAPPY -> Color(0xFFFFD700)
    MoodType.CALM -> Color(0xFF10B981)
    MoodType.UNHAPPY -> Color(0xFFEF4444)
}

fun MoodType.toIcon(): ImageVector = when (this) {
    MoodType.HAPPY -> Icons.Filled.SentimentSatisfied
    MoodType.CALM -> Icons.Filled.SentimentNeutral
    MoodType.UNHAPPY -> Icons.Filled.SentimentDissatisfied
}

fun MoodType.toDisplayName(): String = when (this) {
    MoodType.HAPPY -> "开心"
    MoodType.CALM -> "平静"
    MoodType.UNHAPPY -> "不开心"
}
```

- [ ] **Step 2: Update DiscoverScreen.kt**

Replace the local `getMoodColor`, `getMoodIcon`, `getMoodText` functions with imports from MoodHelpers:
```kotlin
import com.onmi.qing.ui.components.toColor
import com.onmi.qing.ui.components.toIcon
import com.onmi.qing.ui.components.toDisplayName
```

Replace calls:
- `getMoodColor(mood)` → `mood.toColor()`
- `getMoodIcon(mood)` → `mood.toIcon()`
- `getMoodText(mood)` → `mood.toDisplayName()`

Delete the local `getMoodColor`, `getMoodIcon`, `getMoodText` functions.

- [ ] **Step 3: Update MoodDiaryScreen.kt**

Same replacement as above.

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/components/MoodHelpers.kt \
       app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt \
       app/src/main/java/com/onmi/qing/ui/screens/mooddiary/MoodDiaryScreen.kt
git commit -m "refactor: extract MoodHelpers as shared extension functions"
```

---

### Task 4.3: Extract DimensionHelpers

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ui/components/DimensionHelpers.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/components/AnalysisBottomSheet.kt`

- [ ] **Step 1: Create DimensionHelpers.kt**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun String.toDimensionColor(): Color = when (this) {
    "情绪稳定" -> Color(0xFF10B981)
    "自我认知" -> Color(0xFF3B82F6)
    "压力管理" -> Color(0xFFF59E0B)
    "社交信心" -> Color(0xFF8B5CF6)
    "睡眠质量" -> Color(0xFF06B6D4)
    "自我关怀" -> Color(0xFFEC4899)
    else -> Color.Gray
}

fun String.toDimensionIcon(): ImageVector = when (this) {
    "情绪稳定" -> Icons.Filled.Favorite
    "自我认知" -> Icons.Filled.Visibility
    "压力管理" -> Icons.Filled.SelfImprovement
    "社交信心" -> Icons.Filled.SocialDistance
    "睡眠质量" -> Icons.Filled.NightsStay
    "自我关怀" -> Icons.Filled.Psychology
    else -> Icons.Filled.Psychology
}
```

- [ ] **Step 2: Update AnalysisBottomSheet.kt**

Replace the local `getDimensionColor` function with:
```kotlin
import com.onmi.qing.ui.components.toDimensionColor
```

Replace `getDimensionColor(name)` → `name.toDimensionColor()`

Delete the local `getDimensionColor` function.

- [ ] **Step 3: Update HomeScreen.kt**

Replace the local `getDimensionIcon` function with:
```kotlin
import com.onmi.qing.ui.components.toDimensionIcon
```

Replace `getDimensionIcon(name)` → `name.toDimensionIcon()`

Delete the local `getDimensionIcon` function.

- [ ] **Step 4: Update DiscoverScreen.kt**

Same as above if it has local dimension helpers.

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/components/DimensionHelpers.kt \
       app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt \
       app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt \
       app/src/main/java/com/onmi/qing/ui/components/AnalysisBottomSheet.kt
git commit -m "refactor: extract DimensionHelpers as shared extension functions"
```

---

### Task 4.4: Decompose DiscoverScreen.kt

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt` (slim down to orchestrator)
- Create: `app/src/main/java/com/onmi/qing/ui/screens/discover/MoodDashboardCard.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/screens/discover/FeatureCards.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/screens/discover/InfoCards.kt`

- [ ] **Step 1: Read the current DiscoverScreen.kt to identify exact cut points**

Read the file and identify the composable function boundaries:
- `MoodDashboardCard` and all its sub-composables → `MoodDashboardCard.kt`
- `BreathingExerciseCard`, `FunTestCard`, `StressDetectionCard`, `AchievementStatCard`, `HistoryStatCard` → `FeatureCards.kt`
- `HotlineCard`, `TipCard` → `InfoCards.kt`

- [ ] **Step 2: Create MoodDashboardCard.kt**

Move these composables from DiscoverScreen.kt:
- `MoodDashboardCard`
- `CurrentMoodSection`
- `MoodTrendSection`
- `MoodCountSection`
- `MoodDistributionSection`
- `MoodDistributionRing`
- `MoodLegendItem`
- `MoodMiniTrendChart`
- `moodToValue` helper function

Add necessary imports. The file should start with:
```kotlin
package com.onmi.qing.ui.screens.discover

// ... all necessary imports moved from DiscoverScreen.kt
```

- [ ] **Step 3: Create FeatureCards.kt**

Move these composables:
- `BreathingExerciseCard`
- `FunTestCard`
- `StressDetectionCard`
- `AchievementStatCard`
- `HistoryStatCard`

- [ ] **Step 4: Create InfoCards.kt**

Move these composables:
- `HotlineCard`
- `TipCard`

- [ ] **Step 5: Slim down DiscoverScreen.kt**

The main `DiscoverScreen` composable becomes a thin orchestrator that imports and composes the extracted components:

```kotlin
package com.onmi.qing.ui.screens.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.viewmodel.PsychologyViewModel

@Composable
fun DiscoverScreen(
    psychologyViewModel: PsychologyViewModel,
    achievementViewModel: AchievementViewModel,
    moodViewModel: MoodViewModel,
    demoModeManager: DemoModeManager,
    onBreathingClick: () -> Unit,
    onAchievementClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStressDetectionClick: () -> Unit,
    onFunTestClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MoodDashboardCard(moodViewModel = moodViewModel, demoModeManager = demoModeManager)

        FeatureCardsSection(
            onBreathingClick = onBreathingClick,
            onStressDetectionClick = onStressDetectionClick,
            onFunTestClick = onFunTestClick,
            onAchievementClick = onAchievementClick,
            onHistoryClick = onHistoryClick,
            achievementViewModel = achievementViewModel,
            demoModeManager = demoModeManager
        )

        TipCard()

        HotlineCard()
    }
}
```

- [ ] **Step 6: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/screens/discover/
git commit -m "refactor: decompose DiscoverScreen into MoodDashboard, FeatureCards, InfoCards"
```

---

### Task 4.5: Fix Bugs and Clean Dead Code

**Files:**
- Various

- [ ] **Step 1: Fix Achievement iconName leading space**

In `app/src/main/java/com/onmi/qing/data/Achievement.kt`, line 107:
```kotlin
// Before:
iconName = " tranquility"
// After:
iconName = "tranquility"
```

- [ ] **Step 2: Fix DailyGoalsCard progress bar bug**

In `HomeScreen.kt`, find the progress calculation and fix:
```kotlin
// Before:
completedCount / 3f
// After:
completedCount / goals.size.toFloat()
```

- [ ] **Step 3: Remove AnimatedVisibility(visible = true) in ChatScreen**

In `ChatScreen.kt`, find the two `AnimatedVisibility(visible = true, ...)` wrappers and either:
- Remove the `AnimatedVisibility` wrapper entirely (keep the content)
- Or bind `visible` to a meaningful state variable

- [ ] **Step 4: Remove if(true) in PermissionBottomSheet**

In `PermissionBottomSheet.kt`, remove the dead `if (true)` condition.

- [ ] **Step 5: Remove Screen.SbtiResult dead route**

In `Screen.kt`, delete the `SbtiResult` data object (lines 116-121).

- [ ] **Step 6: Remove parseMbtiQuestions dead code**

In `MbtiQuestion.kt`, delete the `parseMbtiQuestions()` function.

- [ ] **Step 7: Remove DeveloperScreen empty onClick test items**

In `DeveloperScreen.kt`, remove or properly implement the test carousel items with empty onClick handlers.

- [ ] **Step 8: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "fix: resolve bugs and remove dead code

- Fix Achievement iconName leading space
- Fix DailyGoalsCard progress bar divisor
- Remove no-op AnimatedVisibility wrappers
- Remove dead if(true) condition
- Remove unused Screen.SbtiResult route
- Remove unused parseMbtiQuestions()
- Remove empty onClick test items"
```

---

### Task 4.6: Final Verification

- [ ] **Step 1: Full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run unit tests**

Run: `./gradlew test`
Expected: All pass

- [ ] **Step 3: Verify file sizes**

```bash
wc -l $(find app/src/main/java/com/onmi/qing -name "*.kt" | sort) | sort -rn | head -20
```

Verify:
- `DiscoverScreen.kt` is under 200 lines
- No single file exceeds 600 lines
- `StateViewModel.kt` no longer exists

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: Phase 4 complete — UI decomposition and cleanup

All 4 phases of the architecture refactoring are now complete."
```

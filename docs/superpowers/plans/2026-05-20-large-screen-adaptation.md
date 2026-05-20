# Large Screen Adaptation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full large screen adaptation for the Qing app: 720dp max content width, vertical capsule nav bar, multi-Activity architecture with Activity Embedding.

**Architecture:** ConstrainedWidthContainer wraps screen content to cap at 720dp. VerticalPillNavBar replaces NavigationRail for non-Compact screens. App splits into 5 Activities (MainActivity + 4 sub-Activities) with Activity Embedding for side-by-side display on Expanded screens.

**Tech Stack:** Jetpack Compose, Material 3, Hilt, Navigation Compose, Window Manager (Activity Embedding)

---

## File Structure

### New Files

| File | Responsibility |
|---|---|
| `app/src/main/java/com/onmi/qing/ui/components/ConstrainedWidthContainer.kt` | Content width constraint composable |
| `app/src/main/java/com/onmi/qing/ui/components/VerticalPillNavBar.kt` | Vertical capsule nav bar for large screens |
| `app/src/main/java/com/onmi/qing/ChatActivity.kt` | Chat + History Activity |
| `app/src/main/java/com/onmi/qing/ui/navigation/ChatNavHost.kt` | ChatActivity internal navigation |
| `app/src/main/java/com/onmi/qing/SettingsActivity.kt` | Settings + Developer Activity |
| `app/src/main/java/com/onmi/qing/ui/navigation/SettingsNavHost.kt` | SettingsActivity internal navigation |
| `app/src/main/java/com/onmi/qing/TestActivity.kt` | SBTI/MBTI Test Activity |
| `app/src/main/java/com/onmi/qing/ui/navigation/TestNavHost.kt` | TestActivity internal navigation |
| `app/src/main/java/com/onmi/qing/StressActivity.kt` | Stress Detection Activity |
| `app/src/main/java/com/onmi/qing/ui/navigation/StressNavHost.kt` | StressActivity internal navigation |
| `app/src/main/res/xml/main_split_config.xml` | Activity Embedding split rules |

### Modified Files

| File | Changes |
|---|---|
| `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt` | Remove migrated routes, add cross-Activity navigation |
| `app/src/main/java/com/onmi/qing/MainActivity.kt` | Replace NavigationRail with VerticalPillNavBar |
| `app/src/main/AndroidManifest.xml` | Register new Activities + Embedding config |
| `gradle/libs.versions.toml` | Add window library version |
| `app/build.gradle.kts` | Add window dependency |
| All 14 screen files | Wrap content in ConstrainedWidthContainer |

---

## Phase 1: Content Width Constraint

### Task 1: Create ConstrainedWidthContainer

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ui/components/ConstrainedWidthContainer.kt`

- [ ] **Step 1: Create the ConstrainedWidthContainer composable**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Constrain content to a maximum width of 720dp, centered horizontally.
 * On screens narrower than 720dp, content fills the available width.
 */
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

- [ ] **Step 2: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/components/ConstrainedWidthContainer.kt
git commit -m "feat: add ConstrainedWidthContainer for 720dp max content width"
```

---

### Task 2: Apply ConstrainedWidthContainer to All Screens

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/discover/DiscoverScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/mooddiary/MoodDiaryScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/breathing/BreathingScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/developer/DeveloperScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/achievement/AchievementScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/sbti/SbtiTestScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/mbti/MbtiTestScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/discover/TestSelectionScreen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/stressdetection/StressDetectionScreen.kt`

The pattern for each screen: wrap the entire function body in `ConstrainedWidthContainer`. For screens with `Scaffold`, wrap inside the Scaffold's `content` lambda. For screens with `LazyColumn`/`Column`/`Box` as root, wrap the root container.

- [ ] **Step 1: Apply to HomeScreen.kt**

Add import at top:
```kotlin
import com.onmi.qing.ui.components.ConstrainedWidthContainer
```

Wrap the `LazyColumn` (line 100) inside `ConstrainedWidthContainer`:
```kotlin
ConstrainedWidthContainer(
    modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... existing content unchanged
    }
}
```

- [ ] **Step 2: Apply to DiscoverScreen.kt**

Add import. Wrap the root `Column` (line 54) inside `ConstrainedWidthContainer`:
```kotlin
ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        // ... existing params
    ) {
        // ... existing content unchanged
    }
}
```

- [ ] **Step 3: Apply to MoodDiaryScreen.kt**

Add import. The outermost container is `Scaffold` (line 92). Wrap inside Scaffold's content:
```kotlin
Scaffold(
    // ... existing Scaffold params
) { innerPadding ->
    ConstrainedWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // ... existing Scaffold content
    }
}
```

- [ ] **Step 4: Apply to ProfileScreen.kt**

Add import. Wrap the root `Column` (line 72) inside `ConstrainedWidthContainer`:
```kotlin
ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        // ... existing params
    ) {
        // ... existing content unchanged
    }
}
```

- [ ] **Step 5: Apply to ChatScreen.kt**

Add import. The outermost container is `Scaffold` (line 174). Wrap inside Scaffold's content lambda. The chat input bar at the bottom should remain full-width, so wrap only the message list area:

Actually, for ChatScreen, the entire Scaffold content should be constrained since the chat bubbles already have their own width. Wrap inside Scaffold:
```kotlin
Scaffold(
    // ... existing Scaffold params
) { innerPadding ->
    ConstrainedWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // ... existing Scaffold content (Column with LazyColumn + input bar)
    }
}
```

- [ ] **Step 6: Apply to HistoryScreen.kt**

Add import. Wrap inside Scaffold's content:
```kotlin
Scaffold(
    // ... existing Scaffold params
) { innerPadding ->
    ConstrainedWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // ... existing Scaffold content
    }
}
```

- [ ] **Step 7: Apply to BreathingScreen.kt**

Add import. The outermost container is `Box` (line 158). Wrap:
```kotlin
ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        // ... existing params
    ) {
        // ... existing content unchanged
    }
}
```

- [ ] **Step 8: Apply to SettingsScreen.kt**

Add import. Wrap inside Scaffold's content:
```kotlin
Scaffold(
    // ... existing Scaffold params
) { innerPadding ->
    ConstrainedWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // ... existing Scaffold content
    }
}
```

- [ ] **Step 9: Apply to DeveloperScreen.kt**

Add import. Wrap inside Scaffold's content:
```kotlin
Scaffold(
    // ... existing Scaffold params
) { innerPadding ->
    ConstrainedWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        // ... existing Scaffold content
    }
}
```

- [ ] **Step 10: Apply to AchievementScreen.kt**

Add import. Wrap the root `Column` (line 76) inside `ConstrainedWidthContainer`:
```kotlin
ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        // ... existing params
    ) {
        // ... existing content unchanged
    }
}
```

- [ ] **Step 11: Apply to SbtiTestScreen.kt**

Add import. The function body has a `when` block (line 23) that renders different sub-composables. Wrap the entire body:
```kotlin
fun SbtiTestScreen(
    viewModel: SbtiViewModel,
    onBackClick: () -> Unit,
    onComplete: (isRetake: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
        val uiState by viewModel.uiState.collectAsState()
        when {
            // ... existing when block content
        }
    }
}
```

- [ ] **Step 12: Apply to MbtiTestScreen.kt**

Add import. Similar to SbtiTestScreen — wrap the entire body:
```kotlin
fun MbtiTestScreen(
    viewModel: MbtiViewModel,
    onBackClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
        val uiState by viewModel.uiState.collectAsState()
        // ... existing body content
    }
}
```

- [ ] **Step 13: Apply to TestSelectionScreen.kt**

Add import. The outermost is `Surface` (line 29). Wrap inside Surface:
```kotlin
Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
) {
    ConstrainedWidthContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        )
    }
    // ... existing ModalBottomSheet
}
```

- [ ] **Step 14: Apply to StressDetectionScreen.kt**

Add import. The outermost is `Box` (line 33). Wrap:
```kotlin
ConstrainedWidthContainer(modifier = modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        // ... existing params
    ) {
        // ... existing content unchanged
    }
}
```

- [ ] **Step 15: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 16: Commit**

```bash
git add -A
git commit -m "feat: apply ConstrainedWidthContainer to all 14 screens"
```

---

## Phase 2: Vertical Pill Nav Bar

### Task 3: Create VerticalPillNavBar

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ui/components/VerticalPillNavBar.kt`

- [ ] **Step 1: Create the VerticalPillNavBar composable**

This component reuses `FloatingNavItem` from `FloatingPillNavBar.kt` and applies the same glassmorphism visual style in a vertical layout.

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerticalPillNavBar(
    visible: Boolean,
    currentRoute: String,
    navItems: List<FloatingNavItem>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val pillWidth = 120.dp
    val itemHeight = 56.dp
    val totalHeight = (itemHeight * navItems.size) + 32.dp // 16dp padding top+bottom

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(pillWidth + 24.dp), // extra space for padding
            contentAlignment = Alignment.Center
        ) {
            // Outer shadow
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.4f else 0.15f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.6f else 0.25f)
                    )
            ) {
                // Frosted glass background
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            )
                        )
                        .blur(8.dp)
                )

                // Highlight strip
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color.White.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                )

                // Border
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                } else {
                                    listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.04f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )

                // Inner background
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.8f),
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            )
                        )
                )

                // Nav items container
                Column(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sliding selected capsule background
                    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }
                    val capsuleOffset by animateFloatAsState(
                        targetValue = if (selectedIndex >= 0) selectedIndex * (itemHeight.value + 4f) else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "capsuleOffset"
                    )

                    // Selected capsule glow
                    if (selectedIndex >= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .offset(y = capsuleOffset.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Selected capsule main body
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .offset(y = capsuleOffset.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            )
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            )
                                        }
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = 0.7f),
                                                Color.Transparent
                                            )
                                        }
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                        )
                    }

                    // Nav buttons
                    navItems.forEach { item ->
                        VerticalNavItemButton(
                            item = item,
                            selected = currentRoute == item.route,
                            isDarkTheme = isDarkTheme,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalNavItemButton(
    item: FloatingNavItem,
    selected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val contentColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isDarkTheme -> Color.White.copy(alpha = 0.75f)
        else -> Color.Black.copy(alpha = 0.65f)
    }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            modifier = Modifier
                .size(if (selected) 22.dp else 20.dp)
                .scale(iconScale),
            tint = contentColor
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

// Reuse luminance from FloatingPillNavBar - make it internal
internal fun Color.luminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
```

Note: The `luminance()` function is already defined as `private` in `FloatingPillNavBar.kt`. Either make it `internal` in `FloatingPillNavBar.kt` and reuse, or duplicate it in `VerticalPillNavBar.kt` as `private`. The simpler approach is to make it `internal` in `FloatingPillNavBar.kt` (line 384: change `private` to `internal`) and remove the duplicate from `VerticalPillNavBar.kt`.

- [ ] **Step 2: Make luminance() internal in FloatingPillNavBar.kt**

In `FloatingPillNavBar.kt`, line 384, change:
```kotlin
private fun Color.luminance(): Float {
```
to:
```kotlin
internal fun Color.luminance(): Float {
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/components/VerticalPillNavBar.kt app/src/main/java/com/onmi/qing/ui/components/FloatingPillNavBar.kt
git commit -m "feat: add VerticalPillNavBar for large screen navigation"
```

---

### Task 4: Integrate VerticalPillNavBar into MainActivity

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/MainActivity.kt`

- [ ] **Step 1: Replace NavigationRail with VerticalPillNavBar in QingApp**

In `MainActivity.kt`, replace the `if (useNavigationRail && showBottomBar)` block (lines 177-233) with:

```kotlin
if (useNavigationRail) {
    // Large screen: vertical pill nav bar on the left
    Row(modifier = Modifier.fillMaxSize()) {
        VerticalPillNavBar(
            visible = showBottomBar,
            currentRoute = currentRoute ?: Screen.Home.route,
            navItems = Screen.toFloatingNavItems(),
            onNavigate = { route ->
                if (currentRoute != route) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )

        Surface(
            modifier = Modifier.weight(1f).fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            QingNavHost(
                navController = navController,
                chatRepository = application.chatRepository,
                demoModeManager = application.demoModeManager,
                isDarkTheme = isDarkTheme,
                followSystemTheme = followSystemTheme,
                onThemeChange = onThemeChange,
                onFollowSystemChange = onFollowSystemChange,
                paddingValues = androidx.compose.foundation.layout.PaddingValues()
            )
        }
    }
} else {
    // ... existing FloatingPillNavBar code unchanged (lines 234-275)
}
```

- [ ] **Step 2: Update imports**

Add to imports:
```kotlin
import com.onmi.qing.ui.components.VerticalPillNavBar
```

Remove unused imports:
```kotlin
// Remove these:
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/MainActivity.kt
git commit -m "feat: replace NavigationRail with VerticalPillNavBar for large screens"
```

---

## Phase 3: Activity Split

### Task 5: Create ChatActivity

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ChatActivity.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/navigation/ChatNavHost.kt`

- [ ] **Step 1: Create ChatNavHost.kt**

```kotlin
package com.onmi.qing.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.ui.screens.chat.ChatScreen
import com.onmi.qing.ui.screens.history.HistoryScreen
import com.onmi.qing.viewmodel.AnalysisViewModel
import com.onmi.qing.viewmodel.ChatViewModel

@Composable
fun ChatNavHost(
    navController: NavHostController,
    startDestination: String,
    chatRepository: ChatRepository,
    demoModeManager: DemoModeManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.85f, animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
                scaleIn(initialScale = 1.15f, animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 1.15f, animationSpec = tween(200))
        }
    ) {
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            val chatViewModel: ChatViewModel = hiltViewModel<ChatViewModel, ChatViewModel.ChatViewModelFactory>(
                creationCallback = { factory ->
                    factory.create(sessionId)
                }
            )
            ChatScreen(
                viewModel = chatViewModel,
                demoModeManager = demoModeManager,
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onNewChatClick = { chatViewModel.createNewSession() },
                onBackClick = onBackClick,
                onBreathingClick = {},
                onStressDetectionClick = {},
                onFunTestClick = {}
            )
        }

        composable(Screen.History.route) {
            val analysisViewModel = hiltViewModel<AnalysisViewModel>()
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { session ->
                    navController.navigate(Screen.Chat.createRoute(session.id))
                },
                analysisViewModel = analysisViewModel,
                chatRepository = chatRepository,
                demoModeManager = demoModeManager
            )
        }
    }
}
```

- [ ] **Step 2: Create ChatActivity.kt**

```kotlin
package com.onmi.qing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.onmi.qing.data.datastore.UserPreferences
import com.onmi.qing.ui.navigation.ChatNavHost
import com.onmi.qing.ui.navigation.Screen
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_SESSION_ID = "sessionId"

        fun createIntent(context: Context, sessionId: String? = null): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                if (sessionId != null) {
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val application = application as QingApplication

        setContent {
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) systemDark else userPrefs.isDarkTheme

            QingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val startDestination = if (sessionId != null) {
                    Screen.Chat.createRoute(sessionId)
                } else {
                    Screen.Chat.createRoute()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        chatRepository = application.chatRepository,
                        demoModeManager = application.demoModeManager,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ChatActivity.kt app/src/main/java/com/onmi/qing/ui/navigation/ChatNavHost.kt
git commit -m "feat: create ChatActivity with internal NavHost"
```

---

### Task 6: Create SettingsActivity

**Files:**
- Create: `app/src/main/java/com/onmi/qing/SettingsActivity.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/navigation/SettingsNavHost.kt`

- [ ] **Step 1: Create SettingsNavHost.kt**

```kotlin
package com.onmi.qing.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.ui.screens.developer.DeveloperScreen
import com.onmi.qing.ui.screens.settings.SettingsScreen
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.PsychologyViewModel
import com.onmi.qing.viewmodel.SettingsViewModel

@Composable
fun SettingsNavHost(
    navController: NavHostController,
    demoModeManager: DemoModeManager,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.85f, animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
                scaleIn(initialScale = 1.15f, animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 1.15f, animationSpec = tween(200))
        }
    ) {
        composable(Screen.Settings.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                achievementViewModel = achievementViewModel,
                isDarkTheme = isDarkTheme,
                followSystemTheme = followSystemTheme,
                onThemeChange = onThemeChange,
                onFollowSystemChange = onFollowSystemChange,
                onBackClick = onBackClick,
                onDeveloperClick = { navController.navigate(Screen.Developer.route) }
            )
        }

        composable(Screen.Developer.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            DeveloperScreen(
                psychologyViewModel = psychologyViewModel,
                achievementViewModel = achievementViewModel,
                settingsViewModel = settingsViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
```

- [ ] **Step 2: Create SettingsActivity.kt**

```kotlin
package com.onmi.qing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.onmi.qing.data.datastore.UserPreferences
import com.onmi.qing.ui.navigation.SettingsNavHost
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as QingApplication
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

        setContent {
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) systemDark else userPrefs.isDarkTheme
            val composeScope = rememberCoroutineScope()

            QingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsNavHost(
                        navController = navController,
                        demoModeManager = application.demoModeManager,
                        isDarkTheme = isDarkTheme,
                        followSystemTheme = userPrefs.followSystemTheme,
                        onThemeChange = { isDark ->
                            composeScope.launch { application.dataStore.updateTheme(isDark) }
                        },
                        onFollowSystemChange = { follow ->
                            composeScope.launch { application.dataStore.updateFollowSystemTheme(follow) }
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/SettingsActivity.kt app/src/main/java/com/onmi/qing/ui/navigation/SettingsNavHost.kt
git commit -m "feat: create SettingsActivity with internal NavHost"
```

---

### Task 7: Create TestActivity

**Files:**
- Create: `app/src/main/java/com/onmi/qing/TestActivity.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/navigation/TestNavHost.kt`

- [ ] **Step 1: Create TestNavHost.kt**

```kotlin
package com.onmi.qing.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.onmi.qing.ui.screens.discover.TestSelectionScreen
import com.onmi.qing.ui.screens.mbti.MbtiTestScreen
import com.onmi.qing.ui.screens.sbti.SbtiTestScreen
import com.onmi.qing.viewmodel.MbtiViewModel
import com.onmi.qing.viewmodel.SbtiViewModel

@Composable
fun TestNavHost(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TestSelection.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.85f, animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
                scaleIn(initialScale = 1.15f, animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 1.15f, animationSpec = tween(200))
        }
    ) {
        composable(Screen.TestSelection.route) {
            TestSelectionScreen(
                onNavigateToSbti = {
                    navController.navigate(Screen.SbtiTest.route) {
                        popUpTo(Screen.TestSelection.route) { inclusive = true }
                    }
                },
                onNavigateToMbti = {
                    navController.navigate(Screen.MbtiTest.route) {
                        popUpTo(Screen.TestSelection.route) { inclusive = true }
                    }
                },
                onBackClick = onBackClick
            )
        }

        composable(Screen.SbtiTest.route) {
            val sbtiViewModel = hiltViewModel<SbtiViewModel>()
            SbtiTestScreen(
                viewModel = sbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { onBackClick() }
            )
        }

        composable(Screen.MbtiTest.route) {
            val mbtiViewModel = hiltViewModel<MbtiViewModel>()
            MbtiTestScreen(
                viewModel = mbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { onBackClick() }
            )
        }
    }
}
```

- [ ] **Step 2: Create TestActivity.kt**

```kotlin
package com.onmi.qing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.onmi.qing.data.datastore.UserPreferences
import com.onmi.qing.ui.navigation.TestNavHost
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TestActivity : ComponentActivity() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, TestActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as QingApplication

        setContent {
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) systemDark else userPrefs.isDarkTheme

            QingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestNavHost(
                        navController = navController,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/TestActivity.kt app/src/main/java/com/onmi/qing/ui/navigation/TestNavHost.kt
git commit -m "feat: create TestActivity with SBTI/MBTI navigation"
```

---

### Task 8: Create StressActivity

**Files:**
- Create: `app/src/main/java/com/onmi/qing/StressActivity.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/navigation/StressNavHost.kt`

- [ ] **Step 1: Create StressNavHost.kt**

```kotlin
package com.onmi.qing.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.onmi.qing.ui.screens.stressdetection.StressDetectionScreen
import com.onmi.qing.viewmodel.StressDetectionViewModel

@Composable
fun StressNavHost(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StressDetection.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.85f, animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
                scaleIn(initialScale = 1.15f, animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 1.15f, animationSpec = tween(200))
        }
    ) {
        composable(Screen.StressDetection.route) {
            val stressDetectionViewModel = hiltViewModel<StressDetectionViewModel>()
            StressDetectionScreen(
                viewModel = stressDetectionViewModel,
                onBackClick = onBackClick,
                onBreathingClick = { /* breathing is in MainActivity, can't navigate cross-activity */ }
            )
        }
    }
}
```

- [ ] **Step 2: Create StressActivity.kt**

```kotlin
package com.onmi.qing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.onmi.qing.data.datastore.UserPreferences
import com.onmi.qing.ui.navigation.StressNavHost
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StressActivity : ComponentActivity() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, StressActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as QingApplication

        setContent {
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) systemDark else userPrefs.isDarkTheme

            QingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StressNavHost(
                        navController = navController,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/StressActivity.kt app/src/main/java/com/onmi/qing/ui/navigation/StressNavHost.kt
git commit -m "feat: create StressActivity for stress detection flow"
```

---

### Task 9: Update Navigation.kt — Remove Migrated Routes

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt`

- [ ] **Step 1: Remove migrated composable destinations**

Remove these blocks from `QingNavHost`:
- `Screen.Chat` composable (lines 148-172) — moved to ChatActivity
- `Screen.History` composable (lines 195-206) — moved to ChatActivity
- `Screen.Settings` composable (lines 217-230) — moved to SettingsActivity
- `Screen.Developer` composable (lines 232-243) — moved to SettingsActivity
- `Screen.SbtiTest` composable (lines 245-252) — moved to TestActivity
- `Screen.MbtiTest` composable (lines 254-261) — moved to TestActivity
- `Screen.TestSelection` composable (lines 263-277) — moved to TestActivity
- `Screen.StressDetection` composable (lines 183-193) — moved to StressActivity

Keep these:
- `Screen.Home` (line 94)
- `Screen.Discover` (line 107)
- `Screen.MoodDiary` (line 124)
- `Screen.Profile` (line 132)
- `Screen.Breathing` (line 174)
- `Screen.Achievement` (line 208)

- [ ] **Step 2: Remove unused imports**

Remove imports that are no longer needed:
```kotlin
import com.onmi.qing.ui.screens.chat.ChatScreen
import com.onmi.qing.ui.screens.history.HistoryScreen
import com.onmi.qing.ui.screens.settings.SettingsScreen
import com.onmi.qing.ui.screens.developer.DeveloperScreen
import com.onmi.qing.ui.screens.sbti.SbtiTestScreen
import com.onmi.qing.ui.screens.mbti.MbtiTestScreen
import com.onmi.qing.ui.screens.discover.TestSelectionScreen
import com.onmi.qing.ui.screens.stressdetection.StressDetectionScreen
import com.onmi.qing.viewmodel.AnalysisViewModel
import com.onmi.qing.viewmodel.ChatViewModel
import com.onmi.qing.viewmodel.PsychologyViewModel
import com.onmi.qing.viewmodel.SettingsViewModel
import com.onmi.qing.viewmodel.StressDetectionViewModel
import com.onmi.qing.viewmodel.SbtiViewModel
import com.onmi.qing.viewmodel.MbtiViewModel
```

- [ ] **Step 3: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt
git commit -m "refactor: remove migrated routes from Navigation.kt"
```

---

### Task 10: Update MainActivity — Add Cross-Activity Navigation

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/MainActivity.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt`

- [ ] **Step 1: Update Navigation.kt to accept cross-Activity navigation callbacks**

Update `QingNavHost` signature to add callbacks for launching sub-Activities:

```kotlin
@Composable
fun QingNavHost(
    navController: NavHostController,
    chatRepository: ChatRepository,
    demoModeManager: DemoModeManager,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    onLaunchChat: (String?) -> Unit = {},
    onLaunchSettings: () -> Unit = {},
    onLaunchTest: () -> Unit = {},
    onLaunchStress: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Update HomeScreen callsite to use onLaunchChat**

In the `Screen.Home` composable block, change:
```kotlin
onStartChatClick = { navController.navigate(Screen.Chat.createRoute()) },
```
to:
```kotlin
onStartChatClick = { onLaunchChat(null) },
```

Update `onStressDetectionClick`:
```kotlin
onStressDetectionClick = { onLaunchStress() }
```

- [ ] **Step 3: Update DiscoverScreen callsite**

Change:
```kotlin
onHistoryClick = { navController.navigate(Screen.History.route) },
onStressDetectionClick = { navController.navigate(Screen.StressDetection.route) },
onFunTestClick = { navController.navigate(Screen.TestSelection.route) }
```
to:
```kotlin
onHistoryClick = { onLaunchChat(null) },
onStressDetectionClick = { onLaunchStress() },
onFunTestClick = { onLaunchTest() }
```

- [ ] **Step 4: Update ProfileScreen callsite**

Change:
```kotlin
onHistoryClick = { navController.navigate(Screen.History.route) },
onSettingsClick = { navController.navigate(Screen.Settings.route) }
```
to:
```kotlin
onHistoryClick = { onLaunchChat(null) },
onSettingsClick = { onLaunchSettings() }
```

- [ ] **Step 5: Update BreathingScreen callsite**

The `BreathingScreen` stays in MainActivity, no changes needed.

- [ ] **Step 6: Update AchievementScreen callsite**

The `AchievementScreen` stays in MainActivity, no changes needed.

- [ ] **Step 7: Update MainActivity.kt QingApp to pass launch callbacks**

In `MainActivity.kt`, update the `QingNavHost` calls (both in `useNavigationRail` and `else` branches) to pass the new callbacks:

```kotlin
QingNavHost(
    navController = navController,
    chatRepository = application.chatRepository,
    demoModeManager = application.demoModeManager,
    isDarkTheme = isDarkTheme,
    followSystemTheme = followSystemTheme,
    onThemeChange = onThemeChange,
    onFollowSystemChange = onFollowSystemChange,
    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
    onLaunchChat = { sessionId ->
        startActivity(ChatActivity.createIntent(this@QingApp, sessionId))
    },
    onLaunchSettings = {
        startActivity(SettingsActivity.createIntent(this@QingApp))
    },
    onLaunchTest = {
        startActivity(TestActivity.createIntent(this@QingApp))
    },
    onLaunchStress = {
        startActivity(StressActivity.createIntent(this@QingApp))
    }
)
```

Note: `QingApp` is a `@Composable` function, so `this@QingApp` won't work. Use `LocalContext.current` instead:
```kotlin
val context = LocalContext.current
// ...
onLaunchChat = { sessionId ->
    context.startActivity(ChatActivity.createIntent(context, sessionId))
},
```

Add import:
```kotlin
import androidx.compose.ui.platform.LocalContext
```

- [ ] **Step 8: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/onmi/qing/MainActivity.kt app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt
git commit -m "feat: wire cross-Activity navigation from MainActivity"
```

---

### Task 11: Register Activities in AndroidManifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add new Activity declarations**

Inside the `<application>` tag, after the existing `<activity>` block (after line 37), add:

```xml
<activity
    android:name=".ChatActivity"
    android:exported="false"
    android:theme="@style/Theme.Qing" />
<activity
    android:name=".SettingsActivity"
    android:exported="false"
    android:theme="@style/Theme.Qing" />
<activity
    android:name=".TestActivity"
    android:exported="false"
    android:theme="@style/Theme.Qing" />
<activity
    android:name=".StressActivity"
    android:exported="false"
    android:theme="@style/Theme.Qing" />
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: register ChatActivity, SettingsActivity, TestActivity, StressActivity"
```

---

## Phase 4: Activity Embedding

### Task 12: Configure Activity Embedding

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/res/xml/main_split_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add window library to version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:
```toml
windowManager = "1.3.0"
```

Add to `[libraries]`:
```toml
androidx-window = { group = "androidx.window", name = "window", version.ref = "windowManager" }
```

- [ ] **Step 2: Add dependency to build.gradle.kts**

In `app/build.gradle.kts`, add after the Hilt dependencies (after line 71):
```kotlin
// Activity Embedding
implementation(libs.androidx.window)
```

- [ ] **Step 3: Create split config XML**

Create `app/src/main/res/xml/main_split_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:window="http://schemas.android.com/apk/res-auto">
    <SplitPairRule
        window:splitRatio="0.4/0.6"
        window:splitMinWidthDp="840"
        window:splitLayoutDirection="LOCALE"
        window:finishPrimaryWithSecondary="NEVER"
        window:finishSecondaryWithPrimary="ALWAYS"
        window:clearTop="false">
        <SplitPair
            window:primaryActivityName="com.onmi.qing.MainActivity"
            window:secondaryActivityName="com.onmi.qing.ChatActivity" />
        <SplitPair
            window:primaryActivityName="com.onmi.qing.MainActivity"
            window:secondaryActivityName="com.onmi.qing.SettingsActivity" />
        <SplitPair
            window:primaryActivityName="com.onmi.qing.MainActivity"
            window:secondaryActivityName="com.onmi.qing.TestActivity" />
        <SplitPair
            window:primaryActivityName="com.onmi.qing.MainActivity"
            window:secondaryActivityName="com.onmi.qing.StressActivity" />
    </SplitPairRule>
</resources>
```

- [ ] **Step 4: Add meta-data to AndroidManifest**

In `app/src/main/AndroidManifest.xml`, inside the `<application>` tag, before the first `<activity>`:

```xml
<meta-data
    android:name="android.window.SplitPairRule"
    android:resource="@xml/main_split_config" />
```

- [ ] **Step 5: Verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/xml/main_split_config.xml app/src/main/AndroidManifest.xml
git commit -m "feat: configure Activity Embedding with SplitPairRule"
```

---

## Final Verification

- [ ] **Step 1: Full build verification**

Run: `./gradlew assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all files exist**

```bash
ls -la app/src/main/java/com/onmi/qing/ChatActivity.kt
ls -la app/src/main/java/com/onmi/qing/SettingsActivity.kt
ls -la app/src/main/java/com/onmi/qing/TestActivity.kt
ls -la app/src/main/java/com/onmi/qing/StressActivity.kt
ls -la app/src/main/java/com/onmi/qing/ui/components/VerticalPillNavBar.kt
ls -la app/src/main/java/com/onmi/qing/ui/components/ConstrainedWidthContainer.kt
ls -la app/src/main/res/xml/main_split_config.xml
```

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: complete large screen adaptation — width constraints, vertical nav, Activity Embedding"
```

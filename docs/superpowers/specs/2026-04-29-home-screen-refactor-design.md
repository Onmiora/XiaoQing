# Home Screen Refactor Design

Date: 2026-04-29

## Problem

The current home screen has 5 cards of similar visual weight (Greeting Banner, Daily Check-In, Quick Access, Daily Goals, Achievement Overview), resulting in unclear visual hierarchy. Users can't tell what's most important at a glance. The screen also requires scrolling to see all content.

## Goal

Refactor the home screen into a **Hero + auxiliary cards** layout with clear visual priority, using MD3 dynamic color throughout.

## Design

### Page Structure

```
LazyColumn
  HeroCard (问候 + 心理分 + 维度环 + 心情选择)
    InlineMoodInput (选心情后内联展开)
  QuickAccessCard (聊天/呼吸/压力检测)
  BottomSpacer
```

From 5 cards down to 2. Everything fits in one screen without scrolling on most devices.

### HeroCard

Container: `ElevatedCard` with `primaryContainer` background, 24dp rounded corners.

**Top section:**
- Left: date string (e.g. "04月29日 周三") + greeting text (`headlineSmall`, bold)
- Right: 80dp circle with `primary` background, `onPrimary` text showing `overallScore` integer + "心理分" label
- Greeting text is time-of-day based (existing 7 time bands logic, kept as-is)

**Middle section — Psychology Dimensions:**
- `LazyRow` of 6 `DimensionRingItem` composables (existing component)
- Each ring: `GlowProgressRing` (70dp, 6dp stroke) + icon + name + percentage
- Dimension colors come from `DimensionHelpers.toDimensionIcon()` / `toDimensionColor()` — these are fixed semantic colors, NOT dynamic color
- Horizontal scroll hint text in the header row

**Bottom section — Mood Recording:**
- Row of 3 `FilterChip`s: 开心 (Happy), 平静 (Calm), 不开心 (Unhappy)
- Uses `surfaceContainerHigh` background for chips
- Selecting a chip expands an inline input area below the Hero card (not a ModalBottomSheet)
- Inline area: `OutlinedTextField` (3-5 lines, 20dp rounded corners) + Save button
- Saving calls `moodViewModel.addMoodEntry(mood, reason)` and collapses the input

### QuickAccessCard

Container: `ElevatedCard` with `surfaceContainerLow` background, 20dp rounded corners.

**Header:** 40dp `primary`-colored circle with Psychology icon + "小晴建议" text.

**Buttons:** 3 equal-width `Card`s in a horizontal `Row`:
- 聊天 (Chat) — Psychology icon, `primary` color
- 呼吸 (Breathe) — Air icon, `secondary` color
- 压力检测 (Stress Detection) — Favorite icon, `tertiary` color

Each button: `surface` background, 20dp rounded corners, icon + label stacked vertically.

### Removed Components

| Component | Reason |
|---|---|
| `DailyGoalsCard` | Removed from home screen. Goals data (`UsageStats`) still exists for potential future use. |
| `AchievementOverviewCard` | Removed from home screen. Achievement page still accessible from elsewhere. |
| Floating "记录心情" `Button` | Replaced by inline mood chips in HeroCard. |
| `MoodBottomSheet` | Replaced by inline expansion below HeroCard. |

### Color Strategy (MD3 Dynamic Color)

- Card containers: `primaryContainer`, `surfaceContainerLow`, `surfaceContainerHigh` — all from dynamic color
- Psychology dimension rings: fixed semantic colors from `DimensionHelpers` (these represent specific psychological concepts and should not change with theme)
- Mood chips: `surfaceContainerHigh` background, `onSurface` text
- Interactive elements: `primary`, `secondary`, `tertiary` from dynamic color
- No hardcoded hex colors, no gradients

### Animation

- Remove per-card `AnimatedCard` wrappers
- Use `LazyColumn`'s `animateItem()` modifier for natural scroll-linked animation
- Hero card dimension rings keep their existing `GlowProgressRing` animation
- Inline mood input uses `AnimatedVisibility` for expand/collapse

### ViewModel Changes

- `HomeViewModel`: no changes needed
- `AchievementViewModel`: no longer collected in HomeScreen (but ViewModel stays for Achievement page)
- `UsageStatsViewModel`: no longer collected in HomeScreen
- `MoodViewModel`: `addMoodEntry()` still called, state collection moves from BottomSheet to inline area
- HomeScreen composable drops `onAchievementClick` callback (no longer navigatable from home)

### Files to Modify

| File | Action |
|---|---|
| `ui/screens/home/HomeScreen.kt` | Rewrite: remove 3 cards, merge greeting+dimensions+mood into Hero, add inline mood input |
| `ui/navigation/Navigation.kt` | Remove `onAchievementClick` from HomeScreen call site |
| `ui/components/MoodBottomSheet.kt` | Keep file (may be used elsewhere or in future), but HomeScreen no longer uses it |

### What Stays the Same

- Bottom navigation (FloatingPillNavBar / NavigationRail) — unchanged
- Navigation routing and Screen sealed class — unchanged
- All ViewModels and data models — unchanged
- Psychology dimension data flow — unchanged
- Demo mode support — unchanged

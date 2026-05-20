package com.onmi.qing.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.ui.components.AnimatedCard
import com.onmi.qing.ui.components.ConstrainedWidthContainer
import com.onmi.qing.ui.components.adaptiveHorizontalPadding
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.viewmodel.UsageStatsViewModel

@Composable
fun DiscoverScreen(
    achievementViewModel: AchievementViewModel,
    usageStatsViewModel: UsageStatsViewModel,
    moodViewModel: MoodViewModel,
    demoModeManager: DemoModeManager,
    onBreathingClick: () -> Unit,
    onAchievementClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStressDetectionClick: () -> Unit,
    onFunTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val achievements by achievementViewModel.achievements.collectAsState()
    val breathingCount = usageStatsViewModel.breathingCount.collectAsState().value
    val unlockedCount = achievementViewModel.getUnlockedCount()
    val totalCount = achievementViewModel.getTotalCount()
    val moodEntries by moodViewModel.moodEntries.collectAsState()
    val latestMood by moodViewModel.latestMood.collectAsState()

    ConstrainedWidthContainer(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 20.dp)
        ) {
            Text(
                text = "发现",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Determine layout based on screen width
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        // Calculate max span based on actual columns that fit
        val maxSpan = if (screenWidth >= 560) 2 else 1
        val useSideBySideCards = screenWidth >= 600

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(
                horizontal = adaptiveHorizontalPadding(),
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 小晴心理Tips - 置顶显示
            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 0) {
                    TipCard()
                }
            }

            // 情绪仪表盘
            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 1) {
                    MoodDashboardCard(
                        moodEntries = moodEntries,
                        latestMood = latestMood
                    )
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 2) {
                    StressDetectionCard(
                        onClick = onStressDetectionClick
                    )
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 3) {
                    BreathingExerciseCard(
                        breathingCount = breathingCount,
                        onClick = onBreathingClick
                    )
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 4) {
                    FunTestCard(
                        onClick = onFunTestClick
                    )
                }
            }

            if (useSideBySideCards) {
                // Wide screen: show Achievement and History side by side in a Row
                item(span = { GridItemSpan(maxSpan) }) {
                    AnimatedCard(index = 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AchievementStatCard(
                                unlockedCount = unlockedCount,
                                totalCount = totalCount,
                                onClick = onAchievementClick,
                                modifier = Modifier.weight(1f)
                            )
                            HistoryStatCard(
                                onClick = onHistoryClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Narrow screen: show Achievement and History separately
                item(span = { GridItemSpan(maxSpan) }) {
                    AnimatedCard(index = 4) {
                        AchievementStatCard(
                            unlockedCount = unlockedCount,
                            totalCount = totalCount,
                            onClick = onAchievementClick
                        )
                    }
                }

                item(span = { GridItemSpan(maxSpan) }) {
                    AnimatedCard(index = 5) {
                        HistoryStatCard(onClick = onHistoryClick)
                    }
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 6) {
                    HotlineCard()
                }
            }

            // 底部间距，避免被浮动导航栏遮挡
            item(span = { GridItemSpan(maxSpan) }) {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }
        }
    }
}

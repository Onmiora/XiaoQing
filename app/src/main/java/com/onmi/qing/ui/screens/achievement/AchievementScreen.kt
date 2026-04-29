package com.onmi.qing.ui.screens.achievement

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.Achievement
import com.onmi.qing.ui.components.GradientProgressBar
import com.onmi.qing.ui.components.adaptiveHorizontalPadding
import com.onmi.qing.ui.components.GlowProgressRing
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.data.demo.DemoModeManager

@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    demoModeManager: DemoModeManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val achievements by viewModel.achievements.collectAsState()
    val unlockedCount = viewModel.getUnlockedCount()
    val totalCount = viewModel.getTotalCount()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "成就",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = adaptiveHorizontalPadding()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "已解锁",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$unlockedCount",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = " / $totalCount",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GradientProgressBar(
                    progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    height = 10.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (unlockedCount == totalCount) {
                        "太棒了！你已解锁所有成就！"
                    } else {
                        "继续加油，你正在变得更强大！"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                horizontal = adaptiveHorizontalPadding(),
                vertical = 8.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            items(achievements, key = { it.id }) { achievement ->
                AchievementCard(achievement = achievement)
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: Achievement) {
    val achievementColor = getAchievementColor(achievement.iconName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked)
                            achievementColor.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked) {
                    Icon(
                        imageVector = getAchievementIcon(achievement.iconName),
                        contentDescription = achievement.name,
                        tint = achievementColor,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "未解锁",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = achievement.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (achievement.isUnlocked)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (achievement.isUnlocked)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (achievement.isUnlocked && achievement.unlockedDate != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = achievement.unlockedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = achievementColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun getAchievementIcon(iconName: String): ImageVector {
    return when (iconName) {
        "star" -> Icons.Default.Star
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "emoji_events" -> Icons.Default.EmojiEvents
        "chat_bubble" -> Icons.Default.ChatBubble
        "psychology" -> Icons.Default.Psychology
        "favorite" -> Icons.Default.Favorite
        "air" -> Icons.Default.Air
        "self_improvement" -> Icons.Default.SelfImprovement
        "spa" -> Icons.Default.Spa
        "sentiment_satisfied" -> Icons.Default.SentimentSatisfied
        "bedtime" -> Icons.Default.Bedtime
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "cake" -> Icons.Default.Cake
        "wb_sunny" -> Icons.Default.WbSunny
        "mood" -> Icons.Default.Mood
        else -> Icons.Default.Star
    }
}

private fun getAchievementColor(iconName: String): Color {
    return when (iconName) {
        "star" -> Color(0xFFFFD700)
        "local_fire_department" -> Color(0xFFFF6B35)
        "emoji_events" -> Color(0xFFFFD700)
        "chat_bubble" -> Color(0xFF6366F1)
        "psychology" -> Color(0xFF8B5CF6)
        "favorite" -> Color(0xFFEC4899)
        "air" -> Color(0xFF22D3EE)
        "self_improvement" -> Color(0xFF10B981)
        "spa" -> Color(0xFF10B981)
        "sentiment_satisfied" -> Color(0xFF10B981)
        "bedtime" -> Color(0xFF6366F1)
        "workspace_premium" -> Color(0xFFFFD700)
        "cake" -> Color(0xFFF59E0B)
        "wb_sunny" -> Color(0xFFFBBF24)
        "mood" -> Color(0xFF10B981)
        else -> Color(0xFF9E9E9E)
    }
}

package com.onmi.qing.data

import androidx.compose.ui.graphics.Color

// 成就数据类
data class Achievement(
    val id: String,
    val name: String,                    // 成就名称
    val description: String,             // 成就描述
    val iconName: String,                // 图标名称 (Material Icon)
    val isUnlocked: Boolean = false,     // 是否已解锁
    val unlockedDate: String? = null     // 解锁日期
)

// 预定义的成就列表
object AchievementList {
    val achievements = listOf(
        // 连续签到成就
        Achievement(
            id = "checkin_3",
            name = "初次签到",
            description = "完成首次情绪签到",
            iconName = "star",
            isUnlocked = false
        ),
        Achievement(
            id = "checkin_7",
            name = "一周坚持",
            description = "累计签到 7 天",
            iconName = "local_fire_department",
            isUnlocked = false
        ),
        Achievement(
            id = "checkin_30",
            name = "月度守护",
            description = "连续签到 30 天",
            iconName = "emoji_events",
            isUnlocked = false
        ),

        // AI 对话成就
        Achievement(
            id = "chat_10",
            name = "初次倾诉",
            description = "与 AI 小晴完成 10 次对话",
            iconName = "chat_bubble",
            isUnlocked = false
        ),
        Achievement(
            id = "chat_50",
            name = "倾诉达人",
            description = "与 AI 小晴完成 50 次对话",
            iconName = "psychology",
            isUnlocked = false
        ),
        Achievement(
            id = "chat_100",
            name = "心灵知己",
            description = "与 AI 小晴完成 100 次对话",
            iconName = "favorite",
            isUnlocked = false
        ),

        // 呼吸练习成就
        Achievement(
            id = "breathing_5",
            name = "呼吸初体验",
            description = "完成 5 次呼吸练习",
            iconName = "air",
            isUnlocked = false
        ),
        Achievement(
            id = "breathing_20",
            name = "呼吸达人",
            description = "完成 20 次呼吸练习",
            iconName = "self_improvement",
            isUnlocked = false
        ),
        Achievement(
            id = "breathing_50",
            name = "呼吸大师",
            description = "完成 50 次呼吸练习",
            iconName = "spa",
            isUnlocked = false
        ),

        // 心理状态成就
        Achievement(
            id = "mood_good_7",
            name = "情绪稳定",
            description = "连续 7 天情绪稳定度 > 70%",
            iconName = "sentiment_satisfied",
            isUnlocked = false
        ),
        Achievement(
            id = "sleep_good_7",
            name = "睡眠改善",
            description = "连续 7 天睡眠质量 > 70%",
            iconName = "bedtime",
            isUnlocked = false
        ),
        Achievement(
            id = "stress_low_7",
            name = "压力释放",
            description = "连续 7 天压力管理 > 70%",
            iconName = "tranquility",
            isUnlocked = false
        ),

        // 综合成就
        Achievement(
            id = "all_round",
            name = "全面发展",
            description = "所有维度都达到 70% 以上",
            iconName = "workspace_premium",
            isUnlocked = false
        ),
        Achievement(
            id = "perfect_day",
            name = "完美一天",
            description = "单日完成签到、对话、呼吸练习",
            iconName = "cake",
            isUnlocked = false
        ),
        Achievement(
            id = "early_bird",
            name = "早起鸟",
            description = "早上 8 点前完成签到",
            iconName = "wb_sunny",
            isUnlocked = false
        )
    )
}

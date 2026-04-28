package com.onmi.qing.data

import kotlin.random.Random

// 心理维度更新
data class DimensionUpdate(
    val dimensionName: String,    // 维度名称
    val oldScore: Float,         // 更新前分数 (0-100)
    val newScore: Float,          // 更新后分数 (0-100)
    val changeIcon: String        // 变化图标 ("up", "down", "stable")
)

// 分析状态
sealed class AnalysisState {
    data object Idle : AnalysisState()
    data object Analyzing : AnalysisState()
    data class Completed(val result: AnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

// 分析结果数据
data class AnalysisResult(
    val title: String,                      // 灵动标题
    val subtitle: String,                    // 副标题
    val dimensionUpdates: List<DimensionUpdate>,  // 维度更新列表
    val summary: String? = null              // 后端返回的分析摘要
)

// 无变化提示消息
object NoChangeMessages {
    private val messages = listOf(
        "小晴没有发现你的变化",
        "这段对话暂时没有新的发现",
        "小晴在静静倾听中..."
    )

    fun random(): String = messages.random()
}

// 灵动标题候选
object InsightfulTitles {
    private val titles = listOf(
        "小晴发现了你新的变化 ✨",
        "你的内心世界很丰富呢 💫",
        "这段对话揭示了一些有趣的事 🌟",
        "小晴对你的了解又深了一步 🌙",
        "这是你成长的印记 🌱",
        "你比自己想象的更强大 💪",
        "每一次对话都是自我发现 🌸"
    )

    fun random(): String = titles.random()
}

// Mock分析结果生成器
object MockAnalysisGenerator {

    // 心理维度定义
    data class PsychologyDimensionDef(
        val name: String,
        val colorHex: Long,
        val icon: String
    )

    val dimensions = listOf(
        PsychologyDimensionDef("情绪稳定", 0xFF10B981, "mood"),
        PsychologyDimensionDef("自我认知", 0xFF3B82F6, "psychology"),
        PsychologyDimensionDef("压力管理", 0xFFF59E0B, "stress"),
        PsychologyDimensionDef("社交信心", 0xFF8B5CF6, "social"),
        PsychologyDimensionDef("睡眠质量", 0xFF06B6D4, "sleep"),
        PsychologyDimensionDef("自我关怀", 0xFFEC4899, "care")
    )

// 生成Mock分析结果
    fun generate(currentDimensions: Map<String, Float>): AnalysisResult {
        val updates = mutableListOf<DimensionUpdate>()
        val dimensionCount = dimensions.size

        // 随机选择1-3个维度进行更新
        val updateCount = (1..3).random()
        val selectedIndices = (0 until dimensionCount).shuffled().take(updateCount)

        for (index in selectedIndices) {
            val dim = dimensions[index]
            val currentScore = currentDimensions[dim.name] ?: 50f
            val change = Random.nextFloat() * 25f - 10f
            val newScore = (currentScore + change).coerceIn(20f, 100f)

            val changeIcon = when {
                newScore > currentScore -> "up"
                newScore < currentScore -> "down"
                else -> "stable"
            }

            updates.add(DimensionUpdate(
                dimensionName = dim.name,
                oldScore = currentScore,
                newScore = newScore,
                changeIcon = changeIcon
            ))
        }

        return AnalysisResult(
            title = InsightfulTitles.random(),
            subtitle = "基于这段对话，小晴对你的心理状态有了新的发现",
            dimensionUpdates = updates
        )
    }
}

package com.onmi.qing.data

// 压力检测问题数据模型
data class StressQuestion(
    val id: Int,
    val question: String,           // 问题文本
    val options: List<String>,     // 选项列表
    val weights: List<Int>,         // 每个选项对应的权重 (0-4)
    val category: QuestionCategory  // 问题分类
)

// 问题分类
enum class QuestionCategory {
    PERCEIVED_STRESS,    // 感知压力
    EMOTIONAL_DISTRESS,  // 情绪困扰
    PHYSICAL_SYMPTOMS,   // 身体症状
    SLEEP_QUALITY        // 睡眠质量
}

// 获取压力检测问题列表
fun getStressQuestions(): List<StressQuestion> = listOf(
    StressQuestion(
        id = 1,
        question = "在过去一个月中，您有多少次因为不可预料的事情而感到烦恼？",
        options = listOf("从来没有", "几乎没有", "有时", "经常", "非常频繁"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.PERCEIVED_STRESS
    ),
    StressQuestion(
        id = 2,
        question = "在过去一个月中，您有多少次感到自己无法控制生活中重要的事情？",
        options = listOf("从来没有", "几乎没有", "有时", "经常", "非常频繁"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.PERCEIVED_STRESS
    ),
    StressQuestion(
        id = 3,
        question = "在过去一个月中，您有多少次感到紧张和压力？",
        options = listOf("从来没有", "几乎没有", "有时", "经常", "非常频繁"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.EMOTIONAL_DISTRESS
    ),
    StressQuestion(
        id = 4,
        question = "在过去一个月中，您有多少次感到自信能够应对生活中的个人问题？",
        options = listOf("从来没有", "几乎没有", "有时", "经常", "一直都有"),
        weights = listOf(4, 3, 2, 1, 0),
        category = QuestionCategory.EMOTIONAL_DISTRESS
    ),
    StressQuestion(
        id = 5,
        question = "在过去一个月中，您的睡眠质量如何？",
        options = listOf("非常好", "较好", "一般", "较差", "非常差"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.SLEEP_QUALITY
    ),
    StressQuestion(
        id = 6,
        question = "在过去一个月中，您有多少次感到疲劳或精力不足？",
        options = listOf("从来没有", "几乎没有", "有时", "经常", "一直都有"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.PHYSICAL_SYMPTOMS
    ),
    StressQuestion(
        id = 7,
        question = "在过去一个月中，您有多少次因为压力而感到身体不适（如头痛、肌肉紧张等）？",
        options = listOf("从来没有", "偶尔", "有时", "经常", "非常频繁"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.PHYSICAL_SYMPTOMS
    ),
    StressQuestion(
        id = 8,
        question = "在过去一个月中，您的情绪波动程度如何？",
        options = listOf("非常稳定", "比较稳定", "一般", "波动较大", "波动非常大"),
        weights = listOf(0, 1, 2, 3, 4),
        category = QuestionCategory.EMOTIONAL_DISTRESS
    )
)

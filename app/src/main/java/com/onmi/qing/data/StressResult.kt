package com.onmi.qing.data

// 压力等级
enum class StressLevel {
    LOW,    // 低压力
    MEDIUM, // 中等压力
    HIGH    // 高压力
}

// 心率数据摘要
data class HeartRateSummary(
    val averageHR: Int,    // 平均心率
    val minHR: Int,        // 最小心率
    val maxHR: Int,        // 最大心率
    val hrvMetric: Float?, // HRV指标 (RMSSD)
    val hrvScore: Int?     // HRV评分 (0-100)
)

// 压力检测结果
data class StressResult(
    val stressScore: Int,              // 综合压力分数 (0-100)
    val stressLevel: StressLevel,      // 压力等级
    val heartRateData: HeartRateSummary?, // 心率数据摘要
    val questionnaireScore: Int,        // 问卷得分 (0-100)
    val recommendations: List<String>, // 建议列表
    val timestamp: Long = System.currentTimeMillis()
)

// 高压力提醒语句
val highStressReminders = listOf(
    "小晴发现你压力有点大，可以试试呼吸练习来放松一下",
    "最近似乎有些疲惫，深呼吸能帮你缓解紧张情绪哦",
    "压力太大时，不妨先停下来做个呼吸练习",
    "小晴建议你可以尝试4-7-8呼吸法来舒缓压力",
    "保持身心健康很重要，呼吸练习是个不错的开始"
)

// 根据压力等级获取随机提醒语句
fun getRandomHighStressReminder(): String {
    return highStressReminders.random()
}

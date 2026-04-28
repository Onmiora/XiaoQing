package com.onmi.qing.data

// SBTI测试算法
// 将维度原始分转换为等级
fun sumToLevel(score: Int): Char {
    return when {
        score <= 3 -> 'L'
        score == 4 -> 'M'
        else -> 'H'
    }
}

// 计算曼哈顿距离（用户向量与目标向量的差异总和）
fun calculateDistance(userVector: List<Char>, targetVector: List<Char>): Int {
    if (userVector.size != targetVector.size) {
        throw IllegalArgumentException("Vectors must have the same length")
    }

    val levelValues = mapOf('L' to 1, 'M' to 2, 'H' to 3)

    return userVector.zip(targetVector).sumOf { (user, target) ->
        kotlin.math.abs(levelValues[user]!! - levelValues[target]!!)
    }
}

// 计算完全匹配的维度数量（差值为0）
fun calculateExactMatches(userVector: List<Char>, targetVector: List<Char>): Int {
    if (userVector.size != targetVector.size) {
        throw IllegalArgumentException("Vectors must have the same length")
    }

    return userVector.zip(targetVector).count { (user, target) -> user == target }
}

// 计算相似度百分比
fun calculateSimilarity(distance: Int): Int {
    val similarity = (1 - distance.toDouble() / 30) * 100
    return maxOf(0, kotlin.math.round(similarity).toInt())
}

// 计算用户的维度等级向量
fun calculateUserVector(
    answers: Map<Int, Int>,  // questionId -> selectedOptionIndex
    questions: List<SbtiQuestion>
): List<Char> {
    // 按维度分组计算总分
    val dimensionScores = mutableMapOf<SbtiDimension, Int>()

    questions.forEach { question ->
        answers[question.id]?.let { selectedIndex ->
            if (selectedIndex < question.weights.size) {
                // 分值 = weights[selectedIndex]，而不是 selectedIndex + 1
                val weight = question.weights[selectedIndex]
                dimensionScores[question.dimension] = (dimensionScores[question.dimension] ?: 0) + weight
            }
        }
    }

    // 按固定顺序返回每个维度的等级
    val orderedDimensions = listOf(
        SbtiDimension.S1, SbtiDimension.S2, SbtiDimension.S3,
        SbtiDimension.E1, SbtiDimension.E2, SbtiDimension.E3,
        SbtiDimension.A1, SbtiDimension.A2, SbtiDimension.A3,
        SbtiDimension.Ac1, SbtiDimension.Ac2, SbtiDimension.Ac3,
        SbtiDimension.So1, SbtiDimension.So2, SbtiDimension.So3
    )

    return orderedDimensions.map { dimension ->
        val score = dimensionScores[dimension] ?: 0
        sumToLevel(score)
    }
}

// 解析人格pattern字符串为等级列表
fun parsePattern(pattern: String): List<Char> {
    return if (pattern.contains('-')) {
        // 5组模式：每组3个维度，展开为15个字符
        pattern.split('-').flatMap { group ->
            group.toList()
        }
    } else {
        // 非标准格式
        pattern.toList()
    }
}

// 匹配最相似的人格类型
fun matchPersonality(
    userAnswers: Map<Int, Int>,
    questions: List<SbtiQuestion>,
    personalityTypes: List<PersonalityType>
): Triple<PersonalityType, Int, Int> {  // 返回 Triple(人格, 距离, 相似度)
    // 计算用户向量
    val userVector = calculateUserVector(userAnswers, questions)

    // 过滤掉特殊人格（DRUNK和HHHH）进行正常匹配
    val normalPersonalityTypes = personalityTypes.filter {
        it.code != "DRUNK" && it.code != "HHHH"
    }

    // 计算与每个人格的匹配度
    val matches = normalPersonalityTypes.map { personality ->
        val targetVector = parsePattern(personality.pattern)
        val distance = calculateDistance(userVector, targetVector)
        val exact = calculateExactMatches(userVector, targetVector)
        val similarity = calculateSimilarity(distance)
        Triple(personality, distance, exact to similarity)
    }

    // 按规则排序：距离升序、完全匹配数降序、相似度降序
    val sortedMatches = matches.sortedWith(
        compareBy<Triple<PersonalityType, Int, Pair<Int, Int>>> { it.second }  // distance ascending
            .thenByDescending { it.third.first }  // exact matches descending
            .thenByDescending { it.third.second }  // similarity descending
    )

    // 获取最佳匹配
    val bestMatch = sortedMatches.firstOrNull()
        ?: throw IllegalStateException("No personality types available")

    return Triple(bestMatch.first, bestMatch.second, bestMatch.third.second)
}

// 检查是否触发特殊人格
fun checkSpecialCases(
    gateQuestion1Answer: Int?,  // drink_gate_q1的答案 (selectedOptionIndex)
    gateQuestion1Weight: Int?, // drink_gate_q1的答案的分值
    gateQuestion2Answer: Int?,  // drink_gate_q2的答案 (selectedOptionIndex)
    gateQuestion2Weight: Int?,  // drink_gate_q2的答案的分值
    similarity: Int? = null,
    personalityTypes: List<PersonalityType>
): PersonalityType? {
    // 1. 检查DRUNK（酒鬼）
    // drink_gate_q1 选择"饮酒"(weight=3) 且 drink_gate_q2 选择B(weight=2)
    if (gateQuestion1Weight == 3 && gateQuestion2Weight == 2) {
        return personalityTypes.find { it.code == "DRUNK" }
    }

    // 2. 检查HHHH（傻乐者）- 兜底人格，匹配度<60%时触发
    if (similarity != null && similarity < 60) {
        return personalityTypes.find { it.code == "HHHH" }
    }

    return null
}

// 执行完整的SBTI人格判定
fun calculatePersonalityResult(
    answers: Map<Int, Int>,
    gateQuestion1Answer: Int?,
    gateQuestion1Weight: Int?,
    gateQuestion2Answer: Int?,
    gateQuestion2Weight: Int?,
    questions: List<SbtiQuestion> = getSbtiQuestions(),
    personalityTypes: List<PersonalityType> = getPersonalityTypes()
): Pair<PersonalityType, Int> {
    // 1. 先检查特殊人格
    val specialCase = checkSpecialCases(
        gateQuestion1Answer = gateQuestion1Answer,
        gateQuestion1Weight = gateQuestion1Weight,
        gateQuestion2Answer = gateQuestion2Answer,
        gateQuestion2Weight = gateQuestion2Weight,
        similarity = null,
        personalityTypes = personalityTypes
    )

    if (specialCase != null) {
        return Pair(specialCase, if (specialCase.code == "DRUNK") 100 else 0)
    }

    // 2. 正常匹配
    val (bestPersonality, distance, similarity) = matchPersonality(answers, questions, personalityTypes)

    // 3. 检查是否需要触发HHHH兜底
    val finalPersonality = if (similarity < 60) {
        personalityTypes.find { it.code == "HHHH" } ?: bestPersonality
    } else {
        bestPersonality
    }

    return Pair(finalPersonality, similarity)
}

// 获取维度的中文描述
fun getDimensionExplanation(): Map<SbtiDimension, String> = mapOf(
    SbtiDimension.S1 to "自尊自信",
    SbtiDimension.S2 to "自我清晰度",
    SbtiDimension.S3 to "核心价值",
    SbtiDimension.E1 to "依恋安全感",
    SbtiDimension.E2 to "情感投入度",
    SbtiDimension.E3 to "边界与依赖",
    SbtiDimension.A1 to "世界观倾向",
    SbtiDimension.A2 to "规则与灵活度",
    SbtiDimension.A3 to "人生意义感",
    SbtiDimension.Ac1 to "动机导向",
    SbtiDimension.Ac2 to "决策风格",
    SbtiDimension.Ac3 to "执行模式",
    SbtiDimension.So1 to "社交主动性",
    SbtiDimension.So2 to "人际边界感",
    SbtiDimension.So3 to "表达与真实度"
)

package com.onmi.qing.data

// MBTI测试算法
// 计算MBTI结果
fun calculateMbtiResult(
    answers: Map<Int, Int>,
    questions: List<MbtiQuestion> = getMbtiQuestions()
): String {
    var eCount = 0  // 外向
    var iCount = 0  // 内向
    var sCount = 0  // 感觉
    var nCount = 0  // 直觉
    var tCount = 0  // 思考
    var fCount = 0  // 情感
    var jCount = 0  // 判断
    var pCount = 0  // 知觉

    // 统计各维度得分
    // 注意：题号1-7, 31, 35, 39, 41, 45, 48-49, 52, 54, 56, 60-61, 66, 71 属于E/I维度
    // 其他属于S/N, T/F, J/P维度

    answers.forEach { (questionId, selectedIndex) ->
        val dimension = getQuestionDimension(questionId)
        val isE = selectedIndex == 0  // 0=E端, 1=I端

        when (dimension) {
            "E_I" -> if (isE) eCount++ else iCount++
            "S_N" -> if (isE) sCount++ else nCount++
            "T_F" -> if (isE) tCount++ else fCount++
            "J_P" -> if (isE) jCount++ else pCount++
        }
    }

    // 组装MBTI结果
    val result = StringBuilder()

    // E/I维度
    result.append(if (eCount >= iCount) "E" else "I")

    // S/N维度
    result.append(if (sCount >= nCount) "S" else "N")

    // T/F维度
    result.append(if (tCount >= fCount) "T" else "F")

    // J/P维度
    result.append(if (jCount >= pCount) "J" else "P")

    return result.toString()
}

// 根据题目ID获取维度类型
fun getQuestionDimension(questionId: Int): String {
    return when (questionId) {
        // E/I维度 (18题)
        1, 2, 3, 4, 5, 6, 7, 31, 35, 39, 41, 45, 48, 49, 52, 54, 56, 60, 61, 66, 71 -> "E_I"
        // S/N维度 (15题)
        8, 9, 10, 11, 12, 13, 30, 32, 40, 42, 50, 55, 57, 68, 69 -> "S_N"
        // T/F维度 (14题)
        14, 15, 16, 17, 18, 19, 20, 33, 43, 50, 58, 63, 72 -> "T_F"
        // J/P维度 (25题)
        else -> "J_P"  // 21-29, 34, 36-38, 44, 46-47, 51, 53, 59, 62, 64-65, 67, 70
    }
}

// 获取MBTI类型描述
fun getMbtiTypeDescription(typeCode: String): String {
    val mbtiType = getMbtiTypes().find { it.code == typeCode }
    return mbtiType?.description ?: "未找到该人格类型的描述"
}

// 获取MBTI类型完整信息
fun getMbtiTypeInfo(typeCode: String): MbtiType? {
    return getMbtiTypes().find { it.code == typeCode }
}

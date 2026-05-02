package com.onmi.qing.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 对话会话数据类（用于历史记录）
data class ChatSession(
    val id: String,
    val title: String,           // 对话标题
    val lastMessage: String,      // 最后一条消息摘要
    val timestamp: Long,          // 最后更新时间
    val messageCount: Int,       // 消息数量
    val analysisCount: Int = 0,  // 分析次数
    val messages: List<ChatMessage> = emptyList()  // 会话的所有消息
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // 分析次数上限
    companion object {
        const val MAX_ANALYSIS_COUNT = 1
    }
}


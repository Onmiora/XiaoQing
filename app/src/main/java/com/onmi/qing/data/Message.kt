package com.onmi.qing.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 聊天消息数据类
data class Message(
    val id: String,
    var content: String,         // 消息内容 (mutable for streaming support)
    val isFromUser: Boolean,     // true: 用户消息, false: AI 消息
    val timestamp: Long = System.currentTimeMillis()
)

// 对话会话数据类（用于历史记录）
data class ChatSession(
    val id: String,
    val title: String,           // 对话标题
    val lastMessage: String,      // 最后一条消息摘要
    val timestamp: Long,          // 最后更新时间
    val messageCount: Int,       // 消息数量
    val analysisCount: Int = 0,  // 分析次数
    val messages: List<Message> = emptyList()  // 会话的所有消息
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

object AIResponses {
    val greeting = listOf(
        "你好呀！今天感觉怎么样？有什么想和我聊聊的吗？",
        "嗨，欢迎回来！今天过得如何？",
        "你好！看到你来我很开心。今天有什么心事想说吗？"
    )

    val fallback = listOf(
        "小晴服务出了一些问题～ \n\n ⚠️ 后端服务出现问题",
        "我出现了一些故障...稍等！在修了\n\n ⚠️ 后端服务出现问题",
        "嗯，我听到了。稍等片刻，我有一些自己的事情在忙～\n\n ⚠️ 后端服务出现问题",
        "你先等等！我马上回来～\n\n ⚠️ 后端服务出现问题"
    )


    // 根据用户输入获取 AI 回复（Demo 逻辑）
    fun getResponse(userMessage: String): String {
        val lowerMessage = userMessage.lowercase()

        return fallback.random()
    }
}


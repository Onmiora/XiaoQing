package com.onmi.qing.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

// Chat API Service - Anthropic 兼容格式
interface ChatApiService {
    @POST("/v1/messages")
    suspend fun chat(@Body request: AnthropicRequest): Response<AnthropicResponse>

// Streaming API
    @Streaming
    @POST("/v1/messages")
    suspend fun chatStreaming(@Body request: AnthropicRequest): Response<ResponseBody>
}

// Anthropic 格式请求
data class AnthropicRequest(
    val model: String = "minimax-m2.7",
    val messages: List<AnthropicMessage>,
    val max_tokens: Int = 4096,
    val stream: Boolean = false,
    val temperature: Double = 1.0
)

// Anthropic 消息格式
data class AnthropicMessage(
    val role: String,
    val content: String
)

// Anthropic 响应格式
data class AnthropicResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<AnthropicContent>? = null,
    val model: String? = null,
    @SerializedName("stop_reason")
    val stopReason: String? = null,
    val usage: AnthropicUsage? = null
)

// Anthropic 内容块
data class AnthropicContent(
    val type: String? = null,
    val text: String? = null,
    val thinking: String? = null
)

// Anthropic 使用量
data class AnthropicUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int? = null,
    @SerializedName("output_tokens")
    val outputTokens: Int? = null
)

// 分析对话 API

// 分析请求
data class AnalyzeRequest(
    val messages: List<AnthropicMessage>,
    val currentScores: Map<String, Float>? = null
)

// 分析响应
data class AnalyzeResponse(
    val deltas: Map<String, Float>,
    val summary: String?
)

// 分析 API Service
interface AnalyzeApiService {
    @POST("/v1/analyze")
    suspend fun analyze(@Body request: AnalyzeRequest): Response<AnalyzeResponse>
}

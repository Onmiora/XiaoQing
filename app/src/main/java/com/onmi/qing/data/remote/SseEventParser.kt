package com.onmi.qing.data.remote

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

// SSE Event types from Anthropic API
sealed class SseEvent {
    data class ContentBlockDelta(val text: String) : SseEvent()
    data class ThinkingDelta(val thinking: String) : SseEvent()
    data class ContentBlockStart(val blockType: String) : SseEvent()
    data class ContentBlockComplete(val blockType: String) : SseEvent()
    data class MessageDelta(val stopReason: String?) : SseEvent()
    data class MessageStop(val reason: String?) : SseEvent()
    data class ToolUse(val name: String, val input: String) : SseEvent()
    data class Ping(val data: String?) : SseEvent()
    data class Unknown(val event: String, val data: String) : SseEvent()
    data class Error(val error: String) : SseEvent()
}

// Parser for Server-Sent Events (SSE) from Anthropic streaming API
class SseEventParser {
    companion object {
        private const val TAG = "SseEventParser"
        private const val DATA_PREFIX = "data:"
    }

// Parse SSE events from ResponseBody as a Flow
    fun parseEvents(body: ResponseBody): Flow<SseEvent> = flow {
        val reader = BufferedReader(InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line?.trim() ?: continue

                // Skip empty lines
                if (trimmedLine.isEmpty()) {
                    continue
                }

                // Parse data line
                if (trimmedLine.startsWith(DATA_PREFIX)) {
                    val dataContent = trimmedLine.substringAfter(DATA_PREFIX).trim()

                    // Handle [DONE] marker - end of stream
                    if (dataContent == "[DONE]") {
                        Log.d(TAG, "Received [DONE], ending stream")
                        emit(SseEvent.MessageStop(null))
                        break
                    }

                    // Parse JSON event
                    val event = parseJsonEvent(dataContent)
                    if (event != null) {
                        emit(event)
                    }
                }
            }
        } finally {
            reader.close()
        }
    }

// Parse JSON event from data content
    private fun parseJsonEvent(data: String): SseEvent? {
        return try {
            val json = JsonParser.parseString(data).asJsonObject
            val type = json.get("type")?.asString

            when (type) {
                "message_start" -> {
                    val message = json.getAsJsonObject("message")
                    val role = message?.get("role")?.asString ?: "assistant"
                    Log.d(TAG, "message_start: role=$role")
                    SseEvent.Unknown("message_start", data)
                }

                "content_block_start" -> {
                    val contentBlock = json.getAsJsonObject("content_block")
                    val blockType = contentBlock?.get("type")?.asString ?: "unknown"
                    Log.d(TAG, "content_block_start: type=$blockType")
                    SseEvent.ContentBlockStart(blockType)
                }

                "content_block_delta" -> {
                    val delta = json.getAsJsonObject("delta")
                    val deltaType = delta?.get("type")?.asString

                    when (deltaType) {
                        "text_delta" -> {
                            val text = delta.get("text")?.asString ?: ""
                            Log.d(TAG, "content_block_delta text: '$text'")
                            SseEvent.ContentBlockDelta(text)
                        }
                        "thinking_delta" -> {
                            val thinking = delta.get("thinking")?.asString ?: ""
                            Log.d(TAG, "content_block_delta thinking: '${thinking.take(20)}...'")
                            SseEvent.ThinkingDelta(thinking)
                        }
                        else -> {
                            Log.w(TAG, "Unknown delta type: $deltaType")
                            SseEvent.Unknown("content_block_delta", data)
                        }
                    }
                }

                "content_block_stop" -> {
                    Log.d(TAG, "content_block_stop")
                    SseEvent.ContentBlockComplete("text")
                }

                "message_delta" -> {
                    val delta = json.getAsJsonObject("delta")
                    val stopReasonElement = delta?.get("stop_reason")
                    val stopReason = if (stopReasonElement != null && !stopReasonElement.isJsonNull) {
                        stopReasonElement.asString
                    } else {
                        null
                    }
                    Log.d(TAG, "message_delta: stopReason=$stopReason")
                    SseEvent.MessageDelta(stopReason)
                }

                "message_stop" -> {
                    Log.d(TAG, "message_stop")
                    SseEvent.MessageStop(null)
                }

                "tool_use" -> {
                    val name = json.get("name")?.asString ?: ""
                    val inputJson = json.get("input")?.asJsonObject
                    val input = inputJson?.toString() ?: "{}"
                    Log.d(TAG, "tool_use: name=$name, input=$input")
                    SseEvent.ToolUse(name, input)
                }

                "error" -> {
                    val error = json.get("error")?.asString ?: "Unknown error"
                    Log.e(TAG, "Error event: $error")
                    SseEvent.Error(error)
                }

                else -> {
                    Log.w(TAG, "Unknown event type: $type, data: $data")
                    if (type != null) {
                        SseEvent.Unknown(type, data)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event: ${e.message}, data: $data", e)
            SseEvent.Error("Parse error: ${e.message}")
        }
    }
}
